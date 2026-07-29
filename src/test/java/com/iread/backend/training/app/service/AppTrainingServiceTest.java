package com.iread.backend.training.app.service;

import com.iread.backend.student.domain.StudentEntity;
import com.iread.backend.global.audio.AudioUploadPolicy;
import com.iread.backend.learning.app.dto.LearningResponseType;
import com.iread.backend.learning.app.dto.LearningSubmission;
import com.iread.backend.learning.app.service.AppLearningQuestionSupport;
import com.iread.backend.pronunciation.PronunciationAnalysisAdapter;
import com.iread.backend.pronunciation.DeterministicPronunciationAnalysisAdapter;
import com.iread.backend.pronunciation.PronunciationWordAligner;
import com.iread.backend.pronunciation.PronunciationAnalysisRequest;
import com.iread.backend.pronunciation.PronunciationAnalysisResult;
import com.iread.backend.pronunciation.PronunciationWordResult;
import com.iread.backend.training.app.dto.req.TrainingRecordingRequest;
import com.iread.backend.student.repository.StudentRepository;
import com.iread.backend.training.admin.service.TrainingService;
import com.iread.backend.training.app.dto.res.TrainingRecordingResponse;
import com.iread.backend.training.domain.TrainingDataEntity;
import com.iread.backend.training.domain.TrainingEntity;
import com.iread.backend.training.domain.TrainingStatus;
import com.iread.backend.training.domain.WordEntity;
import com.iread.backend.training.repository.TrainingDataRepository;
import com.iread.backend.training.repository.TrainingRepository;
import com.iread.backend.training.repository.WordRepository;
import com.iread.backend.training.input.TrainingInputType;
import com.iread.backend.training.input.TrainingInputRequirementService;
import com.iread.backend.wordattempt.domain.WordAttemptLogEntity;
import com.iread.backend.wordattempt.repository.WordAttemptLogRepository;
import com.iread.backend.wordattempt.config.WordAttemptScoreProperties;
import com.iread.backend.wordattempt.service.WordAttemptScoreCalculator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.util.unit.DataSize;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AppTrainingServiceTest {
    @Mock StudentRepository studentRepository;
    @Mock TrainingRepository trainingRepository;
    @Mock TrainingDataRepository trainingDataRepository;
    @Mock WordRepository wordRepository;
    @Mock WordAttemptLogRepository wordAttemptLogRepository;
    @Mock PronunciationAnalysisAdapter pronunciationAnalysisAdapter;
    @Mock AudioUploadPolicy audioUploadPolicy;
    @Mock WordAttemptScoreCalculator wordAttemptScoreCalculator;
    @Mock TrainingInputRequirementService trainingInputRequirementService;
    @Mock TrainingService trainingService;
    @Mock ObjectMapper objectMapper;
    @InjectMocks AppTrainingService appTrainingService;

    @Test
    void startsOwnedNotStartedTraining() {
        StudentEntity student = mock(StudentEntity.class);
        TrainingEntity training = mock(TrainingEntity.class);
        when(studentRepository.findByIdAndTeacherId(20L, 1L)).thenReturn(Optional.of(student));
        when(trainingRepository.findForUpdate(30L, 20L))
                .thenReturn(Optional.of(training));
        when(training.getStatus())
                .thenReturn(TrainingStatus.NOT_STARTED, TrainingStatus.IN_PROGRESS);

        var result = appTrainingService.start(1L, 20L, 30L);

        verify(training).start(any(LocalDateTime.class));
        assertThat(result.trainingId()).isEqualTo(30L);
        assertThat(result.status()).isEqualTo(TrainingStatus.IN_PROGRESS);
        assertThat(result.startedAt()).isNotNull();
    }

    @Test
    void evaluatesRawSelectionAndStoresFeedbackInTrainingResult() {
        StudentEntity student = mock(StudentEntity.class);
        TrainingEntity training = mock(TrainingEntity.class);
        when(studentRepository.findByIdAndTeacherId(20L, 1L)).thenReturn(Optional.of(student));
        when(trainingRepository.findForUpdate(30L, 20L))
                .thenReturn(Optional.of(training));
        when(training.getStatus()).thenReturn(TrainingStatus.IN_PROGRESS);
        when(training.getResult()).thenReturn(null);
        TrainingDataEntity data = mock(TrainingDataEntity.class);
        when(trainingDataRepository.findByTrainingId(30L)).thenReturn(Optional.of(data));
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
        AppTrainingService service = new AppTrainingService(
                studentRepository,
                trainingRepository,
                trainingDataRepository,
                wordRepository,
                wordAttemptLogRepository,
                pronunciationAnalysisAdapter,
                audioUploadPolicy,
                scoreCalculator(),
                new PronunciationWordAligner(),
                trainingInputRequirementService,
                trainingService,
                mapper,
                new AppLearningQuestionSupport(mapper)
        );
        UUID submissionId = UUID.randomUUID();
        var response = mapper.createObjectNode().put("selectedIndex", 0);

        var result = service.saveSelection(
                1L,
                20L,
                30L,
                1,
                new LearningSubmission(
                        submissionId,
                        LearningResponseType.SINGLE_CHOICE,
                        response
                )
        );

        assertThat(result.submissionId()).isEqualTo(submissionId);
        assertThat(result.correct()).isTrue();
        assertThat(result.attemptNo()).isEqualTo(1);
        assertThat(result.questionCompleted()).isTrue();
        ArgumentCaptor<String> resultCaptor = ArgumentCaptor.forClass(String.class);
        verify(training).recordProgressResult(resultCaptor.capture());
        assertThat(resultCaptor.getValue())
                .contains("\"submissionId\":\"" + submissionId + "\"")
                .contains("\"questionNo\":1")
                .contains("\"isCorrect\":true");
    }

    @Test
    void returnsStoredFeedbackForRetriedSubmissionIdWithoutAddingAttempt() {
        StudentEntity student = mock(StudentEntity.class);
        TrainingEntity training = mock(TrainingEntity.class);
        TrainingDataEntity data = mock(TrainingDataEntity.class);
        UUID submissionId = UUID.randomUUID();
        when(studentRepository.findByIdAndTeacherId(20L, 1L)).thenReturn(Optional.of(student));
        when(trainingRepository.findForUpdate(30L, 20L)).thenReturn(Optional.of(training));
        when(training.getStatus()).thenReturn(TrainingStatus.IN_PROGRESS);
        when(training.getResult()).thenReturn("""
                {
                  "submissions":[{
                    "submissionId":"%s",
                    "questionNo":1,
                    "responseType":"SINGLE_CHOICE",
                    "response":{"selectedIndex":1},
                    "feedback":{
                      "feedbackType":"TRAINING_FEEDBACK",
                      "submissionId":"%s",
                      "attemptNo":1,
                      "maxAttempts":3,
                      "remainingAttempts":2,
                      "correct":false,
                      "questionCompleted":false,
                      "canRetry":true,
                      "errorLocations":[],
                      "hint":"다시 살펴보세요.",
                      "correctResponse":null
                    }
                  }]
                }
                """.formatted(submissionId, submissionId));
        when(trainingDataRepository.findByTrainingId(30L)).thenReturn(Optional.of(data));
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
        AppTrainingService service = new AppTrainingService(
                studentRepository,
                trainingRepository,
                trainingDataRepository,
                wordRepository,
                wordAttemptLogRepository,
                pronunciationAnalysisAdapter,
                audioUploadPolicy,
                scoreCalculator(),
                new PronunciationWordAligner(),
                trainingInputRequirementService,
                trainingService,
                mapper,
                new AppLearningQuestionSupport(mapper)
        );

        var result = service.saveSelection(
                1L,
                20L,
                30L,
                1,
                new LearningSubmission(
                        submissionId,
                        LearningResponseType.SINGLE_CHOICE,
                        mapper.createObjectNode().put("selectedIndex", 1)
                )
        );

        assertThat(result.attemptNo()).isEqualTo(1);
        assertThat(result.canRetry()).isTrue();
        verify(training, never()).recordProgressResult(any(String.class));
    }

    @Test
    void revealsCorrectResponseAfterSecondIncorrectAttempt() {
        StudentEntity student = mock(StudentEntity.class);
        TrainingEntity training = mock(TrainingEntity.class);
        TrainingDataEntity data = mock(TrainingDataEntity.class);
        when(studentRepository.findByIdAndTeacherId(20L, 1L)).thenReturn(Optional.of(student));
        when(trainingRepository.findForUpdate(30L, 20L)).thenReturn(Optional.of(training));
        when(training.getStatus()).thenReturn(TrainingStatus.IN_PROGRESS);
        when(training.getResult()).thenReturn("""
                {
                  "submissions":[
                    {"submissionId":"00000000-0000-0000-0000-000000000001","questionNo":1}
                  ]
                }
                """);
        when(trainingDataRepository.findByTrainingId(30L)).thenReturn(Optional.of(data));
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
        AppTrainingService service = new AppTrainingService(
                studentRepository,
                trainingRepository,
                trainingDataRepository,
                wordRepository,
                wordAttemptLogRepository,
                pronunciationAnalysisAdapter,
                audioUploadPolicy,
                scoreCalculator(),
                new PronunciationWordAligner(),
                trainingInputRequirementService,
                trainingService,
                mapper,
                new AppLearningQuestionSupport(mapper)
        );

        var result = service.saveSelection(
                1L,
                20L,
                30L,
                1,
                new LearningSubmission(
                        UUID.randomUUID(),
                        LearningResponseType.SINGLE_CHOICE,
                        mapper.createObjectNode().put("selectedIndex", 1)
                )
        );

        assertThat(result.attemptNo()).isEqualTo(2);
        assertThat(result.questionCompleted()).isTrue();
        assertThat(result.canRetry()).isFalse();
        assertThat(result.correctResponse().path("response").path("selectedIndex").asInt())
                .isZero();
    }

    @Test
    void studentQuestionExposesAnswerButNotProfileOrInternalValidation() throws Exception {
        StudentEntity student = mock(StudentEntity.class);
        TrainingEntity training = mock(TrainingEntity.class);
        TrainingDataEntity data = mock(TrainingDataEntity.class);
        when(studentRepository.findByIdAndTeacherId(20L, 1L)).thenReturn(Optional.of(student));
        when(trainingRepository.findByIdAndDailyCurriculumStudentId(30L, 20L))
                .thenReturn(Optional.of(training));
        when(trainingDataRepository.findByTrainingId(30L)).thenReturn(Optional.of(data));
        when(data.getGeneratedData()).thenReturn("""
                {
                  "schemaVersion":2,
                  "profileSnapshot":{"features":[{"featureCode":"SECRET"}]},
                  "validationResult":{"passed":true},
                  "questions":[{
                    "questionNo":1,
                    "type":"SENTENCE_READING",
                    "requiredInputs":["VOICE","GAZE"],
                    "content":{"sentence":"아기는 사과를 먹는다."},
                    "answer":{"expectedText":"아기는 사과를 먹는다."},
                    "analysisTargets":[{"text":"아기는 사과를 먹는다."}],
                    "targetFeatureCodes":["PHONOLOGY.NASALIZATION"],
                    "text":"아기는 사과를 먹는다."
                  }]
                }
                """);
        ObjectMapper mapper = JsonMapper.builder().build();
        AppTrainingService service = new AppTrainingService(
                studentRepository,
                trainingRepository,
                trainingDataRepository,
                wordRepository,
                wordAttemptLogRepository,
                pronunciationAnalysisAdapter,
                audioUploadPolicy,
                wordAttemptScoreCalculator,
                new PronunciationWordAligner(),
                trainingInputRequirementService,
                trainingService,
                mapper,
                new AppLearningQuestionSupport(mapper)
        );

        var result = service.getQuestion(1L, 20L, 30L, 1);

        assertThat(result.question().path("questionType").asText())
                .isEqualTo("SENTENCE_READING");
        assertThat(result.question().path("responseType").asText())
                .isEqualTo("AUDIO");
        assertThat(result.question().path("content").path("sentence").asText())
                .isEqualTo("아기는 사과를 먹는다.");
        assertThat(result.question().path("requiredInputs"))
                .extracting(JsonNode::asText)
                .containsExactly("VOICE", "GAZE");
        assertThat(result.question().path("answer").path("expectedText").asText())
                .isEqualTo("아기는 사과를 먹는다.");
        assertThat(result.question().has("analysisTargets")).isFalse();
        assertThat(result.question().has("targetFeatureCodes")).isFalse();
    }

    @Test
    void recordingUsesTransientAudioAndLinksPronunciationDetailToTrainingResult() {
        StudentEntity student = mock(StudentEntity.class);
        TrainingEntity training = mock(TrainingEntity.class);
        TrainingDataEntity data = mock(TrainingDataEntity.class);
        WordEntity word = mock(WordEntity.class);
        WordAttemptLogEntity saved = mock(WordAttemptLogEntity.class);
        LocalDateTime createdAt = LocalDateTime.of(2026, 7, 28, 12, 0);
        when(studentRepository.findByIdAndTeacherId(20L, 1L)).thenReturn(Optional.of(student));
        when(trainingRepository.findForUpdate(30L, 20L))
                .thenReturn(Optional.of(training));
        when(training.getStatus()).thenReturn(TrainingStatus.IN_PROGRESS);
        when(training.getResult()).thenReturn(null);
        when(trainingDataRepository.findByTrainingId(30L)).thenReturn(Optional.of(data));
        when(data.getGeneratedData()).thenReturn("""
                {
                  "questions":[{
                    "analysisTargets":[{
                      "text":"먹는다"
                    }],
                    "words":[{
                      "wordIndex":0,
                      "surface":"먹는다"
                    }]
                  }]
                }
                """);
        when(wordRepository.findById(40L)).thenReturn(Optional.of(word));
        when(word.getId()).thenReturn(40L);
        when(word.getContent()).thenReturn("먹는다");
        when(wordAttemptLogRepository.saveAllAndFlush(any()))
                .thenReturn(List.of(saved));
        when(saved.getId()).thenReturn(50L);
        when(saved.getWord()).thenReturn(word);
        when(saved.getTotalScore()).thenReturn(542);
        when(saved.getCreatedAt()).thenReturn(createdAt);
        ObjectMapper mapper = JsonMapper.builder().build();
        AppTrainingService service = new AppTrainingService(
                studentRepository,
                trainingRepository,
                trainingDataRepository,
                wordRepository,
                wordAttemptLogRepository,
                new DeterministicPronunciationAnalysisAdapter(),
                new AudioUploadPolicy(
                        DataSize.ofMegabytes(20),
                        "audio/webm,audio/wav,audio/mpeg,audio/mp4"
                ),
                scoreCalculator(),
                new PronunciationWordAligner(),
                trainingInputRequirementService,
                trainingService,
                mapper,
                new AppLearningQuestionSupport(mapper)
        );
        TrainingRecordingRequest request = new TrainingRecordingRequest(
                40L,
                0,
                0,
                "먹는다",
                new MockMultipartFile(
                        "audioFile",
                        "nasalization-error.wav",
                        "audio/wav",
                        new byte[]{1, 2, 3}
                ),
                100,
                900
        );

        var result = service.saveRecording(1L, 20L, 30L, 1, request);

        verify(trainingInputRequirementService).requireQuestionInput(
                30L,
                1,
                com.iread.backend.training.input.TrainingInputType.VOICE
        );
        assertThat(result.words()).hasSize(1);
        assertThat(result.words().getFirst().attemptId()).isEqualTo(50L);
        assertThat(result.pronunciationAccuracyScore()).isEqualTo(54.2);
        assertThat(result.words().getFirst().pronunciationErrorType())
                .isEqualTo("Mispronunciation");
        ArgumentCaptor<String> resultCaptor = ArgumentCaptor.forClass(String.class);
        verify(training).recordProgressResult(resultCaptor.capture());
        assertThat(resultCaptor.getValue())
                .contains("\"questionNo\":1")
                .contains("\"tokenIndex\":0")
                .contains("\"wordAttemptLogId\":50")
                .contains("\"isFinal\":true")
                .contains("\"pronunciationAccuracyScore\":54.2");
    }

    @Test
    void recordingRejectsMismatchedAudioBeforePronunciationAnalysis() {
        StudentEntity student = mock(StudentEntity.class);
        TrainingEntity training = mock(TrainingEntity.class);
        TrainingDataEntity data = mock(TrainingDataEntity.class);
        WordEntity word = mock(WordEntity.class);
        when(studentRepository.findByIdAndTeacherId(20L, 1L)).thenReturn(Optional.of(student));
        when(trainingRepository.findForUpdate(30L, 20L)).thenReturn(Optional.of(training));
        when(training.getStatus()).thenReturn(TrainingStatus.IN_PROGRESS);
        when(trainingDataRepository.findByTrainingId(30L)).thenReturn(Optional.of(data));
        when(data.getGeneratedData()).thenReturn("""
                {
                  "questions":[{
                    "analysisTargets":[{
                      "text":"먹는다"
                    }]
                  }]
                }
                """);
        ObjectMapper mapper = JsonMapper.builder().build();
        AppTrainingService service = new AppTrainingService(
                studentRepository,
                trainingRepository,
                trainingDataRepository,
                wordRepository,
                wordAttemptLogRepository,
                pronunciationAnalysisAdapter,
                new AudioUploadPolicy(
                        DataSize.ofMegabytes(20),
                        "audio/webm,audio/wav,audio/mpeg,audio/mp4"
                ),
                scoreCalculator(),
                new PronunciationWordAligner(),
                trainingInputRequirementService,
                trainingService,
                mapper,
                new AppLearningQuestionSupport(mapper)
        );
        TrainingRecordingRequest request = new TrainingRecordingRequest(
                40L,
                0,
                null,
                "먹는다",
                new MockMultipartFile(
                        "audioFile",
                        "voice.wav",
                        "audio/mpeg",
                        new byte[]{1, 2, 3}
                ),
                100,
                900
        );

        assertThatThrownBy(() -> service.saveRecording(1L, 20L, 30L, 1, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("음성 파일 형식과 확장자가 일치하지 않습니다.");
        verifyNoInteractions(pronunciationAnalysisAdapter);
    }

    @Test
    void sentenceRecordingStoresOneAttemptForEachAnalyzedWord() {
        when(trainingInputRequirementService.inputsForQuestion(30L, 1))
                .thenReturn(Set.of(TrainingInputType.VOICE, TrainingInputType.GAZE));
        StudentEntity student = mock(StudentEntity.class);
        TrainingEntity training = mock(TrainingEntity.class);
        TrainingDataEntity data = mock(TrainingDataEntity.class);
        WordEntity first = word(101L, "아기는");
        WordEntity second = word(102L, "사과를");
        WordEntity third = word(103L, "먹는다");
        when(studentRepository.findByIdAndTeacherId(20L, 1L))
                .thenReturn(Optional.of(student));
        when(trainingRepository.findForUpdate(30L, 20L))
                .thenReturn(Optional.of(training));
        when(training.getId()).thenReturn(30L);
        when(training.getStatus()).thenReturn(TrainingStatus.IN_PROGRESS);
        when(training.getResult()).thenReturn(null);
        when(trainingDataRepository.findByTrainingId(30L)).thenReturn(Optional.of(data));
        when(data.getGeneratedData()).thenReturn("""
                {
                  "questions":[{
                    "questionNo":1,
                    "type":"SENTENCE_READING",
                    "requiredInputs":["VOICE","GAZE"],
                    "analysisTargets":[{"text":"아기는 사과를 먹는다."}],
                    "text":"아기는 사과를 먹는다.",
                    "words":[
                      {"wordIndex":0,"surface":"아기는"},
                      {"wordIndex":1,"surface":"사과를"},
                      {"wordIndex":2,"surface":"먹는다"}
                    ]
                  }]
                }
                """);
        when(wordRepository.findByContent("아기는")).thenReturn(Optional.of(first));
        when(wordRepository.findByContent("사과를")).thenReturn(Optional.of(second));
        when(wordRepository.findByContent("먹는다")).thenReturn(Optional.of(third));
        when(pronunciationAnalysisAdapter.analyze(any()))
                .thenReturn(new PronunciationAnalysisResult(
                        "sentence-request",
                        82.0,
                        79.0,
                        66.0,
                        78.0,
                        0.94,
                        "AZURE_SPEECH_V1",
                        List.of(
                                new PronunciationWordResult(
                                        0, "아기는", 91.0, "None", 100, 500
                                ),
                                new PronunciationWordResult(
                                        1, "정말", 75.0, "Insertion", 650, 250
                                ),
                                new PronunciationWordResult(
                                        2, "사과를", null, "Omission", 0, 0
                                ),
                                new PronunciationWordResult(
                                        3, "먹는다", 74.0, "None", 950, 600
                                )
                        )
                ));
        LocalDateTime createdAt = LocalDateTime.of(2026, 7, 29, 12, 0);
        when(wordAttemptLogRepository.saveAllAndFlush(any())).thenAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            List<WordAttemptLogEntity> attempts = invocation.getArgument(0);
            for (int index = 0; index < attempts.size(); index++) {
                ReflectionTestUtils.setField(attempts.get(index), "id", 501L + index);
                ReflectionTestUtils.setField(attempts.get(index), "createdAt", createdAt);
            }
            return attempts;
        });
        AppTrainingService service = new AppTrainingService(
                studentRepository,
                trainingRepository,
                trainingDataRepository,
                wordRepository,
                wordAttemptLogRepository,
                pronunciationAnalysisAdapter,
                new AudioUploadPolicy(
                        DataSize.ofMegabytes(20),
                        "audio/webm,audio/wav,audio/mpeg,audio/mp4"
                ),
                scoreCalculator(),
                new PronunciationWordAligner(),
                trainingInputRequirementService,
                trainingService,
                JsonMapper.builder().build(),
                new AppLearningQuestionSupport(JsonMapper.builder().build())
        );
        TrainingRecordingRequest request = new TrainingRecordingRequest(
                null,
                null,
                null,
                "아기는 사과를 먹는다.",
                new MockMultipartFile(
                        "audioFile",
                        "sentence.wav",
                        "audio/wav",
                        new byte[]{1, 2, 3}
                ),
                null,
                null
        );

        var response = service.saveRecording(1L, 20L, 30L, 1, request);

        assertThat(response.words()).hasSize(3);
        assertThat(response.words()).extracting(
                TrainingRecordingResponse.WordResult::tokenIndex
        ).containsExactly(0, 1, 2);
        assertThat(response.words()).extracting(
                TrainingRecordingResponse.WordResult::pronunciationAccuracyScore
        ).containsExactly(91.0, 0.0, 74.0);
        assertThat(response.words()).extracting(
                TrainingRecordingResponse.WordResult::totalScore
        ).containsExactly(null, null, null);
        assertThat(response.words().get(1).pronunciationErrorType())
                .isEqualTo("Omission");
        assertThat(response.words().get(2).speechStartOffsetMs()).isEqualTo(950);
        ArgumentCaptor<PronunciationAnalysisRequest> requestCaptor =
                ArgumentCaptor.forClass(PronunciationAnalysisRequest.class);
        verify(pronunciationAnalysisAdapter).analyze(requestCaptor.capture());
        assertThat(requestCaptor.getValue().expectedText())
                .isEqualTo("아기는 사과를 먹는다.");
        ArgumentCaptor<String> resultCaptor = ArgumentCaptor.forClass(String.class);
        verify(training).recordProgressResult(resultCaptor.capture());
        assertThat(resultCaptor.getValue())
                .contains("\"insertionCount\":1")
                .contains("\"wordAttemptLogId\":501")
                .contains("\"wordAttemptLogId\":503");
    }

    private WordEntity word(Long id, String content) {
        WordEntity word = new WordEntity(content);
        ReflectionTestUtils.setField(word, "id", id);
        return word;
    }

    private WordAttemptScoreCalculator scoreCalculator() {
        return new WordAttemptScoreCalculator(
                new WordAttemptScoreProperties(
                        100,
                        70,
                        200,
                        600,
                        100,
                        50,
                        30,
                        20
                )
        );
    }
}
