package com.iread.backend.story.admin.service;

import com.iread.backend.gaze.domain.GazeAnalysisResultEntity;
import com.iread.backend.gaze.domain.GazeCalibrationStatus;
import com.iread.backend.gaze.domain.GazeContentType;
import com.iread.backend.gaze.domain.GazeSessionEntity;
import com.iread.backend.gaze.domain.GazeSessionStatus;
import com.iread.backend.gaze.app.service.GazeDataStorage;
import com.iread.backend.gaze.repository.GazeAnalysisResultRepository;
import com.iread.backend.gaze.repository.GazeSessionRepository;
import com.iread.backend.ai.client.AiClient;
import com.iread.backend.ai.dto.req.GenerateImageRequest;
import com.iread.backend.ai.dto.res.GenerateImageResponse;
import com.iread.backend.global.storage.FileStorage;
import com.iread.backend.global.storage.LoadedFile;
import com.iread.backend.story.admin.repository.StoryPageEditAuditRepository;
import com.iread.backend.story.admin.dto.req.StoryPageUpdateRequest;
import com.iread.backend.story.admin.dto.res.StoryHistoryDetailResponse;
import com.iread.backend.story.admin.dto.res.StoryHistoryResponse;
import com.iread.backend.story.analysis.StoryLineContentService;
import com.iread.backend.story.domain.StoryChoiceEntity;
import com.iread.backend.story.domain.StoryEntity;
import com.iread.backend.story.domain.StoryLineEntity;
import com.iread.backend.story.domain.StorySceneEntity;
import com.iread.backend.story.domain.StoryStatus;
import com.iread.backend.story.domain.StoryTemplateEntity;
import com.iread.backend.story.generation.StorySceneImagePrompt;
import com.iread.backend.story.repository.StoryChoiceRepository;
import com.iread.backend.story.repository.StoryLineRepository;
import com.iread.backend.story.repository.StoryRepository;
import com.iread.backend.story.repository.StorySceneRepository;
import com.iread.backend.story.repository.StoryTemplateRepository;
import com.iread.backend.student.domain.StudentEntity;
import com.iread.backend.student.repository.StudentRepository;
import com.iread.backend.training.analysis.KoreanTextAnalysis;
import com.iread.backend.training.analysis.KoreanTextAnalyzer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StoryAdminServiceTest {

    @Mock StudentRepository studentRepository;
    @Mock StoryRepository storyRepository;
    @Mock StorySceneRepository storySceneRepository;
    @Mock StoryTemplateRepository storyTemplateRepository;
    @Mock StoryLineRepository storyLineRepository;
    @Mock StoryChoiceRepository storyChoiceRepository;
    @Mock GazeSessionRepository gazeSessionRepository;
    @Mock GazeAnalysisResultRepository gazeAnalysisResultRepository;
    @Mock GazeDataStorage gazeDataStorage;
    @Mock StoryPageEditAuditRepository storyPageEditAuditRepository;
    @Mock AiClient aiClient;
    @Mock FileStorage fileStorage;
    @Mock KoreanTextAnalyzer koreanTextAnalyzer;

    private StoryAdminService service;
    private StudentEntity student;
    private StoryTemplateEntity template;
    private StoryEntity story;
    private StoryLineEntity firstLine;
    private StoryLineEntity secondLine;
    private GazeSessionEntity session;
    private GazeAnalysisResultEntity analysis;

    @BeforeEach
    void setUp() {
        var objectMapper = JsonMapper.builder().build();
        service = new StoryAdminService(
                studentRepository,
                storyRepository,
                storySceneRepository,
                storyTemplateRepository,
                storyLineRepository,
                storyChoiceRepository,
                gazeSessionRepository,
                gazeAnalysisResultRepository,
                gazeDataStorage,
                new StoryGazeWordAnalysisService(),
                new StoryLineContentService(koreanTextAnalyzer, objectMapper),
                objectMapper,
                storyPageEditAuditRepository,
                aiClient,
                fileStorage
        );
        student = StudentEntity.builder().name("student").build();
        ReflectionTestUtils.setField(student, "id", 10L);
        template = new StoryTemplateEntity("Forest", "Story template");
        ReflectionTestUtils.setField(template, "id", 20L);
        story = new StoryEntity(student, template);
        ReflectionTestUtils.setField(story, "id", 30L);
        ReflectionTestUtils.setField(story, "createdAt", LocalDateTime.of(2026, 7, 30, 9, 0));
        ReflectionTestUtils.setField(story, "status", StoryStatus.COMPLETED);
        ReflectionTestUtils.setField(story, "progress", 100);

        StorySceneEntity firstScene = new StorySceneEntity(story, "https://cdn/scene.png", 1);
        ReflectionTestUtils.setField(firstScene, "id", 40L);
        StorySceneEntity secondScene = new StorySceneEntity(story, null, 2);
        ReflectionTestUtils.setField(secondScene, "id", 41L);
        firstLine = new StoryLineEntity(null, firstScene, false, "First line", 1);
        ReflectionTestUtils.setField(firstLine, "id", 50L);
        firstLine.markRead(LocalDateTime.of(2026, 7, 30, 9, 5));
        secondLine = new StoryLineEntity(firstLine, secondScene, true, "Choose a path", 1);
        ReflectionTestUtils.setField(secondLine, "id", 51L);

        session = new GazeSessionEntity(
                student,
                null,
                null,
                story,
                GazeContentType.STORY,
                GazeCalibrationStatus.SUCCESS,
                LocalDateTime.of(2026, 7, 30, 9, 1)
        );
        ReflectionTestUtils.setField(session, "id", 60L);
        session.end(
                GazeSessionStatus.COMPLETED,
                LocalDateTime.of(2026, 7, 30, 9, 10),
                "{}"
        );
        analysis = new GazeAnalysisResultEntity(
                session,
                6000,
                7,
                2,
                857,
                """
                [{
                  "storyLineId":50,
                  "sequenceNo":1,
                  "surfaceText":"First line",
                  "dwellDurationMs":6000,
                  "fixationCount":7,
                  "regressionCount":2,
                  "firstGazeOffsetMs":100,
                  "lastGazeOffsetMs":6100
                }]
                """,
                """
                [{
                  "fromTargetIndex":0,
                  "toTargetIndex":0,
                  "fromTokenIndex":3,
                  "toTokenIndex":1,
                  "offsetMs":4500
                }]
                """,
                "{\"contentType\":\"STORY\",\"storyId\":30}"
        );
        ReflectionTestUtils.setField(analysis, "id", 70L);
    }

    @Test
    void loadsOnlyAnImageReferencedByTheOwnedStory() {
        String fileName = "123e4567-e89b-12d3-a456-426614174000.png";
        LoadedFile image = new LoadedFile(new byte[]{1, 2, 3}, "image/png");
        allowStudent();
        when(storyRepository.findByIdAndStudentId(30L, 10L)).thenReturn(Optional.of(story));
        when(storySceneRepository.existsByStoryIdAndImageUrlEndingWith(30L, "/" + fileName))
                .thenReturn(true);
        when(fileStorage.load(fileName)).thenReturn(image);

        assertThat(service.getStoryImage(1L, 10L, 30L, fileName)).isSameAs(image);
        verify(fileStorage).load(fileName);
    }

    @Test
    void returnsReadingProgressAndAvailableGazeStatus() {
        allowStudent();
        when(storyRepository.findAllByStudentIdAndStatusNotOrderByCreatedAtDesc(
                10L,
                StoryStatus.DELETED
        )).thenReturn(List.of(story));
        when(storyTemplateRepository.findAllByOrderByIdAsc()).thenReturn(List.of(template));
        allowContext();

        var response = service.getStoryHistory(
                1L,
                10L,
                LocalDate.of(2026, 7, 30),
                LocalDate.of(2026, 7, 30),
                20L,
                0,
                20
        );

        assertThat(response.totalElements()).isEqualTo(1);
        StoryHistoryResponse.StorySummary summary = response.storyHistory().getFirst();
        assertThat(summary.readLineCount()).isEqualTo(1);
        assertThat(summary.totalLineCount()).isEqualTo(2);
        assertThat(summary.readingProgress()).isEqualTo(50);
        assertThat(summary.readingStatus())
                .isEqualTo(StoryHistoryResponse.ReadingStatus.IN_PROGRESS);
        assertThat(summary.gazeAnalysisStatus())
                .isEqualTo(StoryHistoryResponse.GazeAnalysisStatus.AVAILABLE);
    }

    @Test
    void assemblesOrderedPagesAndBranchRecord() {
        secondLine.updateContent("""
                {"text":"Choose a path","analysis":{"analyzerVersion":"test"}}
                """);
        StoryChoiceEntity choice = new StoryChoiceEntity(secondLine, "Take the river path");
        ReflectionTestUtils.setField(choice, "id", 80L);
        ReflectionTestUtils.setField(choice, "createdAt", LocalDateTime.of(2026, 7, 30, 9, 7));
        allowStudent();
        when(storyRepository.findByIdAndStudentId(30L, 10L)).thenReturn(Optional.of(story));
        allowContext();
        when(storyChoiceRepository.findAllByStoryLineIdIn(List.of(50L, 51L)))
                .thenReturn(List.of(choice));

        var response = service.getStoryHistoryDetail(1L, 10L, 30L);

        assertThat(response.totalPages()).isEqualTo(2);
        assertThat(response.pages()).extracting(StoryHistoryDetailResponse.StoryPage::pageNo)
                .containsExactly(1, 2);
        assertThat(response.pages().getFirst().imageGenerationStatus())
                .isEqualTo(StoryHistoryDetailResponse.ImageGenerationStatus.AVAILABLE);
        assertThat(response.pages().get(1).imageGenerationStatus())
                .isEqualTo(StoryHistoryDetailResponse.ImageGenerationStatus.NOT_REQUESTED);
        assertThat(response.pages().get(1).textLines())
                .containsExactly("Choose a path");
        assertThat(response.pages().get(1).branchRecord().promptText())
                .isEqualTo("Choose a path");
        assertThat(response.pages().get(1).branchRecord().transcript())
                .isEqualTo("Take the river path");
    }

    @Test
    void teacherUpdatesOnlyUnreadPageContentAndBranchAtExpectedRevision() {
        secondLine.updateBranchPrompt("""
                {"subtitle":"어느 길로 갈까요","options":[
                  {"optionNo":1,"label":"별빛 길로 가요"},
                  {"optionNo":2,"label":"숲길로 가요"},
                  {"optionNo":3,"label":"시냇물 길로 가요"}
                ]}
                """);
        allowStudent();
        when(storyRepository.findByIdAndStudentId(30L, 10L)).thenReturn(Optional.of(story));
        when(storyLineRepository.findByIdAndStoryIdForUpdate(51L, 30L))
                .thenReturn(Optional.of(secondLine));
        String body = """
                하린이는 별빛 다리를 천천히 건너갔어요. 다친 새는 따뜻한 노래로 용기를 냈어요. 두 친구는 환한 언덕에서 함께 웃었어요.
                """.strip();
        when(koreanTextAnalyzer.analyze(body)).thenReturn(new KoreanTextAnalysis(
                body, List.of(), List.of(), "test", "test", "test"
        ));

        var response = service.updateUnreadPage(
                1L,
                10L,
                30L,
                51L,
                new StoryPageUpdateRequest(
                        0L,
                        "새와 건너는 별빛 다리",
                        body,
                        List.of("새를 꼭 안아 줘요", "노래하며 건너가요", "잠시 쉬어 가요")
                )
        );

        assertThat(response.revision()).isEqualTo(1L);
        assertThat(response.body()).isEqualTo(body);
        assertThat(response.subtitle()).isEqualTo("새와 건너는 별빛 다리");
        assertThat(response.choices()).containsExactly(
                "새를 꼭 안아 줘요", "노래하며 건너가요", "잠시 쉬어 가요"
        );
        assertThat(response.editable()).isTrue();
        verify(storyLineRepository).saveAndFlush(secondLine);
        verify(storyPageEditAuditRepository).save(any());
    }

    @Test
    void teacherRegeneratesImageWithTheSameStoryScenePromptAsInitialGeneration() {
        allowStudent();
        when(storyRepository.findByIdAndStudentId(30L, 10L)).thenReturn(Optional.of(story));
        when(storyLineRepository.findByIdAndStoryIdForUpdate(51L, 30L))
                .thenReturn(Optional.of(secondLine));
        when(aiClient.generateImage(any())).thenReturn(new GenerateImageResponse(
                "teacher-story-image-51-0",
                "/uploads/images/regenerated.png",
                "test"
        ));

        var response = service.regenerateUnreadPageImage(1L, 10L, 30L, 51L, 0L);

        ArgumentCaptor<GenerateImageRequest> requestCaptor =
                ArgumentCaptor.forClass(GenerateImageRequest.class);
        verify(aiClient).generateImage(requestCaptor.capture());
        GenerateImageRequest request = requestCaptor.getValue();
        assertThat(request.requestId()).isEqualTo("teacher-story-image-51-0");
        assertThat(request.storyTemplateId()).isEqualTo(20L);
        assertThat(request.prompt()).isEqualTo(StorySceneImagePrompt.build(
                "Forest",
                "Choose a path"
        ));
        assertThat(request.prompt()).startsWith("[STORY_SCENE] ");
        assertThat(response.imageUrl()).isEqualTo("/uploads/images/regenerated.png");
    }

    @Test
    void mapsStoredGazeJsonToPageMetrics() {
        allowStudent();
        when(storyRepository.findByIdAndStudentId(30L, 10L)).thenReturn(Optional.of(story));
        allowContext();
        when(gazeDataStorage.load("{}")).thenReturn("""
                {"rawData":{
                  "replayWords":[{"questionNo":1,"storyLineId":50,"tokenIndex":0,"text":"First","dwellMs":160}],
                  "samples":[
                    {"x":10,"y":20,"pageNo":1,"storyLineId":50,"tokenIndex":0,"text":"First","capturedAtMs":1000},
                    {"x":11,"y":21,"pageNo":1,"storyLineId":50,"tokenIndex":0,"text":"First","capturedAtMs":1080},
                    {"x":12,"y":22,"pageNo":1,"storyLineId":50,"tokenIndex":1,"text":"line","capturedAtMs":1160}
                  ]
                }}
                """);

        var response = service.getStoryGazeAnalysis(1L, 10L, 30L);

        assertThat(response.gazeSessionId()).isEqualTo(60L);
        assertThat(response.gazeAnalysisId()).isEqualTo(70L);
        assertThat(response.pageMetrics()).hasSize(1);
        var metric = response.pageMetrics().getFirst();
        assertThat(metric.storyLineId()).isEqualTo(50L);
        assertThat(metric.pageNo()).isEqualTo(1);
        assertThat(metric.regressionCount()).isEqualTo(2);
        assertThat(metric.regressions()).hasSize(1);
        assertThat(response.wordMetrics()).hasSize(2);
        assertThat(response.wordMetrics().getFirst().text()).isEqualTo("First");
        assertThat(response.wordMetrics().getFirst().firstSeenMs()).isZero();
        assertThat(response.replay().path("words").size()).isEqualTo(1);
        assertThat(response.replay().path("samples").get(0).path("questionNumber").asInt()).isEqualTo(1);
        assertThat(response.replay().path("samples").get(0).has("x")).isFalse();
        assertThat(response.replay().path("samples").get(0).has("y")).isFalse();
        assertThat(response.replay().path("events").size()).isEqualTo(2);
        assertThat(response.analysisMeta().calculationVersion()).isEqualTo("story-gaze-word-v1");
        assertThat(response.analysisMeta().maxSampleGapMs()).isEqualTo(250);
        assertThat(metric.averageFixationTimeMs()).isEqualTo(857);
    }


    @Test
    void usesLatestCompletedSessionWhenNewerSessionFailed() {
        GazeSessionEntity newerFailedSession = new GazeSessionEntity(
                student,
                null,
                null,
                story,
                GazeContentType.STORY,
                GazeCalibrationStatus.SUCCESS,
                LocalDateTime.of(2026, 7, 30, 9, 11)
        );
        ReflectionTestUtils.setField(newerFailedSession, "id", 61L);
        newerFailedSession.fail(LocalDateTime.of(2026, 7, 30, 9, 12));
        allowStudent();
        when(storyRepository.findByIdAndStudentId(30L, 10L)).thenReturn(Optional.of(story));
        when(storyLineRepository.findAllByStoryIdInOrderBySequenceNoAsc(List.of(30L)))
                .thenReturn(List.of(firstLine, secondLine));
        when(gazeSessionRepository
                .findAllByStudentIdAndContentTypeAndStoryIdInOrderByCreatedAtDescIdDesc(
                        10L,
                        GazeContentType.STORY,
                        List.of(30L)
                )).thenReturn(List.of(newerFailedSession, session));
        when(gazeAnalysisResultRepository.findAllByGazeSessionIdIn(List.of(61L, 60L)))
                .thenReturn(List.of(analysis));

        var response = service.getStoryGazeAnalysis(1L, 10L, 30L);

        assertThat(response.gazeSessionId()).isEqualTo(60L);
        assertThat(response.gazeAnalysisId()).isEqualTo(70L);
    }
    private void allowStudent() {
        when(studentRepository.findByIdAndTeacherId(10L, 1L))
                .thenReturn(Optional.of(student));
    }

    private void allowContext() {
        when(storyLineRepository.findAllByStoryIdInOrderBySequenceNoAsc(List.of(30L)))
                .thenReturn(List.of(firstLine, secondLine));
        when(gazeSessionRepository
                .findAllByStudentIdAndContentTypeAndStoryIdInOrderByCreatedAtDescIdDesc(
                        10L,
                        GazeContentType.STORY,
                        List.of(30L)
                )).thenReturn(List.of(session));
        when(gazeAnalysisResultRepository.findAllByGazeSessionIdIn(List.of(60L)))
                .thenReturn(List.of(analysis));
    }
}
