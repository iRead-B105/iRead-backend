package com.iread.backend.story.app.service;

import com.iread.backend.ai.client.AiClient;
import com.iread.backend.ai.dto.req.*;
import com.iread.backend.ai.dto.res.GenerateStoryResponse;
import com.iread.backend.ai.dto.res.GeneratedStoryLine;
import com.iread.backend.ai.dto.res.SpeechTranscriptionResponse;
import com.iread.backend.ai.exception.AiClientException;
import com.iread.backend.exception.ResourceNotFoundException;
import com.iread.backend.story.app.dto.req.StoryTtsRequest;
import com.iread.backend.story.app.dto.res.*;
import com.iread.backend.story.domain.*;
import com.iread.backend.story.repository.*;
import com.iread.backend.student.domain.StudentEntity;
import com.iread.backend.student.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StoryService {

    private static final int STORY_SCHEMA_VERSION = 1;

    private final StudentRepository studentRepository;
    private final StoryTemplateRepository storyTemplateRepository;
    private final StoryRepository storyRepository;
    private final StorySceneRepository storySceneRepository;
    private final StoryLineRepository storyLineRepository;
    private final StoryChoiceRepository storyChoiceRepository;
    private final AiClient aiClient;
    private final StoryAudioStorage storyAudioStorage;

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
                        story.getStatus()
                ))
                .toList();

        List<StoryShelfResponse.StoryTemplateItem> templates = storyTemplateRepository.findAllByOrderByIdAsc()
                .stream()
                .map(template -> new StoryShelfResponse.StoryTemplateItem(template.getId(), template.getTitle()))
                .toList();

        return new StoryShelfResponse(stories, templates);
    }

    public StoryTemplateResponse getStoryTemplate(Long teacherId, Long studentId, Long storyTemplateId) {
        validateStudentOwner(teacherId, studentId);
        StoryTemplateEntity template = findTemplate(storyTemplateId);
        return new StoryTemplateResponse(template.getId(), template.getTitle(), template.getContent());
    }

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
        return toLineResponse(line);
    }

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

        return new StorySessionResponse(
                story.getId(), teacherId, template.getId(), story.getCreatedAt(), story.getStatus()
        );
    }

    @Transactional
    public StoryChoiceResponse chooseStoryDirection(Long teacherId, Long studentId, Long storyId,
                                                    Long storyLineId, MultipartFile audioFile) {
        StoryEntity story = findOwnedStory(teacherId, studentId, storyId);
        StoryLineEntity selectedLine = storyLineRepository
                .findByIdAndStoryIdForUpdate(storyLineId, story.getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "스토리 대사를 찾을 수 없습니다."
                ));
        Optional<StoryChoiceEntity> existingChoice = storyChoiceRepository.findByStoryLineId(storyLineId);
        if (existingChoice.isPresent()) {
            return replayChoice(story, selectedLine, existingChoice.get());
        }
        if (!story.isInProgress()) {
            throw new IllegalStateException("진행 중인 스토리에서만 분기 입력을 제출할 수 있습니다.");
        }
        StoryLineEntity lastLine = storyLineRepository.findFirstByStoryIdOrderBySequenceNoDesc(story.getId())
                .orElseThrow(() -> new ResourceNotFoundException("스토리 대사를 찾을 수 없습니다."));
        if (!Objects.equals(selectedLine.getId(), lastLine.getId())) {
            throw new IllegalStateException("현재 마지막 분기 장면에만 답할 수 있습니다.");
        }
        if (!selectedLine.isRequiresBranchInput()) {
            throw new IllegalStateException("분기 입력이 필요한 장면이 아닙니다.");
        }
        if (selectedLine.getReadAt() == null) {
            throw new IllegalStateException("장면을 읽은 후 선택지를 제출할 수 있습니다.");
        }
        List<StoryLineEntity> historyLines = storyLineRepository
                .findAllByStoryIdOrderBySequenceNoAsc(story.getId());
        storyAudioStorage.store(studentId, audioFile);
        String speechRequestId = UUID.randomUUID().toString();
        String transcript = aiClient.transcribeSpeech(
                speechRequestId, studentId, null, audioFile
        ).transcript();

        String requestId = UUID.randomUUID().toString();
        GenerateStoryResponse generated = aiClient.continueStory(new ContinueStoryRequest(
                requestId,
                story.getId(),
                studentId,
                STORY_SCHEMA_VERSION,
                story.getProgress(),
                toTemplateData(story.getStoryTemplate()),
                selectedLine.getId(),
                transcript,
                historyLines.stream().map(this::toHistoryLine).toList()
        ));

        GeneratedSegment segment = appendGeneratedLines(story, selectedLine, generated);
        updateProgress(story, generated);
        StoryChoiceEntity choice = storyChoiceRepository.saveAndFlush(
                new StoryChoiceEntity(selectedLine, transcript)
        );

        return new StoryChoiceResponse(
                choice.getId(),
                transcript,
                segment.scene().getId(),
                segment.lines().getFirst().getId(),
                joinContent(segment.lines()),
                segment.scene().getImageUrl(),
                story.getProgress(),
                story.getStatus().name().toLowerCase(Locale.ROOT),
                false
        );
    }

    public StorySpeechResponse transcribeStoryLine(Long teacherId, Long studentId, Long storyId,
                                                   Long lineId, MultipartFile audioFile) {
        StoryEntity story = findOwnedStory(teacherId, studentId, storyId);
        StoryLineEntity line = findLine(story.getId(), lineId);
        storyAudioStorage.store(studentId, audioFile);
        SpeechTranscriptionResponse speech = aiClient.transcribeSpeech(
                UUID.randomUUID().toString(), studentId, line.getContent(), audioFile
        );
        String readingStatus = speech.transcript() == null || speech.transcript().isBlank()
                ? "failed"
                : speech.confidence() < 0.6 ? "low_confidence" : "recognized";
        return new StorySpeechResponse(
                speech.transcript(),
                Math.round(speech.confidence() * 10_000.0) / 100.0,
                readingStatus
        );
    }

    public StoryTtsResponse synthesizeStoryLine(Long teacherId, Long studentId, Long storyId,
                                                StoryTtsRequest request) {
        StoryEntity story = findOwnedStory(teacherId, studentId, storyId);
        StoryLineEntity line = findLine(story.getId(), request.lineId());
        var speech = aiClient.synthesizeSpeech(new SpeechSynthesisRequest(
                UUID.randomUUID().toString(), line.getContent(), null
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
                    generated.content(),
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
                .map(StoryLineEntity::getContent)
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

    private StoryTemplateData toTemplateData(StoryTemplateEntity template) {
        return new StoryTemplateData(template.getId(), template.getTitle(), template.getContent());
    }

    private StoryHistoryLine toHistoryLine(StoryLineEntity line) {
        return new StoryHistoryLine(
                line.getId(),
                line.getContent(),
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
                line.getContent(),
                line.getScene().getSequenceNo(),
                line.getSequenceNo(),
                line.getCreatedAt(),
                line.getReadAt()
        );
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
}
