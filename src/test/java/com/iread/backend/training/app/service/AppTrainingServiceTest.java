package com.iread.backend.training.app.service;

import com.iread.backend.student.domain.StudentEntity;
import com.iread.backend.global.audio.AudioUploadPolicy;
import com.iread.backend.pronunciation.PronunciationAnalysisAdapter;
import com.iread.backend.pronunciation.DeterministicPronunciationAnalysisAdapter;
import com.iread.backend.training.app.dto.req.TrainingRecordingRequest;
import com.iread.backend.student.repository.StudentRepository;
import com.iread.backend.training.admin.service.TrainingService;
import com.iread.backend.training.app.dto.req.TrainingSelectionRequest;
import com.iread.backend.training.domain.TrainingDataEntity;
import com.iread.backend.training.domain.TrainingEntity;
import com.iread.backend.training.domain.TrainingStatus;
import com.iread.backend.training.domain.WordEntity;
import com.iread.backend.training.repository.TrainingDataRepository;
import com.iread.backend.training.repository.TrainingRepository;
import com.iread.backend.training.repository.WordRepository;
import com.iread.backend.wordattempt.domain.WordAttemptLogEntity;
import com.iread.backend.wordattempt.repository.WordAttemptLogRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.util.unit.DataSize;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
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
    void storesSelectionAsTrainingWordAttempt() {
        StudentEntity student = mock(StudentEntity.class);
        TrainingEntity training = mock(TrainingEntity.class);
        WordEntity word = mock(WordEntity.class);
        WordAttemptLogEntity saved = mock(WordAttemptLogEntity.class);
        LocalDateTime createdAt = LocalDateTime.of(2026, 7, 27, 12, 0);
        when(studentRepository.findByIdAndTeacherId(20L, 1L)).thenReturn(Optional.of(student));
        when(trainingRepository.findForUpdate(30L, 20L))
                .thenReturn(Optional.of(training));
        when(training.getStatus()).thenReturn(TrainingStatus.IN_PROGRESS);
        when(wordRepository.findById(40L)).thenReturn(Optional.of(word));
        when(word.getId()).thenReturn(40L);
        when(word.getContent()).thenReturn("사과");
        when(saved.getId()).thenReturn(50L);
        when(saved.getCorrect()).thenReturn(true);
        when(saved.getTotalScore()).thenReturn(900);
        when(saved.getCreatedAt()).thenReturn(createdAt);
        when(wordAttemptLogRepository.saveAndFlush(any(WordAttemptLogEntity.class)))
                .thenReturn(saved);

        var result = appTrainingService.saveSelection(
                1L,
                20L,
                30L,
                new TrainingSelectionRequest(40L, true, 900)
        );

        assertThat(result.attemptId()).isEqualTo(50L);
        assertThat(result.trainingId()).isEqualTo(30L);
        assertThat(result.wordId()).isEqualTo(40L);
        assertThat(result.isCorrect()).isTrue();
        assertThat(result.totalScore()).isEqualTo(900);
        assertThat(result.createdAt()).isEqualTo(createdAt);
    }

    @Test
    void studentQuestionDoesNotExposeAnswerProfileOrInternalValidation() throws Exception {
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
                    "content":{"sentence":"아기는 사과를 먹는다."},
                    "answer":{"expectedText":"아기는 사과를 먹는다."},
                    "analysisTargets":[{"text":"아기는 사과를 먹는다."}],
                    "targetFeatureCodes":["PHONOLOGY.NASALIZATION"],
                    "text":"아기는 사과를 먹는다.",
                    "expectedPronunciation":"아기는 사과를 멍는다."
                  }]
                }
                """);
        AppTrainingService service = new AppTrainingService(
                studentRepository,
                trainingRepository,
                trainingDataRepository,
                wordRepository,
                wordAttemptLogRepository,
                pronunciationAnalysisAdapter,
                audioUploadPolicy,
                trainingService,
                JsonMapper.builder().build()
        );

        var result = service.getQuestion(1L, 20L, 30L, 1);

        assertThat(result.question().path("content").path("sentence").asText())
                .isEqualTo("아기는 사과를 먹는다.");
        assertThat(result.question().has("answer")).isFalse();
        assertThat(result.question().has("analysisTargets")).isFalse();
        assertThat(result.question().has("targetFeatureCodes")).isFalse();
        assertThat(result.question().has("expectedPronunciation")).isFalse();
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
                      "text":"먹는다",
                      "expectedPronunciation":"멍는다"
                    }],
                    "words":[{
                      "wordIndex":0,
                      "surface":"먹는다",
                      "expectedPronunciation":"멍는다"
                    }]
                  }]
                }
                """);
        when(wordRepository.findById(40L)).thenReturn(Optional.of(word));
        when(word.getId()).thenReturn(40L);
        when(word.getContent()).thenReturn("먹는다");
        when(wordAttemptLogRepository.saveAndFlush(any(WordAttemptLogEntity.class)))
                .thenReturn(saved);
        when(saved.getId()).thenReturn(50L);
        when(saved.getTotalScore()).thenReturn(542);
        when(saved.getCreatedAt()).thenReturn(createdAt);
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
                trainingService,
                JsonMapper.builder().build()
        );
        TrainingRecordingRequest request = new TrainingRecordingRequest(
                40L,
                0,
                0,
                "먹는다",
                "멍는다",
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

        assertThat(result.attemptId()).isEqualTo(50L);
        assertThat(result.observedPronunciation()).isEqualTo("먹는다");
        assertThat(result.pronunciationScore()).isEqualTo(54.2);
        assertThat(result.pronunciationErrorType()).isEqualTo("PHONOLOGICAL_RULE_NOT_APPLIED");
        ArgumentCaptor<String> resultCaptor = ArgumentCaptor.forClass(String.class);
        verify(training).recordProgressResult(resultCaptor.capture());
        assertThat(resultCaptor.getValue())
                .contains("\"questionNo\":1")
                .contains("\"tokenIndex\":0")
                .contains("\"wordAttemptLogId\":50")
                .contains("\"isFinal\":true")
                .contains("\"observedPronunciation\":\"먹는다\"");
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
                      "text":"먹는다",
                      "expectedPronunciation":"멍는다"
                    }]
                  }]
                }
                """);
        when(wordRepository.findById(40L)).thenReturn(Optional.of(word));
        when(word.getContent()).thenReturn("먹는다");
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
                trainingService,
                JsonMapper.builder().build()
        );
        TrainingRecordingRequest request = new TrainingRecordingRequest(
                40L,
                0,
                null,
                "먹는다",
                "멍는다",
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
}
