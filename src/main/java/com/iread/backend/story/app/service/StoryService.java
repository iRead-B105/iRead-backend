package com.iread.backend.story.app.service;

import com.iread.backend.ai.client.AiClient;
import com.iread.backend.ai.dto.req.*;
import com.iread.backend.ai.dto.res.GeneratedStoryBranchOption;
import com.iread.backend.ai.dto.res.GeneratedStoryBranchPrompt;
import com.iread.backend.ai.dto.res.GenerateStoryResponse;
import com.iread.backend.ai.dto.res.GeneratedStoryLine;
import com.iread.backend.ai.dto.res.SpeechTranscriptionResponse;
import com.iread.backend.ai.exception.AiClientException;
import com.iread.backend.exception.ResourceNotFoundException;
import com.iread.backend.exception.ConflictException;
import com.iread.backend.mypage.domain.CharacterEntity;
import com.iread.backend.story.app.dto.req.StoryBranchSelectionRequest;
import com.iread.backend.mypage.repository.CharacterRepository;
import com.iread.backend.pronunciation.PronunciationAnalysisAdapter;
import com.iread.backend.pronunciation.PronunciationAnalysisRequest;
import com.iread.backend.pronunciation.PronunciationAnalysisResult;
import com.iread.backend.pronunciation.PronunciationReferenceWord;
import com.iread.backend.pronunciation.PronunciationWordAligner;
import com.iread.backend.readingfeature.service.StudentFeatureProfileService;
import com.iread.backend.realtime.RealtimeEventPublisher;
import com.iread.backend.realtime.RealtimeResource;
import com.iread.backend.story.analysis.StoryLineContentService;
import com.iread.backend.story.app.dto.req.StoryTtsRequest;
import com.iread.backend.story.app.dto.res.*;
import com.iread.backend.story.domain.*;
import com.iread.backend.story.repository.*;
import com.iread.backend.student.domain.StudentEntity;
import com.iread.backend.student.repository.StudentRepository;
import com.iread.backend.training.domain.WordEntity;
import com.iread.backend.training.repository.WordRepository;
import com.iread.backend.wordattempt.domain.WordAttemptLogEntity;
import com.iread.backend.wordattempt.repository.WordAttemptLogRepository;
import com.iread.backend.wordattempt.service.WordAttemptScoreCalculator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StoryService {

    private static final int STORY_SCHEMA_VERSION = 1;
    private static final String STORY_CHARACTER_PROMPT_PREFIX = "[STORY_CHARACTER] ";
    private static final int MAX_IMAGE_PROMPT_LENGTH = 1_000;
    private static final int MAX_CHARACTER_NAME_LENGTH = 50;

    private final StudentRepository studentRepository;
    private final StoryTemplateRepository storyTemplateRepository;
    private final StoryRepository storyRepository;
    private final StorySceneRepository storySceneRepository;
    private final StoryLineRepository storyLineRepository;
    private final StoryChoiceRepository storyChoiceRepository;
    private final CharacterRepository characterRepository;
    private final WordRepository wordRepository;
    private final WordAttemptLogRepository wordAttemptLogRepository;
    private final AiClient aiClient;
    private final StoryAudioStorage storyAudioStorage;
    private final StoryLineContentService storyLineContentService;
    private final PronunciationAnalysisAdapter pronunciationAnalysisAdapter;
    private final PronunciationWordAligner pronunciationWordAligner;
    private final WordAttemptScoreCalculator wordAttemptScoreCalculator;
    private final StudentFeatureProfileService studentFeatureProfileService;
    private final RealtimeEventPublisher realtimeEventPublisher;
    private final ObjectMapper objectMapper;

    public StoryShelfResponse getStoryShelf(Long teacherId, Long studentId) {
        validateStudentOwner(teacherId, studentId);

        List<StoryShelfResponse.StoryItem> stories = storyRepository
                .findAllByStudentIdAndStatusNotOrderByCreatedAtDesc(studentId, StoryStatus.DELETED)
                .stream()
                .map(story -> new StoryShelfResponse.StoryItem(
                        story.getId(),
                        teacherId,
                        story.getStoryTemplate().getId(),
                        story.getCreatedAt(),
                        story.getStatus(),
                        story.getProgress()
                ))
                .toList();

        List<StoryShelfResponse.StoryTemplateItem> templates = storyTemplateRepository.findAllByOrderByIdAsc()
                .stream()
                .map(template -> new StoryShelfResponse.StoryTemplateItem(
                        template.getId(),
                        template.getTitle(),
                        template.getImageUrl()
                ))
                .toList();

        return new StoryShelfResponse(stories, templates);
    }

    public StoryTemplateResponse getStoryTemplate(Long teacherId, Long studentId, Long storyTemplateId) {
        validateStudentOwner(teacherId, studentId);
        StoryTemplateEntity template = findTemplate(storyTemplateId);
        return new StoryTemplateResponse(template.getId(), template.getTitle(), template.getContent());
    }

    @Transactional
    public StoryResumeResponse resumeStory(Long teacherId, Long studentId, Long storyId) {
        StoryEntity story = findOwnedStory(teacherId, studentId, storyId);

        List<StoryLineResponse> storyLines = toLineResponses(
                storyLineRepository.findAllByStoryIdOrderBySequenceNoAsc(storyId)
        );
        return new StoryResumeResponse(story.getId(), story.getStatus(), storyLines);
    }

    @Transactional
    public StoryLineResponse getStoryLine(Long teacherId, Long studentId, Long storyId, Long storyLineId) {
        StoryEntity story = findOwnedStory(teacherId, studentId, storyId);
        StoryLineEntity line = findLine(story.getId(), storyLineId);
        line.markRead(LocalDateTime.now());
        realtimeEventPublisher.publishAfterCommit(
                teacherId,
                studentId,
                RealtimeResource.STORY,
                storyId,
                "PROGRESS_UPDATED"
        );
        return toLineResponse(line);
    }

    @Transactional
    public StoryLinesResponse getStoryLines(Long teacherId, Long studentId, Long storyId) {
        StoryEntity story = findOwnedStory(teacherId, studentId, storyId);
        return new StoryLinesResponse(toLineResponses(
                storyLineRepository.findAllByStoryIdOrderBySequenceNoAsc(story.getId())
        ));
    }

    @Transactional
    public StorySessionResponse startStory(Long teacherId, Long studentId, Long storyTemplateId) {
        StudentEntity student = findStudentOwner(teacherId, studentId);
        StoryTemplateEntity template = findTemplate(storyTemplateId);
        StoryEntity story = storyRepository.saveAndFlush(new StoryEntity(student, template));

        String requestId = UUID.randomUUID().toString();
        GenerateStoryResponse generated = aiClient.generateStory(new GenerateStoryRequest(
                requestId,
                story.getId(),
                student.getId(),
                STORY_SCHEMA_VERSION,
                story.getProgress(),
                toTemplateData(template)
        ));

        appendGeneratedLines(story, null, generated);
        updateProgress(story, generated);
        if (generated.completed()) {
            createStoryCharacter(story, List.of(), generated);
        }
        realtimeEventPublisher.publishAfterCommit(
                teacherId,
                studentId,
                RealtimeResource.STORY,
                story.getId(),
                generated.completed() ? "COMPLETED" : "STARTED"
        );

        return new StorySessionResponse(
                story.getId(), teacherId, template.getId(), story.getCreatedAt(), story.getStatus()
        );
    }

    @Transactional
    public StoryChoiceResponse chooseStoryDirection(Long teacherId, Long studentId, Long storyId,
                                                    Long storyLineId, MultipartFile audioFile) {
        BranchContext context = prepareBranch(teacherId, studentId, storyId, storyLineId);
        if (context.existingChoice().isPresent()) {
            return replayChoice(context.story(), context.line(), context.existingChoice().get());
        }

        storyAudioStorage.store(studentId, audioFile);
        String transcript = aiClient.transcribeSpeech(
                UUID.randomUUID().toString(), studentId, null, audioFile
        ).transcript();
        return continueStoryDirection(
                teacherId, studentId, context.story(), context.line(), transcript
        );
    }

    @Transactional
    public StoryChoiceResponse chooseStoryDirection(Long teacherId, Long studentId, Long storyId,
                                                    Long storyLineId,
                                                    StoryBranchSelectionRequest request) {
        BranchContext context = prepareBranch(teacherId, studentId, storyId, storyLineId);
        if (context.existingChoice().isPresent()) {
            return replayChoice(context.story(), context.line(), context.existingChoice().get());
        }
        String branchIntent = resolveBranchOption(context.line(), request.optionNo());
        return continueStoryDirection(
                teacherId, studentId, context.story(), context.line(), branchIntent
        );
    }

    private BranchContext prepareBranch(Long teacherId, Long studentId, Long storyId,
                                        Long storyLineId) {
        StoryEntity story = findOwnedStory(teacherId, studentId, storyId);
        StoryLineEntity selectedLine = storyLineRepository
                .findByIdAndStoryIdForUpdate(storyLineId, story.getId())
                .orElseThrow(() -> new ResourceNotFoundException("스토리 대사를 찾을 수 없습니다."));
        Optional<StoryChoiceEntity> existingChoice = storyChoiceRepository.findByStoryLineId(storyLineId);
        if (existingChoice.isPresent()) {
            return new BranchContext(story, selectedLine, existingChoice);
        }
        if (!story.isInProgress()) {
            throw new ConflictException("진행 중인 스토리에서만 분기 입력을 제출할 수 있습니다.");
        }
        StoryLineEntity lastLine = storyLineRepository.findFirstByStoryIdOrderBySequenceNoDesc(story.getId())
                .orElseThrow(() -> new ResourceNotFoundException("스토리 대사를 찾을 수 없습니다."));
        if (!Objects.equals(selectedLine.getId(), lastLine.getId())) {
            throw new ConflictException("현재 마지막 분기 장면에만 답할 수 있습니다.");
        }
        if (!selectedLine.isRequiresBranchInput()) {
            throw new ConflictException("분기 입력이 필요한 장면이 아닙니다.");
        }
        if (selectedLine.getReadAt() == null) {
            throw new ConflictException("장면을 읽은 후 선택지를 제출할 수 있습니다.");
        }
        return new BranchContext(story, selectedLine, Optional.empty());
    }

    private StoryChoiceResponse continueStoryDirection(Long teacherId, Long studentId,
                                                       StoryEntity story,
                                                       StoryLineEntity selectedLine,
                                                       String branchIntent) {
        List<StoryLineEntity> historyLines = storyLineRepository
                .findAllByStoryIdOrderBySequenceNoAsc(story.getId());
        GenerateStoryResponse generated = aiClient.continueStory(new ContinueStoryRequest(
                UUID.randomUUID().toString(), story.getId(), studentId, STORY_SCHEMA_VERSION,
                story.getProgress(), toTemplateData(story.getStoryTemplate()), selectedLine.getId(),
                branchIntent, historyLines.stream().map(this::toHistoryLine).toList()
        ));

        GeneratedSegment segment = appendGeneratedLines(story, selectedLine, generated);
        updateProgress(story, generated);
        if (generated.completed()) {
            createStoryCharacter(story, historyLines, generated);
        }
        StoryChoiceEntity choice = storyChoiceRepository.saveAndFlush(
                new StoryChoiceEntity(selectedLine, branchIntent)
        );
        realtimeEventPublisher.publishAfterCommit(
                teacherId, studentId, RealtimeResource.STORY, story.getId(),
                generated.completed() ? "COMPLETED" : "PROGRESS_UPDATED"
        );

        return new StoryChoiceResponse(
                choice.getId(), branchIntent, segment.scene().getId(), segment.lines().getFirst().getId(),
                joinContent(segment.lines()), segment.scene().getImageUrl(), story.getProgress(),
                story.getStatus().name().toLowerCase(Locale.ROOT), false
        );
    }

    /**
     * 대사 읽기 음성을 받아 훈련과 같은 순서로 처리한다.
     * 음성 인식 → 발음 분석 → 기준 단어 정렬 → 단어별 시도 로그 적재 → 약점 프로파일 갱신.
     */
    @Transactional
    public StorySpeechResponse transcribeStoryLine(Long teacherId, Long studentId, Long storyId,
                                                   Long lineId, MultipartFile audioFile) {
        StudentEntity student = findStudentOwner(teacherId, studentId);
        StoryEntity story = storyRepository.findByIdAndStudentId(storyId, studentId)
                .orElseThrow(() -> new ResourceNotFoundException("스토리를 찾을 수 없습니다."));
        StoryLineEntity line = findLine(story.getId(), lineId);
        storyAudioStorage.store(studentId, audioFile);

        String referenceText = storyLineContentService.textOf(line);
        JsonNode analysis = storyLineContentService.ensureAnalysis(line);
        List<PronunciationReferenceWord> references =
                storyLineContentService.referenceWords(referenceText);

        SpeechTranscriptionResponse speech = aiClient.transcribeSpeech(
                UUID.randomUUID().toString(), studentId, referenceText, audioFile
        );
        String readingStatus = speech.transcript() == null || speech.transcript().isBlank()
                ? "failed"
                : speech.confidence() < 0.6 ? "low_confidence" : "recognized";

        PronunciationAnalysisResult pronunciation = pronunciationAnalysisAdapter.analyze(
                new PronunciationAnalysisRequest(
                        "story-speech-" + storyId + "-" + lineId + "-" + System.nanoTime(),
                        referenceText,
                        audioFile.getOriginalFilename(),
                        audioFile.getContentType(),
                        audioBytes(audioFile)
                )
        );
        PronunciationWordAligner.Alignment alignment = pronunciationWordAligner.align(
                references,
                pronunciation.words()
        );
        Map<Integer, List<String>> featureCodes =
                storyLineContentService.featureCodesByTokenIndex(analysis, references);
        List<StorySpeechResponse.WordResult> words = storeWordAttempts(
                student, line, alignment, featureCodes
        );
        studentFeatureProfileService.recalculate(student);

        return new StorySpeechResponse(
                speech.transcript(),
                Math.round(speech.confidence() * 10_000.0) / 100.0,
                readingStatus,
                pronunciation.pronunciationAccuracyScore(),
                pronunciation.fluencyScore(),
                pronunciation.completenessScore(),
                pronunciation.pronScore(),
                pronunciation.analysisVersion(),
                words
        );
    }

    /** 정렬된 단어를 단어 시도 로그로 적재한다. 같은 대사를 다시 읽으면 이전 시도는 최종이 아니게 된다. */
    private List<StorySpeechResponse.WordResult> storeWordAttempts(
            StudentEntity student,
            StoryLineEntity line,
            PronunciationWordAligner.Alignment alignment,
            Map<Integer, List<String>> featureCodes
    ) {
        markPreviousAttemptsNotFinal(line.getId());
        List<WordAttemptLogEntity> attempts = new ArrayList<>();
        for (PronunciationWordAligner.AlignedWord aligned : alignment.words()) {
            PronunciationReferenceWord reference = aligned.reference();
            var analyzed = aligned.analyzed();
            int accuracyScore = (int) Math.round(analyzed.scoreOrZero() * 10);
            boolean correct = wordAttemptScoreCalculator
                    .meetsPronunciationThreshold(accuracyScore)
                    && "NONE".equalsIgnoreCase(analyzed.errorType());
            Integer totalScore = wordAttemptScoreCalculator.calculate(
                    accuracyScore,
                    true,
                    true,
                    analyzed.isOmission(),
                    false,
                    false,
                    null,
                    null,
                    0,
                    correct
            );
            attempts.add(WordAttemptLogEntity.forStory(
                    student,
                    resolveWord(reference.surface()),
                    line,
                    reference.surface(),
                    true,
                    accuracyScore,
                    analyzed.offsetMs(),
                    analyzed.offsetMs() + analyzed.durationMs(),
                    analyzed.isOmission(),
                    correct,
                    totalScore,
                    reference.tokenIndex()
            ));
        }
        List<WordAttemptLogEntity> saved = wordAttemptLogRepository.saveAllAndFlush(attempts);

        List<StorySpeechResponse.WordResult> results = new ArrayList<>();
        for (int index = 0; index < saved.size(); index++) {
            WordAttemptLogEntity attempt = saved.get(index);
            var analyzed = alignment.words().get(index).analyzed();
            results.add(new StorySpeechResponse.WordResult(
                    attempt.getId(),
                    attempt.getTokenIndex(),
                    attempt.getSurfaceText(),
                    attempt.getPronunciationAccuracyScore(),
                    analyzed.errorType(),
                    analyzed.durationMs(),
                    attempt.getCorrect(),
                    attempt.getTotalScore(),
                    featureCodes.getOrDefault(attempt.getTokenIndex(), List.of())
            ));
        }
        return List.copyOf(results);
    }

    private void markPreviousAttemptsNotFinal(Long storyLineId) {
        wordAttemptLogRepository.findAllByStoryLineIdAndFinalAttemptTrue(storyLineId)
                .forEach(WordAttemptLogEntity::markNotFinal);
    }

    private WordEntity resolveWord(String surface) {
        return wordRepository.findByContent(surface)
                .orElseGet(() -> wordRepository.save(new WordEntity(surface)));
    }

    private byte[] audioBytes(MultipartFile audioFile) {
        try {
            if (audioFile.isEmpty()) {
                throw new IllegalArgumentException("audioFile은 비어 있을 수 없습니다.");
            }
            return audioFile.getBytes();
        } catch (IOException exception) {
            throw new IllegalArgumentException("audioFile을 읽을 수 없습니다.", exception);
        }
    }

    public StoryTtsResponse synthesizeStoryLine(Long teacherId, Long studentId, Long storyId,
                                                StoryTtsRequest request) {
        StoryEntity story = findOwnedStory(teacherId, studentId, storyId);
        StoryLineEntity line = findLine(story.getId(), request.lineId());
        var speech = aiClient.synthesizeSpeech(new SpeechSynthesisRequest(
                UUID.randomUUID().toString(), storyLineContentService.textOf(line), null
        ));
        String fileName = storyAudioStorage.storeGenerated(studentId, speech.audio());
        return new StoryTtsResponse(
                "/api/app/story/" + studentId + "/audio/" + fileName,
                speech.durationMs(),
                null
        );
    }

    public byte[] getGeneratedAudio(Long teacherId, Long studentId, String fileName) {
        validateStudentOwner(teacherId, studentId);
        return storyAudioStorage.loadGenerated(studentId, fileName);
    }

    private GeneratedSegment appendGeneratedLines(StoryEntity story, StoryLineEntity previousLine,
                                                  GenerateStoryResponse response) {
        validateGeneratedSegment(response);
        int sceneSequence = Math.toIntExact(storySceneRepository.countByStoryId(story.getId()) + 1);
        StorySceneEntity scene = storySceneRepository.saveAndFlush(
                new StorySceneEntity(story, null, sceneSequence)
        );
        List<StoryLineEntity> lines = new ArrayList<>();
        StoryLineEntity previous = previousLine;
        for (int index = 0; index < response.lines().size(); index++) {
            GeneratedStoryLine generated = response.lines().get(index);
            StoryLineEntity line = new StoryLineEntity(
                    previous, scene,
                    generated.requiresBranchInput(),
                    storyLineContentService.buildContent(generated.content()),
                    serializeBranchPrompt(generated.branchPrompt()),
                    index + 1
            );
            lines.add(line);
            previous = line;
        }
        return new GeneratedSegment(scene, storyLineRepository.saveAllAndFlush(lines));
    }

    private StoryChoiceResponse replayChoice(StoryEntity story, StoryLineEntity selectedLine,
                                             StoryChoiceEntity choice) {
        List<StoryLineEntity> storyLines = storyLineRepository
                .findAllByStoryIdOrderBySequenceNoAsc(story.getId());
        StoryLineEntity nextLine = storyLines
                .stream()
                .dropWhile(line -> !Objects.equals(line.getId(), selectedLine.getId()))
                .skip(1)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("저장된 분기 결과의 다음 대사를 찾을 수 없습니다."));
        List<StoryLineEntity> nextSceneLines = storyLines.stream()
                .filter(line -> Objects.equals(line.getScene().getId(), nextLine.getScene().getId()))
                .toList();
        return new StoryChoiceResponse(
                choice.getId(),
                choice.getContent(),
                nextLine.getScene().getId(),
                nextLine.getId(),
                joinContent(nextSceneLines),
                nextLine.getImageUrl(),
                story.getProgress(),
                story.getStatus().name().toLowerCase(Locale.ROOT),
                true
        );
    }

    private String joinContent(List<StoryLineEntity> lines) {
        return lines.stream()
                .map(storyLineContentService::textOf)
                .collect(java.util.stream.Collectors.joining("\n"));
    }

    private void validateGeneratedSegment(GenerateStoryResponse response) {
        if (response.lines().isEmpty()) {
            throw new AiClientException("AI 서버가 생성한 스토리 대사가 없습니다.");
        }
        for (int index = 0; index < response.lines().size(); index++) {
            GeneratedStoryLine line = response.lines().get(index);
            if (line.content() == null || line.content().isBlank()) {
                throw new AiClientException("AI 서버가 빈 스토리 대사를 반환했습니다.");
            }
            if (line.requiresBranchInput()) {
                validateBranchPrompt(line.branchPrompt());
            } else if (line.branchPrompt() != null) {
                throw new AiClientException("분기 입력이 아닌 대사에는 branchPrompt를 포함할 수 없습니다.");
            }
            if (index < response.lines().size() - 1 && line.requiresBranchInput()) {
                throw new AiClientException("AI 서버 응답의 분기 입력은 생성 구간 마지막에만 올 수 있습니다.");
            }
        }

        boolean lastRequiresBranchInput = response.lines().getLast().requiresBranchInput();
        if (response.completed() == lastRequiresBranchInput) {
            throw new AiClientException("AI 서버 응답의 완료 상태와 마지막 분기 입력 상태가 일치하지 않습니다.");
        }
    }

    private void updateProgress(StoryEntity story, GenerateStoryResponse response) {
        if (response.nextProgress() < story.getProgress() || response.nextProgress() > 100) {
            throw new AiClientException("AI 서버 응답의 nextProgress가 유효하지 않습니다.");
        }
        if (response.completed() != (response.nextProgress() == 100)) {
            throw new AiClientException("AI 서버 응답의 완료 상태와 진행률이 일치하지 않습니다.");
        }
        story.updateProgress(response.nextProgress());
    }

    private void createStoryCharacter(StoryEntity story, List<StoryLineEntity> historyLines,
                                      GenerateStoryResponse generated) {
        StoryTemplateEntity template = story.getStoryTemplate();
        String characterName = truncate(template.getTitle() + " 주인공", MAX_CHARACTER_NAME_LENGTH);
        String storyText = java.util.stream.Stream.concat(
                        historyLines.stream().map(storyLineContentService::textOf),
                        generated.lines().stream().map(GeneratedStoryLine::content)
                )
                .collect(java.util.stream.Collectors.joining(" "));
        String prompt = truncate(
                STORY_CHARACTER_PROMPT_PREFIX
                        + "완성된 어린이 이야기 '" + template.getTitle()
                        + "'의 친근한 주인공 단독 초상화. 이야기 내용: " + storyText,
                MAX_IMAGE_PROMPT_LENGTH
        );
        String imageUrl = aiClient.generateImage(new GenerateImageRequest(
                UUID.randomUUID().toString(),
                prompt
        )).imageUrl();
        if (imageUrl == null || imageUrl.isBlank()) {
            throw new AiClientException("AI 서버가 이야기 주인공 이미지를 반환하지 않았습니다.");
        }
        characterRepository.saveAndFlush(new CharacterEntity(
                story.getStudent(),
                story,
                imageUrl,
                characterName
        ));
    }

    private String truncate(String value, int maxLength) {
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    private StoryTemplateData toTemplateData(StoryTemplateEntity template) {
        return new StoryTemplateData(template.getId(), template.getTitle(), template.getContent());
    }

    private StoryHistoryLine toHistoryLine(StoryLineEntity line) {
        return new StoryHistoryLine(
                line.getId(),
                storyLineContentService.textOf(line),
                line.isRequiresBranchInput()
        );
    }

    private StoryLineResponse toLineResponse(StoryLineEntity line) {
        return toLineResponse(
                line,
                line.getPreviousStoryLine() == null ? null : line.getPreviousStoryLine().getId()
        );
    }

    private List<StoryLineResponse> toLineResponses(List<StoryLineEntity> lines) {
        List<StoryLineResponse> responses = new ArrayList<>();
        Long previousLineId = null;
        for (StoryLineEntity line : lines) {
            responses.add(toLineResponse(line, previousLineId));
            previousLineId = line.getId();
        }
        return List.copyOf(responses);
    }

    private StoryLineResponse toLineResponse(StoryLineEntity line, Long previousLineId) {
        return new StoryLineResponse(
                line.getId(),
                previousLineId,
                line.getScene().getId(),
                line.getStory().getId(),
                line.getImageUrl(),
                line.isRequiresBranchInput(),
                storyLineContentService.textOf(line),
                toBranchPrompt(line.getBranchPrompt()),
                storyLineContentService.ensureAnalysis(line),
                line.getScene().getSequenceNo(),
                line.getSequenceNo(),
                line.getCreatedAt(),
                line.getReadAt()
        );
    }

    private String resolveBranchOption(StoryLineEntity line, Integer optionNo) {
        StoryBranchPromptResponse prompt = toBranchPrompt(line.getBranchPrompt());
        if (prompt == null || optionNo == null) {
            throw new ConflictException("제공된 분기 선택지 중 하나를 선택해야 합니다.");
        }
        return prompt.options().stream()
                .filter(option -> option.optionNo() == optionNo)
                .map(StoryBranchPromptResponse.Option::label)
                .findFirst()
                .orElseThrow(() -> new ConflictException(
                        "제공된 분기 선택지 중 하나를 선택해야 합니다."
                ));
    }

    private void validateBranchPrompt(GeneratedStoryBranchPrompt prompt) {
        if (prompt == null || prompt.options() == null || prompt.options().size() != 3) {
            throw new AiClientException("AI 서버의 branchPrompt는 선택지 3개를 포함해야 합니다.");
        }
        Set<Integer> optionNumbers = new HashSet<>();
        Set<String> labels = new HashSet<>();
        for (GeneratedStoryBranchOption option : prompt.options()) {
            if (option == null || option.optionNo() < 1 || option.optionNo() > 3) {
                throw new AiClientException("AI 서버의 분기 선택지 번호는 1부터 3까지여야 합니다.");
            }
            if (option.label() == null || option.label().isBlank()
                    || option.label().length() > 80
                    || !option.label().equals(option.label().strip())) {
                throw new AiClientException("AI 서버의 분기 선택지 문구가 유효하지 않습니다.");
            }
            optionNumbers.add(option.optionNo());
            labels.add(option.label());
        }
        if (!optionNumbers.equals(Set.of(1, 2, 3)) || labels.size() != 3) {
            throw new AiClientException("AI 서버의 분기 선택지는 번호와 문구가 중복되지 않아야 합니다.");
        }
    }

    private String serializeBranchPrompt(GeneratedStoryBranchPrompt prompt) {
        if (prompt == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(prompt);
        } catch (Exception exception) {
            throw new AiClientException("AI 서버의 branchPrompt를 저장할 수 없습니다.", exception);
        }
    }

    private StoryBranchPromptResponse toBranchPrompt(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            GeneratedStoryBranchPrompt prompt = objectMapper.readValue(
                    json, GeneratedStoryBranchPrompt.class
            );
            validateBranchPrompt(prompt);
            return new StoryBranchPromptResponse(prompt.options().stream()
                    .map(option -> new StoryBranchPromptResponse.Option(
                            option.optionNo(), option.label()
                    ))
                    .toList());
        } catch (AiClientException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new AiClientException("저장된 branchPrompt를 읽을 수 없습니다.", exception);
        }
    }

    private StoryTemplateEntity findTemplate(Long storyTemplateId) {
        return storyTemplateRepository.findById(storyTemplateId)
                .orElseThrow(() -> new ResourceNotFoundException("스토리 템플릿을 찾을 수 없습니다."));
    }

    private StoryEntity findOwnedStory(Long teacherId, Long studentId, Long storyId) {
        validateStudentOwner(teacherId, studentId);
        return storyRepository.findByIdAndStudentId(storyId, studentId)
                .orElseThrow(() -> new ResourceNotFoundException("스토리를 찾을 수 없습니다."));
    }

    private StoryLineEntity findLine(Long storyId, Long storyLineId) {
        return storyLineRepository.findByIdAndStoryId(storyLineId, storyId)
                .orElseThrow(() -> new ResourceNotFoundException("스토리 대사를 찾을 수 없습니다."));
    }

    private StudentEntity findStudentOwner(Long teacherId, Long studentId) {
        return studentRepository.findByIdAndTeacherId(studentId, teacherId)
                .orElseThrow(() -> new ResourceNotFoundException("학생을 찾을 수 없습니다."));
    }

    private void validateStudentOwner(Long teacherId, Long studentId) {
        findStudentOwner(teacherId, studentId);
    }

    private record GeneratedSegment(StorySceneEntity scene, List<StoryLineEntity> lines) {
    }

    private record BranchContext(StoryEntity story, StoryLineEntity line,
                                 Optional<StoryChoiceEntity> existingChoice) {
    }
}
