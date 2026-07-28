package com.iread.backend.training.app.service;

import com.iread.backend.student.domain.StudentEntity;
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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AppTrainingServiceTest {
    @Mock StudentRepository studentRepository;
    @Mock TrainingRepository trainingRepository;
    @Mock TrainingDataRepository trainingDataRepository;
    @Mock WordRepository wordRepository;
    @Mock WordAttemptLogRepository wordAttemptLogRepository;
    @Mock TrainingService trainingService;
    @Mock ObjectMapper objectMapper;
    @InjectMocks AppTrainingService appTrainingService;

    @Test
    void startsOwnedNotStartedTraining() {
        StudentEntity student = mock(StudentEntity.class);
        TrainingEntity training = mock(TrainingEntity.class);
        when(studentRepository.findByIdAndTeacherId(20L, 1L)).thenReturn(Optional.of(student));
        when(trainingRepository.findByIdAndDailyCurriculumStudentId(30L, 20L))
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
        when(trainingRepository.findByIdAndDailyCurriculumStudentId(30L, 20L))
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
}
