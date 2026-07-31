package com.iread.backend.story.app.service;

import com.iread.backend.ai.client.AiClient;
import com.iread.backend.ai.dto.req.ContinueStoryRequest;
import com.iread.backend.ai.dto.req.GenerateImageRequest;
import com.iread.backend.ai.exception.AiClientException;
import com.iread.backend.ai.dto.res.GenerateImageResponse;
import com.iread.backend.ai.dto.res.GenerateStoryResponse;
import com.iread.backend.ai.dto.res.GeneratedStoryBranchOption;
import com.iread.backend.ai.dto.res.GeneratedStoryBranchPrompt;
import com.iread.backend.ai.dto.res.GeneratedStoryLine;
import com.iread.backend.ai.dto.res.SpeechTranscriptionResponse;
import com.iread.backend.exception.ConflictException;
import com.iread.backend.mypage.domain.CharacterEntity;
import com.iread.backend.story.app.dto.res.StoryLineResponse;
import com.iread.backend.mypage.repository.CharacterRepository;
import com.iread.backend.pronunciation.PronunciationAnalysisAdapter;
import com.iread.backend.pronunciation.PronunciationAnalysisResult;
import com.iread.backend.pronunciation.PronunciationWordAligner;
import com.iread.backend.pronunciation.PronunciationWordResult;
import com.iread.backend.readingfeature.service.StudentFeatureProfileService;
import com.iread.backend.realtime.RealtimeEventPublisher;
import com.iread.backend.story.analysis.StoryLineContentService;
import com.iread.backend.story.app.dto.req.StoryBranchSelectionRequest;
import com.iread.backend.story.domain.*;
import com.iread.backend.story.repository.*;
import com.iread.backend.student.domain.StudentEntity;
import com.iread.backend.student.repository.StudentRepository;
import com.iread.backend.training.analysis.KoreanG2pEngine;
import com.iread.backend.training.analysis.KoreanTextAnalyzer;
import com.iread.backend.training.domain.WordEntity;
import com.iread.backend.training.repository.WordRepository;
import com.iread.backend.wordattempt.domain.WordAttemptLogEntity;
import com.iread.backend.wordattempt.domain.WordAttemptUseLocation;
import com.iread.backend.wordattempt.repository.WordAttemptLogRepository;
import com.iread.backend.wordattempt.service.WordAttemptScoreCalculator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.json.JsonMapper;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.mock.web.MockMultipartFile;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StoryServiceTest {

    @Mock StudentRepository studentRepository;
    @Mock StoryTemplateRepository storyTemplateRepository;
    @Mock StoryRepository storyRepository;
    @Mock StorySceneRepository storySceneRepository;
    @Mock StoryLineRepository storyLineRepository;
    @Mock StoryChoiceRepository storyChoiceRepository;
    @Mock CharacterRepository characterRepository;
    @Mock WordRepository wordRepository;
    @Mock WordAttemptLogRepository wordAttemptLogRepository;
    @Mock AiClient aiClient;
    @Mock StoryAudioStorage storyAudioStorage;
    @Mock PronunciationAnalysisAdapter pronunciationAnalysisAdapter;
    @Mock WordAttemptScoreCalculator wordAttemptScoreCalculator;
    @Mock StudentFeatureProfileService studentFeatureProfileService;
    @Mock RealtimeEventPublisher realtimeEventPublisher;
    @Spy PronunciationWordAligner pronunciationWordAligner = new PronunciationWordAligner();
    @Spy StoryLineContentService storyLineContentService = new StoryLineContentService(
            new KoreanTextAnalyzer(new KoreanG2pEngine()),
            JsonMapper.builder().build()
    );
    @Spy ObjectMapper objectMapper = JsonMapper.builder().build();
    @InjectMocks StoryService storyService;

    private StudentEntity student;
    private StoryTemplateEntity template;

    @BeforeEach
    void setUp() {
        student = mock(StudentEntity.class);
        template = mock(StoryTemplateEntity.class);
        lenient().when(student.getId()).thenReturn(20L);
        lenient().when(template.getId()).thenReturn(30L);
        lenient().when(template.getTitle()).thenReturn("신비한 숲");
        lenient().when(template.getContent()).thenReturn("숲에서 친구를 만나는 이야기");
        lenient().when(template.getImageUrl()).thenReturn("/images/mystic-forest.png");
    }

    @Test
    void 책장은_삭제되지_않은_학생_스토리와_모든_템플릿을_반환한다() {
        StoryEntity story = story(100L);
        when(studentRepository.findByIdAndTeacherId(20L, 1L)).thenReturn(Optional.of(student));
        when(storyRepository.findAllByStudentIdAndStatusNotOrderByCreatedAtDesc(20L, StoryStatus.DELETED))
                .thenReturn(List.of(story));
        when(storyTemplateRepository.findAllByOrderByIdAsc()).thenReturn(List.of(template));

        var response = storyService.getStoryShelf(1L, 20L);

        assertThat(response.stories()).hasSize(1);
        assertThat(response.stories().getFirst().storyId()).isEqualTo(100L);
        assertThat(response.stories().getFirst().progress()).isZero();
        assertThat(response.storyTemplates()).hasSize(1);
        assertThat(response.storyTemplates().getFirst().storyTemplateId()).isEqualTo(30L);
        assertThat(response.storyTemplates().getFirst().imageUrl())
                .isEqualTo("/images/mystic-forest.png");
    }

    @Test
    void 신규_세션은_첫_선택지까지_생성하여_순서대로_저장한다() {
        when(studentRepository.findByIdAndTeacherId(20L, 1L)).thenReturn(Optional.of(student));
        when(storyTemplateRepository.findById(30L)).thenReturn(Optional.of(template));
        when(storyRepository.saveAndFlush(any(StoryEntity.class))).thenAnswer(invocation -> {
            StoryEntity saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 100L);
            ReflectionTestUtils.setField(saved, "createdAt", LocalDateTime.of(2026, 7, 22, 10, 0));
            return saved;
        });
        when(aiClient.generateStory(any())).thenReturn(new GenerateStoryResponse(
                "ignored-by-service-mock",
                1,
                50,
                false,
                List.of(
                        new GeneratedStoryLine("숲에 도착했어요.", false),
                        new GeneratedStoryLine("반짝이는 길을 걸었어요.", false),
                        new GeneratedStoryLine("작은 친구를 만났어요.", false),
                        new GeneratedStoryLine("숨겨진 표지판을 찾았어요.", false),
                        new GeneratedStoryLine("어디로 갈까요?", true, branchPrompt())
                )
        ));
        mockSceneSave(200L);
        mockLineSave(1000L);

        var response = storyService.startStory(1L, 20L, 30L);

        assertThat(response.storyId()).isEqualTo(100L);
        assertThat(response.storyStatus()).isEqualTo(StoryStatus.IN_PROGRESS);
        ArgumentCaptor<List<StoryLineEntity>> linesCaptor = listCaptor();
        verify(storyLineRepository).saveAllAndFlush(linesCaptor.capture());
        List<StoryLineEntity> lines = linesCaptor.getValue();
        assertThat(lines).extracting(StoryLineEntity::getSequenceNo).containsExactly(1, 2, 3, 4, 5);
        assertThat(lines).extracting(storyLineContentService::textOf)
                .containsExactly(
                        "숲에 도착했어요.",
                        "반짝이는 길을 걸었어요.",
                        "작은 친구를 만났어요.",
                        "숨겨진 표지판을 찾았어요.",
                        "어디로 갈까요?"
                );
        assertThat(lines.get(4).getPreviousStoryLine()).isSameAs(lines.get(3));
        assertThat(lines.getLast().isRequiresBranchInput()).isTrue();
        assertThat(lines.getLast().getBranchPrompt()).contains("\"optionNo\":1");
    }

    @Test
    void 장면을_조회하면_최초_조회_시각을_읽은_일자로_저장한다() {
        StoryEntity story = story(100L);
        StoryLineEntity line = line(1000L, story, null, false, "첫 대사", 1, null);
        ownedStory(story);
        when(storyLineRepository.findByIdAndStoryId(1000L, 100L)).thenReturn(Optional.of(line));

        var response = storyService.getStoryLine(1L, 20L, 100L, 1000L);

        assertThat(response.readAt()).isNotNull();
        assertThat(line.getReadAt()).isEqualTo(response.readAt());
    }

    @Test
    void 읽지_않은_대사가_없으면_답하지_않은_마지막_선택지에서_재개한다() {
        StoryEntity story = story(100L);
        StoryLineEntity choiceLine = line(1001L, story, null, true, "어떻게 할까요?", 2,
                LocalDateTime.of(2026, 7, 22, 10, 10));
        ownedStory(story);
        when(storyLineRepository.findAllByStoryIdOrderBySequenceNoAsc(100L))
                .thenReturn(List.of(choiceLine));
        var response = storyService.resumeStory(1L, 20L, 100L);

        assertThat(response.storyLines().getFirst().lineId()).isEqualTo(1001L);
    }

    @Test
    void AI_선택지_번호를_검증해_문구를_저장하고_다음_대사를_생성한다() {
        StoryEntity story = story(100L);
        StoryLineEntity firstLine = line(1000L, story, null, false, "숲에 도착했어요.", 1,
                LocalDateTime.of(2026, 7, 22, 10, 5));
        StoryLineEntity choiceLine = line(1001L, story, firstLine, true, "어떻게 할까요?", 2,
                LocalDateTime.of(2026, 7, 22, 10, 10));
        ownedStory(story);
        when(storyLineRepository.findByIdAndStoryIdForUpdate(1001L, 100L)).thenReturn(Optional.of(choiceLine));
        when(storyChoiceRepository.findByStoryLineId(1001L)).thenReturn(Optional.empty());
        when(storyLineRepository.findFirstByStoryIdOrderBySequenceNoDesc(100L))
                .thenReturn(Optional.of(choiceLine));
        when(storyLineRepository.findAllByStoryIdOrderBySequenceNoAsc(100L))
                .thenReturn(List.of(firstLine, choiceLine));
        when(aiClient.continueStory(any())).thenReturn(new GenerateStoryResponse(
                "ignored-by-service-mock",
                1,
                100,
                true,
                List.of(
                        new GeneratedStoryLine("선택한 길로 걸었어요.", false),
                        new GeneratedStoryLine("친구가 앞장섰어요.", false),
                        new GeneratedStoryLine("빛나는 단서를 찾았어요.", false),
                        new GeneratedStoryLine("잃어버린 보물을 발견했어요.", false),
                        new GeneratedStoryLine("모두와 인사하고 집으로 돌아왔어요.", false)
                )
        ));
        when(aiClient.generateImage(any())).thenAnswer(invocation -> {
            GenerateImageRequest request = invocation.getArgument(0);
            return new GenerateImageResponse(
                    request.requestId(),
                    "data:image/svg+xml;base64,bW9jaw==",
                    "BACKEND_MOCK_STORY_CHARACTER_V1"
            );
        });
        mockSceneSave(201L);
        mockLineSave(1002L);
        when(storyChoiceRepository.saveAndFlush(any())).thenAnswer(invocation -> {
            StoryChoiceEntity saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 300L);
            return saved;
        });

        var response = storyService.chooseStoryDirection(
                1L, 20L, 100L, 1001L, new StoryBranchSelectionRequest(2)
        );

        assertThat(response.choiceId()).isEqualTo(300L);
        assertThat(response.transcript()).isEqualTo("작은 친구가 가리킨 숲길로 간다");
        assertThat(response.nextSceneId()).isEqualTo(201L);
        assertThat(response.nextLineId()).isEqualTo(1002L);
        assertThat(response.generatedContent()).contains(
                "선택한 길로 걸었어요.",
                "모두와 인사하고 집으로 돌아왔어요."
        );
        assertThat(response.status()).isEqualTo("completed");
        assertThat(response.replayed()).isFalse();

        ArgumentCaptor<ContinueStoryRequest> requestCaptor = ArgumentCaptor.forClass(ContinueStoryRequest.class);
        verify(aiClient).continueStory(requestCaptor.capture());
        assertThat(requestCaptor.getValue().branchIntent())
                .isEqualTo("작은 친구가 가리킨 숲길로 간다");
        verifyNoInteractions(storyAudioStorage);
        assertThat(requestCaptor.getValue().currentStoryLineId()).isEqualTo(1001L);

        // 장면 삽화와 주인공 초상화를 각각 만든다.
        ArgumentCaptor<GenerateImageRequest> imageRequestCaptor =
                ArgumentCaptor.forClass(GenerateImageRequest.class);
        verify(aiClient, times(2)).generateImage(imageRequestCaptor.capture());
        assertThat(imageRequestCaptor.getAllValues().get(0).prompt())
                .startsWith("[STORY_SCENE]")
                .contains(template.getTitle())
                .contains("선택한 길로 걸었어요.");
        assertThat(imageRequestCaptor.getAllValues().get(1).prompt())
                .startsWith("[STORY_CHARACTER]")
                .contains(template.getTitle());
        ArgumentCaptor<CharacterEntity> characterCaptor =
                ArgumentCaptor.forClass(CharacterEntity.class);
        verify(characterRepository).saveAndFlush(characterCaptor.capture());
        assertThat(characterCaptor.getValue().getStory()).isSameAs(story);
        assertThat(characterCaptor.getValue().getStudent()).isSameAs(student);
        assertThat(characterCaptor.getValue().getImageUrl())
                .isEqualTo("data:image/svg+xml;base64,bW9jaw==");
        assertThat(characterCaptor.getValue().getName()).endsWith(" 주인공");
    }

    @Test
    void 생성된_대사는_형태소분석_결과와_함께_저장된다() {
        when(studentRepository.findByIdAndTeacherId(20L, 1L)).thenReturn(Optional.of(student));
        when(storyTemplateRepository.findById(30L)).thenReturn(Optional.of(template));
        when(storyRepository.saveAndFlush(any(StoryEntity.class))).thenAnswer(invocation -> {
            StoryEntity saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 100L);
            ReflectionTestUtils.setField(saved, "createdAt", LocalDateTime.of(2026, 7, 22, 10, 0));
            return saved;
        });
        when(aiClient.generateStory(any())).thenReturn(new GenerateStoryResponse(
                "ignored-by-service-mock", 1, 50, false,
                List.of(
                        new GeneratedStoryLine("토끼가 깡충 뛰었어요.", false),
                        new GeneratedStoryLine("어디로 갈까요?", true, branchPrompt())
                )
        ));
        mockSceneSave(200L);
        mockLineSave(1000L);

        storyService.startStory(1L, 20L, 30L);

        ArgumentCaptor<List<StoryLineEntity>> linesCaptor = listCaptor();
        verify(storyLineRepository).saveAllAndFlush(linesCaptor.capture());
        StoryLineEntity first = linesCaptor.getValue().getFirst();
        var analysis = storyLineContentService.analysisOf(first);

        assertThat(storyLineContentService.textOf(first)).isEqualTo("토끼가 깡충 뛰었어요.");
        assertThat(analysis.path("analyzerVersion").asText()).isEqualTo("KOREAN_ANALYZER_V1");
        assertThat(analysis.path("words")).hasSize(3);
        assertThat(analysis.path("words").get(0).path("surface").asText()).isEqualTo("토끼가");
        assertThat(analysis.path("words").get(0).path("featureCodes")).isNotEmpty();
    }

    @Test
    void 대사를_읽으면_단어별로_시도_로그를_적재하고_약점_프로파일을_갱신한다() {
        StoryEntity story = story(100L);
        StoryLineEntity line = line(1000L, story, null, false, "토끼가 뛰었어요.", 1, null);
        when(studentRepository.findByIdAndTeacherId(20L, 1L)).thenReturn(Optional.of(student));
        when(storyRepository.findByIdAndStudentId(100L, 20L)).thenReturn(Optional.of(story));
        when(storyLineRepository.findByIdAndStoryId(1000L, 100L)).thenReturn(Optional.of(line));
        when(aiClient.transcribeSpeech(any(), eq(20L), eq("토끼가 뛰었어요."), any()))
                .thenReturn(new SpeechTranscriptionResponse("req", "토끼가 뛰었어요", 0.93, 1_200));
        when(pronunciationAnalysisAdapter.analyze(any())).thenReturn(new PronunciationAnalysisResult(
                "req", 91.0, 88.0, 100.0, 90.0, 0.95, "PRONUNCIATION_MOCK_V1",
                List.of(
                        new PronunciationWordResult(0, "토끼가", 94.0, "None", 0, 400),
                        new PronunciationWordResult(1, "뛰었어요", 55.0, "Mispronunciation", 500, 600)
                )
        ));
        when(wordAttemptLogRepository.findAllByStoryLineIdAndFinalAttemptTrue(1000L))
                .thenReturn(List.of());
        when(wordRepository.findByContent(any())).thenReturn(Optional.empty());
        when(wordRepository.save(any(WordEntity.class))).thenAnswer(i -> i.getArgument(0));
        when(wordAttemptScoreCalculator.meetsPronunciationThreshold(anyInt()))
                .thenAnswer(i -> (int) i.getArgument(0) >= 700);
        when(wordAttemptScoreCalculator.calculate(
                anyInt(), anyBoolean(), anyBoolean(), any(), anyBoolean(),
                anyBoolean(), any(), any(), anyInt(), any()
        )).thenReturn(820);
        when(wordAttemptLogRepository.saveAllAndFlush(anyList())).thenAnswer(invocation -> {
            List<WordAttemptLogEntity> attempts = new ArrayList<>(invocation.getArgument(0));
            AtomicLong nextId = new AtomicLong(5000L);
            attempts.forEach(attempt ->
                    ReflectionTestUtils.setField(attempt, "id", nextId.getAndIncrement()));
            return attempts;
        });

        var response = storyService.transcribeStoryLine(
                1L, 20L, 100L, 1000L,
                new MockMultipartFile("audioFile", "read.webm", "audio/webm", new byte[]{1})
        );

        assertThat(response.readingStatus()).isEqualTo("recognized");
        assertThat(response.pronunciationAccuracyScore()).isEqualTo(91.0);
        assertThat(response.analysisVersion()).isEqualTo("PRONUNCIATION_MOCK_V1");
        assertThat(response.words()).hasSize(2);
        assertThat(response.words().getFirst().expectedText()).isEqualTo("토끼가");
        assertThat(response.words().getFirst().pronunciationAccuracyScore()).isEqualTo(940);
        assertThat(response.words().getFirst().isCorrect()).isTrue();
        assertThat(response.words().getFirst().featureCodes()).isNotEmpty();
        assertThat(response.words().getLast().pronunciationErrorType())
                .isEqualTo("Mispronunciation");
        assertThat(response.words().getLast().isCorrect()).isFalse();

        ArgumentCaptor<List<WordAttemptLogEntity>> attemptsCaptor = listCaptor();
        verify(wordAttemptLogRepository).saveAllAndFlush(attemptsCaptor.capture());
        assertThat(attemptsCaptor.getValue()).allSatisfy(attempt -> {
            assertThat(attempt.getUseLocation()).isEqualTo(WordAttemptUseLocation.STORY);
            assertThat(attempt.getStoryLine()).isSameAs(line);
            assertThat(attempt.getTraining()).isNull();
            assertThat(attempt.getTest()).isNull();
            assertThat(attempt.isFinalAttempt()).isTrue();
        });
        assertThat(attemptsCaptor.getValue()).extracting(WordAttemptLogEntity::getTokenIndex)
                .containsExactly(0, 1);
        verify(studentFeatureProfileService).recalculate(student);
    }

    @Test
    void 같은_대사를_다시_읽으면_이전_시도는_최종이_아니게_된다() {
        StoryEntity story = story(100L);
        StoryLineEntity line = line(1000L, story, null, false, "토끼가", 1, null);
        WordAttemptLogEntity previous = WordAttemptLogEntity.forStory(
                student, new WordEntity("토끼가"), line, "토끼가", true,
                900, 0, 400, false, true, 900, 0
        );
        when(studentRepository.findByIdAndTeacherId(20L, 1L)).thenReturn(Optional.of(student));
        when(storyRepository.findByIdAndStudentId(100L, 20L)).thenReturn(Optional.of(story));
        when(storyLineRepository.findByIdAndStoryId(1000L, 100L)).thenReturn(Optional.of(line));
        when(aiClient.transcribeSpeech(any(), eq(20L), eq("토끼가"), any()))
                .thenReturn(new SpeechTranscriptionResponse("req", "토끼가", 0.9, 400));
        when(pronunciationAnalysisAdapter.analyze(any())).thenReturn(new PronunciationAnalysisResult(
                "req", 95.0, null, null, null, 0.9, "PRONUNCIATION_MOCK_V1",
                List.of(new PronunciationWordResult(0, "토끼가", 95.0, "None", 0, 400))
        ));
        when(wordAttemptLogRepository.findAllByStoryLineIdAndFinalAttemptTrue(1000L))
                .thenReturn(List.of(previous));
        when(wordRepository.findByContent(any())).thenReturn(Optional.of(new WordEntity("토끼가")));
        when(wordAttemptScoreCalculator.meetsPronunciationThreshold(anyInt())).thenReturn(true);
        when(wordAttemptScoreCalculator.calculate(
                anyInt(), anyBoolean(), anyBoolean(), any(), anyBoolean(),
                anyBoolean(), any(), any(), anyInt(), any()
        )).thenReturn(950);
        when(wordAttemptLogRepository.saveAllAndFlush(anyList()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        storyService.transcribeStoryLine(
                1L, 20L, 100L, 1000L,
                new MockMultipartFile("audioFile", "read.webm", "audio/webm", new byte[]{1})
        );

        assertThat(previous.isFinalAttempt()).isFalse();
    }

    @Test
    void JSON_전환_이전에_저장된_평문_대사는_읽힐_때_분석이_채워진다() {
        StoryEntity story = story(100L);
        StoryLineEntity line = line(1000L, story, null, false, "토끼가 깡충 뛰었어요.", 1, null);
        ReflectionTestUtils.setField(line, "content", "토끼가 깡충 뛰었어요.");
        ownedStory(story);
        when(storyLineRepository.findByIdAndStoryId(1000L, 100L)).thenReturn(Optional.of(line));

        var response = storyService.getStoryLine(1L, 20L, 100L, 1000L);

        assertThat(response.lineText()).isEqualTo("토끼가 깡충 뛰었어요.");
        assertThat(response.analysis().path("words")).hasSize(3);
        assertThat(line.getContent()).startsWith("{");
        assertThat(storyLineContentService.analysisOf(line).path("analyzerVersion").asText())
                .isEqualTo("KOREAN_ANALYZER_V1");
    }

    @Test
    void 저장된_분기를_재요청하면_음성과_AI를_다시_처리하지_않는다() {
        StoryEntity story = story(100L);
        StoryLineEntity choiceLine = line(1001L, story, null, true, "어떻게 할까요?", 2,
                LocalDateTime.of(2026, 7, 22, 10, 10));
        StoryLineEntity nextLine = line(1002L, story, choiceLine, false, "친구를 만났어요.", 1,
                null);
        StoryChoiceEntity choice = new StoryChoiceEntity(choiceLine, "친구를 따라간다");
        ReflectionTestUtils.setField(choice, "id", 300L);
        ownedStory(story);
        when(storyLineRepository.findByIdAndStoryIdForUpdate(1001L, 100L))
                .thenReturn(Optional.of(choiceLine));
        when(storyChoiceRepository.findByStoryLineId(1001L)).thenReturn(Optional.of(choice));
        when(storyLineRepository.findAllByStoryIdOrderBySequenceNoAsc(100L))
                .thenReturn(List.of(choiceLine, nextLine));

        var response = storyService.chooseStoryDirection(
                1L, 20L, 100L, 1001L,
                new MockMultipartFile("audioFile", "answer.webm", "audio/webm", new byte[]{1})
        );

        assertThat(response.choiceId()).isEqualTo(300L);
        assertThat(response.nextLineId()).isEqualTo(1002L);
        assertThat(response.replayed()).isTrue();
        verifyNoInteractions(aiClient, storyAudioStorage);
    }

    @Test
    void 마지막_대사가_아닌_분기에는_답할_수_없다() {
        StoryEntity story = story(100L);
        StoryLineEntity oldChoiceLine = line(1001L, story, null, true, "예전 선택", 1,
                LocalDateTime.of(2026, 7, 22, 10, 10));
        StoryLineEntity currentChoiceLine = line(1002L, story, oldChoiceLine, true, "현재 선택", 2,
                LocalDateTime.of(2026, 7, 22, 10, 11));
        ownedStory(story);
        when(storyLineRepository.findByIdAndStoryIdForUpdate(1001L, 100L))
                .thenReturn(Optional.of(oldChoiceLine));
        when(storyChoiceRepository.findByStoryLineId(1001L)).thenReturn(Optional.empty());
        when(storyLineRepository.findFirstByStoryIdOrderBySequenceNoDesc(100L))
                .thenReturn(Optional.of(currentChoiceLine));

        assertThatThrownBy(() -> storyService.chooseStoryDirection(
                1L, 20L, 100L, 1001L,
                new MockMultipartFile("audioFile", "answer.webm", "audio/webm", new byte[]{1})
        ))
                .isInstanceOf(ConflictException.class)
                .hasMessage("현재 마지막 분기 장면에만 답할 수 있습니다.");

        verifyNoInteractions(aiClient, storyAudioStorage);
    }

    @Test
    void 읽지_않은_분기에는_답할_수_없다() {
        StoryEntity story = story(100L);
        StoryLineEntity unreadChoiceLine = line(1001L, story, null, true, "어떻게 할까요?", 1, null);
        ownedStory(story);
        when(storyLineRepository.findByIdAndStoryIdForUpdate(1001L, 100L))
                .thenReturn(Optional.of(unreadChoiceLine));
        when(storyChoiceRepository.findByStoryLineId(1001L)).thenReturn(Optional.empty());
        when(storyLineRepository.findFirstByStoryIdOrderBySequenceNoDesc(100L))
                .thenReturn(Optional.of(unreadChoiceLine));

        assertThatThrownBy(() -> storyService.chooseStoryDirection(
                1L, 20L, 100L, 1001L,
                new MockMultipartFile("audioFile", "answer.webm", "audio/webm", new byte[]{1})
        ))
                .isInstanceOf(ConflictException.class)
                .hasMessage("장면을 읽은 후 선택지를 제출할 수 있습니다.");

        verifyNoInteractions(aiClient, storyAudioStorage);
    }

    @Test
    void 완료한_이야기를_대사와_고른_답과_이야기_친구로_다시_읽는다() {
        StoryEntity story = story(100L);
        ReflectionTestUtils.setField(story, "status", StoryStatus.COMPLETED);
        StoryLineEntity firstLine = line(1000L, story, null, false, "숲에 도착했어요.", 1,
                LocalDateTime.of(2026, 7, 22, 10, 5));
        StoryLineEntity choiceLine = line(1001L, story, firstLine, true, "어떻게 할까요?", 2,
                LocalDateTime.of(2026, 7, 22, 10, 10));
        ownedStory(story);
        when(storyLineRepository.findAllByStoryIdOrderBySequenceNoAsc(100L))
                .thenReturn(List.of(firstLine, choiceLine));
        StoryChoiceEntity choice = new StoryChoiceEntity(choiceLine, "작은 친구가 가리킨 숲길로 간다");
        ReflectionTestUtils.setField(choice, "createdAt", LocalDateTime.of(2026, 7, 22, 10, 11));
        when(storyChoiceRepository.findAllByStoryLineIdIn(List.of(1001L)))
                .thenReturn(List.of(choice));
        CharacterEntity friend = new CharacterEntity(
                student, story, "data:image/svg+xml;base64,bW9jaw==", "별빛 숲 주인공"
        );
        ReflectionTestUtils.setField(friend, "id", 400L);
        when(characterRepository.findFirstByStoryIdOrderByCreatedAtDesc(100L))
                .thenReturn(Optional.of(friend));

        var response = storyService.reviewStory(1L, 20L, 100L);

        assertThat(response.status()).isEqualTo(StoryStatus.COMPLETED);
        assertThat(response.title()).isEqualTo(template.getTitle());
        assertThat(response.storyLines()).extracting(StoryLineResponse::lineId)
                .containsExactly(1000L, 1001L);
        assertThat(response.branchChoices()).singleElement().satisfies(branch -> {
            assertThat(branch.lineId()).isEqualTo(1001L);
            assertThat(branch.selectedText()).isEqualTo("작은 친구가 가리킨 숲길로 간다");
        });
        assertThat(response.storyFriend().characterId()).isEqualTo(400L);
        assertThat(response.storyFriend().name()).isEqualTo("별빛 숲 주인공");
        // 다시 읽기는 조회일 뿐이므로 실시간 이벤트를 일으키지 않는다.
        verifyNoInteractions(realtimeEventPublisher);
    }

    @Test
    void 진행_중인_이야기는_다시_읽기를_거부한다() {
        StoryEntity story = story(100L);
        ownedStory(story);

        assertThatThrownBy(() -> storyService.reviewStory(1L, 20L, 100L))
                .isInstanceOf(ConflictException.class)
                .hasMessage("완료된 스토리만 다시 볼 수 있습니다.");
    }

    @Test
    void 이야기_친구가_없어도_다시_읽기는_가능하다() {
        StoryEntity story = story(100L);
        ReflectionTestUtils.setField(story, "status", StoryStatus.COMPLETED);
        StoryLineEntity firstLine = line(1000L, story, null, false, "숲에 도착했어요.", 1,
                LocalDateTime.of(2026, 7, 22, 10, 5));
        ownedStory(story);
        when(storyLineRepository.findAllByStoryIdOrderBySequenceNoAsc(100L))
                .thenReturn(List.of(firstLine));
        when(characterRepository.findFirstByStoryIdOrderByCreatedAtDesc(100L))
                .thenReturn(Optional.empty());

        var response = storyService.reviewStory(1L, 20L, 100L);

        assertThat(response.storyFriend()).isNull();
        assertThat(response.branchChoices()).isEmpty();
    }

    private void ownedStory(StoryEntity story) {
        when(studentRepository.findByIdAndTeacherId(20L, 1L)).thenReturn(Optional.of(student));
        when(storyRepository.findByIdAndStudentId(100L, 20L)).thenReturn(Optional.of(story));
    }

    private StoryEntity story(Long id) {
        StoryEntity story = new StoryEntity(student, template);
        ReflectionTestUtils.setField(story, "id", id);
        ReflectionTestUtils.setField(story, "createdAt", LocalDateTime.of(2026, 7, 22, 10, 0));
        return story;
    }

    private StoryLineEntity line(Long id, StoryEntity story, StoryLineEntity previous, boolean requiresBranchInput,
                                 String content, int sequenceNo, LocalDateTime readAt) {
        StorySceneEntity scene = new StorySceneEntity(story, null, 1);
        ReflectionTestUtils.setField(scene, "id", 200L);
        StoryLineEntity line = new StoryLineEntity(
                previous,
                scene,
                requiresBranchInput,
                content,
                requiresBranchInput ? writeBranchPrompt() : null,
                sequenceNo
        );
        ReflectionTestUtils.setField(line, "id", id);
        ReflectionTestUtils.setField(line, "createdAt", LocalDateTime.of(2026, 7, 22, 10, sequenceNo));
        ReflectionTestUtils.setField(line, "readAt", readAt);
        return line;
    }

    private GeneratedStoryBranchPrompt branchPrompt() {
        return new GeneratedStoryBranchPrompt(List.of(
                new GeneratedStoryBranchOption(1, "반짝이는 별빛 길로 간다"),
                new GeneratedStoryBranchOption(2, "작은 친구가 가리킨 숲길로 간다"),
                new GeneratedStoryBranchOption(3, "맑은 시냇물 길을 따라간다")
        ));
    }

    private String writeBranchPrompt() {
        return objectMapper.writeValueAsString(branchPrompt());
    }

    private void mockLineSave(long firstId) {
        when(storyLineRepository.saveAllAndFlush(anyList())).thenAnswer(invocation -> {
            List<StoryLineEntity> lines = new ArrayList<>(invocation.getArgument(0));
            AtomicLong nextId = new AtomicLong(firstId);
            lines.forEach(line -> {
                ReflectionTestUtils.setField(line, "id", nextId.getAndIncrement());
                ReflectionTestUtils.setField(line, "createdAt", LocalDateTime.of(2026, 7, 22, 11, line.getSequenceNo()));
            });
            return lines;
        });
    }

    @Test
    void 장면을_만들_때_대사로_삽화를_함께_생성한다() {
        when(studentRepository.findByIdAndTeacherId(20L, 1L)).thenReturn(Optional.of(student));
        when(storyTemplateRepository.findById(30L)).thenReturn(Optional.of(template));
        when(storyRepository.saveAndFlush(any(StoryEntity.class))).thenAnswer(invocation -> {
            StoryEntity saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 100L);
            return saved;
        });
        when(aiClient.generateStory(any())).thenReturn(new GenerateStoryResponse(
                "ignored", 1, 50, false,
                List.of(
                        new GeneratedStoryLine("토끼가 깡충 뛰었어요.", false),
                        new GeneratedStoryLine("어디로 갈까요?", true, branchPrompt())
                )
        ));
        when(aiClient.generateImage(any())).thenAnswer(invocation -> new GenerateImageResponse(
                ((GenerateImageRequest) invocation.getArgument(0)).requestId(),
                "https://images.example.invalid/scene.png",
                "MOCK_IMAGE_V1"
        ));
        mockSceneSave(200L);
        mockLineSave(1000L);

        storyService.startStory(1L, 20L, 30L);

        ArgumentCaptor<StorySceneEntity> sceneCaptor =
                ArgumentCaptor.forClass(StorySceneEntity.class);
        verify(storySceneRepository).saveAndFlush(sceneCaptor.capture());
        assertThat(sceneCaptor.getValue().getImageUrl())
                .isEqualTo("https://images.example.invalid/scene.png");

        ArgumentCaptor<GenerateImageRequest> imageCaptor =
                ArgumentCaptor.forClass(GenerateImageRequest.class);
        verify(aiClient).generateImage(imageCaptor.capture());
        assertThat(imageCaptor.getValue().prompt())
                .startsWith("[STORY_SCENE]")
                .contains(template.getTitle())
                .contains("토끼가 깡충 뛰었어요.");
    }

    @Test
    void 삽화_생성이_실패해도_이야기는_진행한다() {
        when(studentRepository.findByIdAndTeacherId(20L, 1L)).thenReturn(Optional.of(student));
        when(storyTemplateRepository.findById(30L)).thenReturn(Optional.of(template));
        when(storyRepository.saveAndFlush(any(StoryEntity.class))).thenAnswer(invocation -> {
            StoryEntity saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 100L);
            return saved;
        });
        when(aiClient.generateStory(any())).thenReturn(new GenerateStoryResponse(
                "ignored", 1, 50, false,
                List.of(
                        new GeneratedStoryLine("토끼가 깡충 뛰었어요.", false),
                        new GeneratedStoryLine("어디로 갈까요?", true, branchPrompt())
                )
        ));
        when(aiClient.generateImage(any()))
                .thenThrow(new AiClientException("이미지 서버가 응답하지 않습니다."));
        mockSceneSave(200L);
        mockLineSave(1000L);

        // 삽화 한 장 때문에 읽기 학습이 막히면 안 된다.
        storyService.startStory(1L, 20L, 30L);

        ArgumentCaptor<StorySceneEntity> sceneCaptor =
                ArgumentCaptor.forClass(StorySceneEntity.class);
        verify(storySceneRepository).saveAndFlush(sceneCaptor.capture());
        assertThat(sceneCaptor.getValue().getImageUrl()).isNull();
        verify(storyLineRepository).saveAllAndFlush(any());
    }

    private void mockSceneSave(long id) {
        when(storySceneRepository.countByStoryId(anyLong())).thenReturn(id == 200L ? 0L : 1L);
        when(storySceneRepository.saveAndFlush(any())).thenAnswer(invocation -> {
            StorySceneEntity scene = invocation.getArgument(0);
            ReflectionTestUtils.setField(scene, "id", id);
            return scene;
        });
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private <T> ArgumentCaptor<List<T>> listCaptor() {
        return ArgumentCaptor.forClass((Class) List.class);
    }
}
