package com.iread.backend.test.app.service;

import com.iread.backend.global.audio.AudioUploadPolicy;
import com.iread.backend.pronunciation.PronunciationAnalysisAdapter;
import com.iread.backend.pronunciation.PronunciationAnalysisRequest;
import com.iread.backend.pronunciation.PronunciationAnalysisResult;
import com.iread.backend.pronunciation.PronunciationWordAligner;
import com.iread.backend.pronunciation.PronunciationWordResult;
import com.iread.backend.realtime.RealtimeEventPublisher;
import com.iread.backend.student.domain.StudentEntity;
import com.iread.backend.student.repository.StudentRepository;
import com.iread.backend.learning.app.service.AppLearningQuestionSupport;
import com.iread.backend.learning.app.dto.LearningResponseType;
import com.iread.backend.learning.app.dto.LearningSubmission;
import com.iread.backend.test.app.dto.req.TestCompleteRequest;
import com.iread.backend.test.app.dto.req.TestRecordingRequest;
import com.iread.backend.test.app.dto.req.TestSubmissionRequest;
import com.iread.backend.test.domain.TestDataEntity;
import com.iread.backend.test.domain.StudentTestEntity;
import com.iread.backend.test.domain.TestStatus;
import com.iread.backend.test.domain.TestCurriculumEntity;
import com.iread.backend.test.repository.StudentTestRepository;
import com.iread.backend.test.repository.TestCurriculumRepository;
import com.iread.backend.test.repository.TestDataRepository;
import com.iread.backend.training.generation.PersonalizedTrainingGenerationService;
import com.iread.backend.training.repository.TrainingTemplateRepository;
import com.iread.backend.training.repository.WordRepository;
import com.iread.backend.wordattempt.domain.WordAttemptLogEntity;
import com.iread.backend.wordattempt.repository.WordAttemptLogRepository;
import com.iread.backend.wordattempt.service.WordAttemptScoreCalculator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.util.unit.DataSize;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AppTestServiceTest {
    @Mock StudentRepository studentRepository;
    @Mock StudentTestRepository testRepository;
    @Mock TestCurriculumRepository testCurriculumRepository;
    @Mock TestDataRepository testDataRepository;
    @Mock TrainingTemplateRepository trainingTemplateRepository;
    @Mock PersonalizedTrainingGenerationService trainingGenerationService;
    @Mock WordRepository wordRepository;
    @Mock WordAttemptLogRepository wordAttemptLogRepository;
    @Mock PronunciationAnalysisAdapter pronunciationAnalysisAdapter;
    @Mock PronunciationWordAligner pronunciationWordAligner;
    @Mock AudioUploadPolicy audioUploadPolicy;
    @Mock WordAttemptScoreCalculator wordAttemptScoreCalculator;
    @Mock ObjectMapper objectMapper;
    @Mock RealtimeEventPublisher realtimeEventPublisher;
    @InjectMocks AppTestService appTestService;

    @Test
    void returnsThreeTracksWithThreePersistedQuestionsEach() {
        StudentEntity student = mock(StudentEntity.class);
        TestCurriculumEntity curriculum = mock(TestCurriculumEntity.class);
        when(studentRepository.findByIdAndTeacherIdForUpdate(20L, 1L))
                .thenReturn(Optional.of(student));
        when(testCurriculumRepository
                .findFirstByStudentIdOrderByCreatedAtDescIdDesc(20L))
                .thenReturn(Optional.of(curriculum));
        when(curriculum.getId()).thenReturn(50L);
        List<StudentTestEntity> tests = java.util.stream.IntStream.rangeClosed(1, 9)
                .mapToObj(sequence -> {
                    StudentTestEntity test = mock(StudentTestEntity.class);
                    when(test.getStatus()).thenReturn(
                            sequence == 1 ? TestStatus.COMPLETED : TestStatus.NOT_STARTED
                    );
                    return test;
                })
                .toList();
        when(tests.get(1).getId()).thenReturn(102L);
        when(tests.get(3).getId()).thenReturn(104L);
        when(tests.get(6).getId()).thenReturn(107L);
        when(testRepository.findAllByTestCurriculumIdOrderBySequenceNoAscIdAsc(50L))
                .thenReturn(tests);

        var plan = appTestService.getChallengePlan(1L, 20L);

        assertThat(plan.totalQuestions()).isEqualTo(9);
        assertThat(plan.completedQuestions()).isEqualTo(1);
        assertThat(plan.tracks()).hasSize(3);
        assertThat(plan.tracks()).allSatisfy(track ->
                assertThat(track.totalQuestions()).isEqualTo(3));
        assertThat(plan.tracks().getFirst().nextTestId()).isEqualTo(102L);
    }

    @Test
    void startsCurrentNotStartedTest() {
        StudentEntity student = mock(StudentEntity.class);
        StudentTestEntity test = mock(StudentTestEntity.class);
        when(studentRepository.findByIdAndTeacherId(20L, 1L)).thenReturn(Optional.of(student));
        when(testRepository
                .findFirstByTestCurriculumStudentIdAndStatusInOrderByTestCurriculumCreatedAtDescSequenceNoAscIdAsc(
                        any(),
                        any()
                ))
                .thenReturn(Optional.of(test));
        when(testRepository.findByIdAndStudentIdForUpdate(30L, 20L))
                .thenReturn(Optional.of(test));
        when(test.getId()).thenReturn(30L);
        when(test.getStatus()).thenReturn(
                TestStatus.NOT_STARTED,
                TestStatus.IN_PROGRESS
        );

        var result = appTestService.start(1L, 20L);

        verify(test).start(any(LocalDateTime.class));
        assertThat(result.testId()).isEqualTo(30L);
        assertThat(result.status()).isEqualTo(TestStatus.IN_PROGRESS);
        assertThat(result.startedAt()).isNotNull();
    }

    @Test
    void repeatedStartReturnsExistingTestSessionWithoutPublishingAnotherEvent() {
        StudentEntity student = mock(StudentEntity.class);
        StudentTestEntity test = mock(StudentTestEntity.class);
        LocalDateTime startedAt = LocalDateTime.of(2026, 7, 31, 10, 30);
        when(studentRepository.findByIdAndTeacherId(20L, 1L))
                .thenReturn(Optional.of(student));
        when(testRepository.findByIdAndStudentIdForUpdate(30L, 20L))
                .thenReturn(Optional.of(test));
        when(test.getId()).thenReturn(30L);
        when(test.getStatus()).thenReturn(TestStatus.IN_PROGRESS);
        when(test.getStartedAt()).thenReturn(startedAt);

        var result = appTestService.start(1L, 20L, 30L);

        assertThat(result.testId()).isEqualTo(30L);
        assertThat(result.startedAt()).isEqualTo(startedAt);
        assertThat(result.status()).isEqualTo(TestStatus.IN_PROGRESS);
        verify(test, never()).start(any(LocalDateTime.class));
        verifyNoInteractions(realtimeEventPublisher);
    }

    @Test
    void completesLastChallengeTestAndCurriculumWithServerTimeWithoutExposingAccuracy() {
        StudentEntity student = mock(StudentEntity.class);
        StudentTestEntity test = mock(StudentTestEntity.class);
        TestCurriculumEntity curriculum = mock(TestCurriculumEntity.class);
        TestDataEntity data = mock(TestDataEntity.class);
        when(studentRepository.findByIdAndTeacherId(20L, 1L)).thenReturn(Optional.of(student));
        when(testRepository.findByIdAndTestCurriculumStudentId(30L, 20L))
                .thenReturn(Optional.of(test));
        when(testRepository.findByIdAndStudentIdForUpdate(30L, 20L))
                .thenReturn(Optional.of(test));
        when(test.getId()).thenReturn(30L);
        when(test.getStartedAt()).thenReturn(LocalDateTime.now().minusSeconds(120));
        when(test.getStatus()).thenReturn(
                TestStatus.IN_PROGRESS,
                TestStatus.IN_PROGRESS,
                TestStatus.COMPLETED
        );
        when(test.getResult()).thenReturn("""
                {
                  "submissions":[
                    {"submissionId":"00000000-0000-0000-0000-000000000001",
                     "questionNo":1,"totalScore":1000}
                  ]
                }
                """);
        when(testDataRepository.findFirstByTestIdOrderByCreatedAtDescIdDesc(30L))
                .thenReturn(Optional.of(data));
        when(data.getGeneratedData()).thenReturn("""
                {"questions":[{"type":"CONSONANT_SOUND_CHOICE"}]}
                """);
        LocalDateTime completedAt = LocalDateTime.of(2026, 7, 27, 15, 0);
        when(test.getFinishedAt()).thenReturn(completedAt);
        when(test.getTestCurriculum()).thenReturn(curriculum);
        when(curriculum.getId()).thenReturn(50L);
        List<StudentTestEntity> curriculumTests = new java.util.ArrayList<>();
        curriculumTests.add(test);
        java.util.stream.IntStream.range(0, 8).forEach(ignored -> {
            StudentTestEntity completed = mock(StudentTestEntity.class);
            when(completed.getStatus()).thenReturn(TestStatus.COMPLETED);
            curriculumTests.add(completed);
        });
        when(testRepository.findAllByTestCurriculumIdOrderBySequenceNoAscIdAsc(50L))
                .thenReturn(curriculumTests);
        ObjectMapper mapper = JsonMapper.builder().build();
        AppTestService service = new AppTestService(
                studentRepository,
                testRepository,
                testDataRepository,
                wordRepository,
                wordAttemptLogRepository,
                pronunciationAnalysisAdapter,
                new PronunciationWordAligner(),
                audioUploadPolicy,
                wordAttemptScoreCalculator,
                mapper,
                new AppLearningQuestionSupport(mapper),
                realtimeEventPublisher
        );

        var result = service.complete(
                1L,
                20L,
                new TestCompleteRequest(30L)
        );

        ArgumentCaptor<LocalDateTime> completedAtCaptor =
                ArgumentCaptor.forClass(LocalDateTime.class);
        ArgumentCaptor<String> resultCaptor = ArgumentCaptor.forClass(String.class);
        verify(test).complete(
                resultCaptor.capture(),
                org.mockito.ArgumentMatchers.eq(new BigDecimal("100.00")),
                completedAtCaptor.capture()
        );
        assertThat(result.testId()).isEqualTo(30L);
        assertThat(result.status()).isEqualTo(TestStatus.COMPLETED);
        assertThat(mapper.readTree(resultCaptor.getValue()).path("solvingTimeSeconds").asLong())
                .isGreaterThanOrEqualTo(120L);
        assertThat(result.completionType()).isEqualTo("TEST_COMPLETED");
        assertThat(result.messageKey()).isEqualTo("TEST_COMPLETE_GREAT_JOB");
        assertThat(result.completedAt()).isEqualTo(completedAt);
        verify(curriculum).complete(any(LocalDateTime.class));
    }

    @Test
    void storesNewSelectionUnderSubmissionsWithEvaluationAndProgress() {
        StudentEntity student = mock(StudentEntity.class);
        StudentTestEntity test = mock(StudentTestEntity.class);
        TestDataEntity data = mock(TestDataEntity.class);
        when(studentRepository.findByIdAndTeacherId(20L, 1L)).thenReturn(Optional.of(student));
        when(testRepository.findByIdAndStudentIdForUpdate(30L, 20L))
                .thenReturn(Optional.of(test));
        when(test.getId()).thenReturn(30L);
        when(test.getStatus()).thenReturn(TestStatus.IN_PROGRESS);
        when(test.getResult()).thenReturn(null);
        when(testDataRepository.findFirstByTestIdOrderByCreatedAtDescIdDesc(30L))
                .thenReturn(Optional.of(data));
        when(data.getGeneratedData()).thenReturn("""
                {
                  "questions":[{
                    "questionNo":1,
                    "type":"CONSONANT_SOUND_CHOICE",
                    "content":{"audioText":"ㄱ","choices":["ㄱ","ㄴ"]},
                    "answer":{"answerIndex":0}
                  }]
                }
                """);
        ObjectMapper mapper = JsonMapper.builder().build();
        AppTestService service = new AppTestService(
                studentRepository,
                testRepository,
                testDataRepository,
                wordRepository,
                wordAttemptLogRepository,
                pronunciationAnalysisAdapter,
                new PronunciationWordAligner(),
                audioUploadPolicy,
                wordAttemptScoreCalculator,
                mapper,
                new AppLearningQuestionSupport(mapper),
                realtimeEventPublisher
        );
        UUID submissionId = UUID.fromString("00000000-0000-0000-0000-000000000010");

        var progress = service.saveSelection(
                1L,
                20L,
                1,
                new TestSubmissionRequest(
                        30L,
                        new LearningSubmission(
                                submissionId,
                                LearningResponseType.SINGLE_CHOICE,
                                mapper.createObjectNode().put("selectedIndex", 0)
                        )
                )
        );

        ArgumentCaptor<String> resultCaptor = ArgumentCaptor.forClass(String.class);
        verify(test).updateResult(resultCaptor.capture());
        var stored = mapper.readTree(resultCaptor.getValue());
        assertThat(stored.path("schemaVersion").asInt()).isEqualTo(2);
        assertThat(stored.path("questions").isMissingNode()).isTrue();
        assertThat(stored.path("submissions").size()).isEqualTo(1);
        var submission = stored.path("submissions").get(0);
        assertThat(submission.path("submissionId").asText()).isEqualTo(submissionId.toString());
        assertThat(submission.path("questionNo").asInt()).isEqualTo(1);
        assertThat(submission.path("responseType").asText()).isEqualTo("SINGLE_CHOICE");
        assertThat(submission.path("response").path("selectedIndex").asInt()).isZero();
        assertThat(submission.path("correct").asBoolean()).isTrue();
        assertThat(submission.path("totalScore").asInt()).isEqualTo(1000);
        assertThat(submission.path("progress").path("testCompleted").asBoolean()).isTrue();
        assertThat(progress.testCompleted()).isTrue();
    }

    @Test
    void returnsExistingProgressForSecondSubmissionToSameTestQuestion() {
        StudentEntity student = mock(StudentEntity.class);
        StudentTestEntity test = mock(StudentTestEntity.class);
        TestDataEntity data = mock(TestDataEntity.class);
        when(studentRepository.findByIdAndTeacherId(20L, 1L)).thenReturn(Optional.of(student));
        when(testRepository.findByIdAndStudentIdForUpdate(30L, 20L))
                .thenReturn(Optional.of(test));
        when(test.getStatus()).thenReturn(TestStatus.IN_PROGRESS);
        when(test.getResult()).thenReturn("""
                {
                  "submissions":[{
                    "submissionId":"00000000-0000-0000-0000-000000000001",
                    "questionNo":1,
                    "totalScore":0
                  }]
                }
                """);
        when(testDataRepository.findFirstByTestIdOrderByCreatedAtDescIdDesc(30L))
                .thenReturn(Optional.of(data));
        when(data.getGeneratedData()).thenReturn("""
                {
                  "questions":[{
                    "type":"CONSONANT_SOUND_CHOICE",
                    "content":{"audioText":"ㄱ","choices":["ㄱ","ㄴ"]},
                    "answer":{"answerIndex":0}
                  }]
                }
                """);
        ObjectMapper mapper = JsonMapper.builder().build();
        AppTestService service = new AppTestService(
                studentRepository,
                testRepository,
                testDataRepository,
                wordRepository,
                wordAttemptLogRepository,
                pronunciationAnalysisAdapter,
                new PronunciationWordAligner(),
                audioUploadPolicy,
                wordAttemptScoreCalculator,
                mapper,
                new AppLearningQuestionSupport(mapper),
                realtimeEventPublisher
        );

        var progress = service.saveSelection(
                1L,
                20L,
                1,
                new TestSubmissionRequest(
                        30L,
                        new LearningSubmission(
                                UUID.randomUUID(),
                                LearningResponseType.SINGLE_CHOICE,
                                mapper.createObjectNode().put("selectedIndex", 0)
                        )
                )
        );

        assertThat(progress.submissionId())
                .isEqualTo(UUID.fromString("00000000-0000-0000-0000-000000000001"));
        assertThat(progress.accepted()).isTrue();
        assertThat(progress.questionNumber()).isEqualTo(1);
        assertThat(progress.completedQuestions()).isEqualTo(1);
        assertThat(progress.nextQuestionNumber()).isNull();
        assertThat(progress.testCompleted()).isTrue();
        verify(test, never()).updateResult(any(String.class));
    }

    @Test
    void recordingStoresAzureOmissionAndOffsetsAndLinksParentAnalysis() {
        StudentEntity student = mock(StudentEntity.class);
        StudentTestEntity test = mock(StudentTestEntity.class);
        TestDataEntity data = mock(TestDataEntity.class);
        var word = mock(com.iread.backend.training.domain.WordEntity.class);
        when(studentRepository.findByIdAndTeacherId(20L, 1L))
                .thenReturn(Optional.of(student));
        when(testRepository.findByIdAndStudentIdForUpdate(30L, 20L))
                .thenReturn(Optional.of(test));
        when(test.getId()).thenReturn(30L);
        when(test.getStatus()).thenReturn(TestStatus.IN_PROGRESS);
        when(test.getResult()).thenReturn(null);
        when(testDataRepository.findFirstByTestIdOrderByCreatedAtDescIdDesc(30L))
                .thenReturn(Optional.of(data));
        when(data.getGeneratedData()).thenReturn("""
                {"questions":[{"type":"WORD_READING"}]}
                """);
        when(wordRepository.findById(40L)).thenReturn(Optional.of(word));
        when(word.getId()).thenReturn(40L);
        when(word.getContent()).thenReturn("학교");
        PronunciationAnalysisResult analysis = new PronunciationAnalysisResult(
                "azure-test-1",
                45.0,
                70.0,
                50.0,
                48.0,
                0.9,
                "azure-speech-v1",
                List.of(
                        new PronunciationWordResult(
                                0,
                                "정말",
                                85.0,
                                "Insertion",
                                10,
                                80
                        ),
                        new PronunciationWordResult(
                                1,
                                "학교",
                                null,
                                "Omission",
                                120,
                                340
                        )
                )
        );
        when(pronunciationAnalysisAdapter.analyze(any())).thenReturn(analysis);
        when(wordAttemptScoreCalculator.meetsPronunciationThreshold(0)).thenReturn(false);
        when(wordAttemptScoreCalculator.calculate(
                0,
                true,
                true,
                true,
                true,
                false,
                null,
                null,
                0,
                false
        )).thenReturn(null);
        when(wordAttemptLogRepository
                .findAllByTestIdAndQuestionNoAndFinalAttemptTrue(30L, 1))
                .thenReturn(List.of());
        when(wordAttemptLogRepository.saveAndFlush(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));
        ObjectMapper mapper = JsonMapper.builder().build();
        AppTestService service = new AppTestService(
                studentRepository,
                testRepository,
                testDataRepository,
                wordRepository,
                wordAttemptLogRepository,
                pronunciationAnalysisAdapter,
                new PronunciationWordAligner(),
                new AudioUploadPolicy(
                        DataSize.ofMegabytes(20),
                        "audio/webm,audio/wav,audio/mpeg,audio/mp4"
                ),
                wordAttemptScoreCalculator,
                mapper,
                new AppLearningQuestionSupport(mapper),
                realtimeEventPublisher
        );

        var response = service.saveRecording(
                1L,
                20L,
                1,
                new TestRecordingRequest(
                        30L,
                        40L,
                        new MockMultipartFile(
                                "audioFile",
                                "reading.webm",
                                "audio/webm",
                                new byte[]{1, 2, 3}
                        ),
                        900,
                        1200
                )
        );

        ArgumentCaptor<WordAttemptLogEntity> attemptCaptor =
                ArgumentCaptor.forClass(WordAttemptLogEntity.class);
        verify(wordAttemptLogRepository).saveAndFlush(attemptCaptor.capture());
        WordAttemptLogEntity stored = attemptCaptor.getValue();
        assertThat(stored.getPronunciationAccuracyScore()).isZero();
        assertThat(stored.getSkipped()).isTrue();
        assertThat(stored.getSpeechStartOffsetMs()).isEqualTo(120);
        assertThat(stored.getSpeechEndOffsetMs()).isEqualTo(460);
        assertThat(stored.getTotalScore()).isNull();
        assertThat(response.pronunciationAccuracyScore()).isEqualTo(45.0);
        assertThat(response.totalScore()).isNull();
        assertThat(response.pronunciationErrorType()).isEqualTo("Omission");
        assertThat(response.words()).singleElement()
                .satisfies(result -> assertThat(result.pronunciationAccuracyScore()).isZero());

        ArgumentCaptor<String> resultCaptor = ArgumentCaptor.forClass(String.class);
        verify(test).updateResult(resultCaptor.capture());
        var storedResult = mapper.readTree(resultCaptor.getValue());
        assertThat(storedResult.path("pronunciationAnalyses").get(0)
                .path("insertionCount").asInt()).isEqualTo(1);
        assertThat(storedResult.path("pronunciationAnalyses").get(0)
                .path("analysisVersion").asText()).isEqualTo("azure-speech-v1");
        assertThat(storedResult.path("pronunciationAnalyses").get(0)
                .path("fluencyScore").asDouble()).isEqualTo(70.0);

        ArgumentCaptor<PronunciationAnalysisRequest> requestCaptor =
                ArgumentCaptor.forClass(PronunciationAnalysisRequest.class);
        verify(pronunciationAnalysisAdapter).analyze(requestCaptor.capture());
        assertThat(requestCaptor.getValue().expectedText()).isEqualTo("학교");
        assertThat(requestCaptor.getValue().audio()).containsExactly(1, 2, 3);
    }

    @Test
    void recordingAlignmentFailureDoesNotStoreAttempt() {
        StudentEntity student = mock(StudentEntity.class);
        StudentTestEntity test = mock(StudentTestEntity.class);
        TestDataEntity data = mock(TestDataEntity.class);
        var word = mock(com.iread.backend.training.domain.WordEntity.class);
        when(studentRepository.findByIdAndTeacherId(20L, 1L))
                .thenReturn(Optional.of(student));
        when(testRepository.findByIdAndStudentIdForUpdate(30L, 20L))
                .thenReturn(Optional.of(test));
        when(test.getStatus()).thenReturn(TestStatus.IN_PROGRESS);
        when(testDataRepository.findFirstByTestIdOrderByCreatedAtDescIdDesc(30L))
                .thenReturn(Optional.of(data));
        when(data.getGeneratedData()).thenReturn("""
                {"questions":[{"type":"WORD_READING"}]}
                """);
        when(wordRepository.findById(40L)).thenReturn(Optional.of(word));
        when(word.getContent()).thenReturn("학교");
        when(pronunciationAnalysisAdapter.analyze(any())).thenReturn(
                new PronunciationAnalysisResult(
                        "azure-test-2",
                        80.0,
                        null,
                        null,
                        null,
                        0.8,
                        "azure-speech-v1",
                        List.of(new PronunciationWordResult(
                                0,
                                "학원",
                                80.0,
                                "None",
                                0,
                                300
                        ))
                )
        );
        ObjectMapper mapper = JsonMapper.builder().build();
        AppTestService service = new AppTestService(
                studentRepository,
                testRepository,
                testDataRepository,
                wordRepository,
                wordAttemptLogRepository,
                pronunciationAnalysisAdapter,
                new PronunciationWordAligner(),
                new AudioUploadPolicy(
                        DataSize.ofMegabytes(20),
                        "audio/webm,audio/wav,audio/mpeg,audio/mp4"
                ),
                wordAttemptScoreCalculator,
                mapper,
                new AppLearningQuestionSupport(mapper),
                realtimeEventPublisher
        );

        assertThatThrownBy(() -> service.saveRecording(
                1L,
                20L,
                1,
                new TestRecordingRequest(
                        30L,
                        40L,
                        new MockMultipartFile(
                                "audioFile",
                                "reading.webm",
                                "audio/webm",
                                new byte[]{1}
                        ),
                        null,
                        null
                )
        ))
                .isInstanceOf(com.iread.backend.exception.ConflictException.class)
                .hasMessageContaining("정렬");
        verifyNoInteractions(wordAttemptScoreCalculator);
        verify(wordAttemptLogRepository, org.mockito.Mockito.never())
                .saveAndFlush(any());
    }
}
