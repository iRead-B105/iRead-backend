package com.iread.backend.readingfeature.service;

import com.iread.backend.readingfeature.domain.ReadingFeatureCategory;
import com.iread.backend.readingfeature.domain.ReadingFeatureEntity;
import com.iread.backend.readingfeature.domain.ReadingFeatureScope;
import com.iread.backend.readingfeature.domain.StudentFeatureProfileEntity;
import com.iread.backend.readingfeature.repository.ReadingFeatureRepository;
import com.iread.backend.readingfeature.repository.StudentFeatureProfileRepository;
import com.iread.backend.story.analysis.StoryLineContentService;
import com.iread.backend.student.domain.StudentEntity;
import com.iread.backend.training.analysis.KoreanG2pEngine;
import com.iread.backend.training.analysis.KoreanTextAnalyzer;
import com.iread.backend.training.domain.TrainingDataEntity;
import com.iread.backend.training.domain.TrainingEntity;
import com.iread.backend.training.domain.TrainingStatus;
import com.iread.backend.training.domain.WordEntity;
import com.iread.backend.training.repository.TrainingDataRepository;
import com.iread.backend.training.repository.TrainingRepository;
import com.iread.backend.wordattempt.domain.WordAttemptLogEntity;
import com.iread.backend.wordattempt.repository.WordAttemptLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StudentFeatureProfileServiceTest {

    @Mock TrainingRepository trainingRepository;
    @Mock TrainingDataRepository trainingDataRepository;
    @Mock WordAttemptLogRepository wordAttemptLogRepository;
    @Mock ReadingFeatureRepository readingFeatureRepository;
    @Mock StudentFeatureProfileRepository profileRepository;

    private StudentFeatureProfileService service;

    @BeforeEach
    void setUp() {
        JsonMapper objectMapper = JsonMapper.builder().build();
        service = new StudentFeatureProfileService(
                trainingRepository,
                trainingDataRepository,
                wordAttemptLogRepository,
                readingFeatureRepository,
                profileRepository,
                new StoryLineContentService(new KoreanTextAnalyzer(
                        new KoreanG2pEngine()
                ), objectMapper),
                objectMapper
        );
    }

    @Test
    void finalAttemptIsMappedToFeatureAndWeaknessUsesConfidenceWeightedPronunciation() {
        StudentEntity student = mock(StudentEntity.class);
        when(student.getId()).thenReturn(15L);
        TrainingEntity training = mock(TrainingEntity.class);
        when(training.getId()).thenReturn(120L);
        when(training.getResult()).thenReturn("""
                {
                  "wordAttempts": [
                    {
                      "wordAttemptLogId": 99,
                      "questionNo": 1,
                      "targetIndex": 0,
                      "isFinal": true,
                      "pronunciationAccuracyScore": 50.0,
                      "pronunciationConfidence": 0.5,
                      "pronunciationErrorType": "NASALIZATION_NOT_APPLIED",
                      "wordReadTimeMs": 2500
                    },
                    {
                      "wordAttemptLogId": 98,
                      "questionNo": 1,
                      "targetIndex": 0,
                      "isFinal": false,
                      "pronunciationAccuracyScore": 0.0,
                      "pronunciationConfidence": 1.0
                    }
                  ]
                }
                """);
        TrainingDataEntity trainingData = new TrainingDataEntity(training, """
                {
                  "questions": [{
                    "questionNo": 1,
                    "analysisTargets": [{
                      "featureCodes": ["PHONOLOGY.NASALIZATION.ㄱ_BEFORE_ㄴ"]
                    }]
                  }]
                }
                """);

        WordEntity word = mock(WordEntity.class);
        WordAttemptLogEntity log = new WordAttemptLogEntity(
                student, word, training, "먹는다",
                true, true, 1200, 3, 100, 2600,
                false, 2, 500, 100, 2600,
                false, 500, 1, 0, null, true
        );
        ReflectionTestUtils.setField(log, "id", 99L);
        ReflectionTestUtils.setField(log, "createdAt", LocalDateTime.of(2026, 7, 28, 14, 0));

        ReadingFeatureEntity feature = new ReadingFeatureEntity(
                10L, null, "PHONOLOGY.NASALIZATION.ㄱ_BEFORE_ㄴ", "비음화",
                ReadingFeatureCategory.PHONOLOGY, ReadingFeatureScope.WORD
        );

        when(trainingRepository.findAllByDailyCurriculumStudentIdAndStatus(
                15L, TrainingStatus.COMPLETED
        )).thenReturn(List.of(training));
        when(trainingDataRepository.findByTrainingId(120L)).thenReturn(Optional.of(trainingData));
        when(wordAttemptLogRepository.findAllById(List.of(99L))).thenReturn(List.of(log));
        when(readingFeatureRepository.findAllByFeatureCodeIn(any()))
                .thenReturn(List.of(feature));
        when(profileRepository.findMaxId()).thenReturn(40L);
        when(profileRepository.findByStudentIdAndReadingFeatureId(15L, 10L))
                .thenReturn(Optional.empty());

        var result = service.recalculate(student);

        assertThat(result).singleElement().satisfies(profile -> {
            assertThat(profile.featureCode())
                    .isEqualTo("PHONOLOGY.NASALIZATION.ㄱ_BEFORE_ㄴ");
            assertThat(profile.accuracyRate()).isZero();
            assertThat(profile.avgPronunciationScore()).isEqualTo(50.0);
            assertThat(profile.weaknessScore()).isEqualTo(0.775);
            assertThat(profile.confidence()).isEqualTo(0.05);
            assertThat(profile.status())
                    .isEqualTo(StudentFeatureProfileService.ProfileStatus.WEAK);
            assertThat(profile.analysisVersion()).isEqualTo("WEAKNESS_V1");
        });

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<StudentFeatureProfileEntity>> captor =
                ArgumentCaptor.forClass(List.class);
        verify(profileRepository).saveAll(captor.capture());
        StudentFeatureProfileEntity saved = captor.getValue().getFirst();
        assertThat(saved.getId()).isEqualTo(41L);
        assertThat(saved.getWeaknessScore()).isEqualTo(775);
        assertThat(saved.getAvgPronunciationScore()).isEqualTo(500);
        assertThat(saved.getEvidenceCount()).isEqualTo(1);
    }

    @Test
    void voiceOnlyAttemptIgnoresMissingGazeMetricsDuringProfileRecalculation() {
        StudentEntity student = mock(StudentEntity.class);
        when(student.getId()).thenReturn(15L);
        TrainingEntity training = mock(TrainingEntity.class);
        when(training.getId()).thenReturn(120L);
        when(training.getResult()).thenReturn("""
                {
                  "wordAttempts":[{
                    "wordAttemptLogId":99,
                    "questionNo":1,
                    "targetIndex":0,
                    "isFinal":true,
                    "pronunciationAccuracyScore":95.0,
                    "pronunciationConfidence":0.96,
                    "wordReadTimeMs":400
                  }]
                }
                """);
        TrainingDataEntity trainingData = new TrainingDataEntity(training, """
                {
                  "questions":[{
                    "questionNo":1,
                    "analysisTargets":[{
                      "featureCodes":["VOICE_ONLY"]
                    }]
                  }]
                }
                """);
        WordEntity word = mock(WordEntity.class);
        WordAttemptLogEntity log = new WordAttemptLogEntity(
                student, word, training, "가",
                false, true, null, null, null, null,
                false, 0, 950, 0, 400,
                true, 950, 1, 0, null, true
        );
        ReflectionTestUtils.setField(log, "id", 99L);
        ReflectionTestUtils.setField(log, "createdAt", LocalDateTime.of(2026, 7, 30, 12, 0));
        ReadingFeatureEntity feature = new ReadingFeatureEntity(
                10L, null, "VOICE_ONLY", "음성 전용",
                ReadingFeatureCategory.PHONOLOGY, ReadingFeatureScope.WORD
        );

        when(trainingRepository.findAllByDailyCurriculumStudentIdAndStatus(
                15L, TrainingStatus.COMPLETED
        )).thenReturn(List.of(training));
        when(trainingDataRepository.findByTrainingId(120L)).thenReturn(Optional.of(trainingData));
        when(wordAttemptLogRepository.findAllById(List.of(99L))).thenReturn(List.of(log));
        when(readingFeatureRepository.findAllByFeatureCodeIn(any()))
                .thenReturn(List.of(feature));
        when(profileRepository.findMaxId()).thenReturn(40L);
        when(profileRepository.findByStudentIdAndReadingFeatureId(15L, 10L))
                .thenReturn(Optional.empty());

        var result = service.recalculate(student);

        assertThat(result).singleElement().satisfies(profile -> {
            assertThat(profile.avgFixationDurationMs()).isNull();
            assertThat(profile.avgFixationCount()).isNull();
            assertThat(profile.avgRegressionCount()).isZero();
            assertThat(profile.avgPronunciationScore()).isEqualTo(95.0);
        });
    }

    @Test
    void nonAudioQuestionResultContributesFeatureAccuracyEvidence() {
        StudentEntity student = mock(StudentEntity.class);
        when(student.getId()).thenReturn(15L);
        TrainingEntity training = mock(TrainingEntity.class);
        when(training.getId()).thenReturn(120L);
        when(training.getFinishedAt())
                .thenReturn(LocalDateTime.of(2026, 7, 28, 15, 0));
        when(training.getResult()).thenReturn("""
                {
                  "questions":[{
                    "questionNo":1,
                    "isCorrect":false,
                    "totalScore":0
                  }]
                }
                """);
        TrainingDataEntity trainingData = new TrainingDataEntity(training, """
                {
                  "questions":[{
                    "questionNo":1,
                    "type":"CONSONANT_SOUND_CHOICE",
                    "targetFeatureCodes":["GRAPHEME.CONSONANT.ㄱ"]
                  }]
                }
                """);
        ReadingFeatureEntity feature = new ReadingFeatureEntity(
                10L,
                null,
                "GRAPHEME.CONSONANT.ㄱ",
                "기역",
                ReadingFeatureCategory.GRAPHEME,
                ReadingFeatureScope.CHARACTER
        );
        when(trainingRepository.findAllByDailyCurriculumStudentIdAndStatus(
                15L, TrainingStatus.COMPLETED
        )).thenReturn(List.of(training));
        when(trainingDataRepository.findByTrainingId(120L)).thenReturn(Optional.of(trainingData));
        when(readingFeatureRepository.findAllByFeatureCodeIn(any()))
                .thenReturn(List.of(feature));
        when(profileRepository.findMaxId()).thenReturn(40L);
        when(profileRepository.findByStudentIdAndReadingFeatureId(15L, 10L))
                .thenReturn(Optional.empty());

        var result = service.recalculate(student);

        assertThat(result).singleElement().satisfies(profile -> {
            assertThat(profile.featureCode()).isEqualTo("GRAPHEME.CONSONANT.ㄱ");
            assertThat(profile.accuracyRate()).isZero();
            assertThat(profile.weaknessScore()).isEqualTo(0.4);
            assertThat(profile.evidenceCount()).isEqualTo(1);
        });
    }
}
