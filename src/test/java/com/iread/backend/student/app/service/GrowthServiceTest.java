package com.iread.backend.student.app.service;

import com.iread.backend.exception.ResourceNotFoundException;
import com.iread.backend.student.domain.StudentEntity;
import com.iread.backend.student.repository.StudentRepository;
import com.iread.backend.training.domain.TrainingEntity;
import com.iread.backend.training.domain.TrainingStatus;
import com.iread.backend.training.domain.TrainingTemplateEntity;
import com.iread.backend.training.repository.TrainingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GrowthServiceTest {
    @Mock StudentRepository studentRepository;
    @Mock TrainingRepository trainingRepository;
    GrowthService growthService;

    @BeforeEach
    void setUp() {
        growthService = new GrowthService(
                studentRepository,
                trainingRepository,
                defaultProperties()
        );
    }

    @Test
    void returnsCompletedCountByTrainingTemplate() {
        StudentEntity student = mock(StudentEntity.class);
        TrainingRepository.TrainingProgressProjection projection =
                mock(TrainingRepository.TrainingProgressProjection.class);
        when(studentRepository.findByIdAndTeacherId(20L, 1L))
                .thenReturn(Optional.of(student));
        when(projection.getTrainingTemplateId()).thenReturn(100L);
        when(projection.getTrainingTemplateName()).thenReturn("낱말 읽기");
        when(projection.getCompletedCount()).thenReturn(3L);
        when(trainingRepository.findCompletedTrainingProgress(20L))
                .thenReturn(List.of(projection));

        var result = growthService.getGrowth(1L, 20L);

        assertThat(result.trainingProgress()).singleElement().satisfies(progress -> {
            assertThat(progress.trainingTemplateId()).isEqualTo(100L);
            assertThat(progress.trainingTemplateName()).isEqualTo("낱말 읽기");
            assertThat(progress.completedCount()).isEqualTo(3L);
        });
    }

    @Test
    void combinesEffortDiversityAndMasteryWithoutLoweringReachedStage() {
        StudentEntity student = mock(StudentEntity.class);
        when(studentRepository.findByIdAndTeacherId(20L, 1L))
                .thenReturn(Optional.of(student));
        List<TrainingEntity> completed = new ArrayList<>();
        completed.add(training(1L, 80, 1));
        completed.add(training(2L, 80, 2));
        completed.add(training(1L, 80, 3));
        for (int index = 0; index < 25; index++) {
            completed.add(training(30L + index % 4, 85, 10 + index));
        }
        // 만개에 도달한 뒤 최근 정확도가 낮아져도 단계가 내려가지 않아야 한다.
        for (int index = 0; index < 10; index++) {
            completed.add(training(30L, 0, 40 + index));
        }
        when(trainingRepository.findAllByDailyCurriculumStudentIdAndStatus(
                20L,
                TrainingStatus.COMPLETED
        )).thenReturn(completed);

        var result = growthService.getGrowth(1L, 20L);

        assertThat(result.growthAreas()).hasSize(3);
        assertThat(result.growthAreas().get(0)).satisfies(area -> {
            assertThat(area.name()).isEqualTo("파닉스");
            assertThat(area.completedCount()).isEqualTo(3);
            assertThat(area.distinctTemplateCount()).isEqualTo(2);
            assertThat(area.stage()).isEqualTo(2);
            assertThat(area.stageName()).isEqualTo("새싹");
            // 꽃봉오리 승급 병목: 필요 커버리지 25%(21개 중 6개) 대비 경험 2개 = 33%
            assertThat(area.nextStageProgressPercent()).isEqualTo(33);
            assertThat(area.nextStageHint()).isEqualTo("새로운 활동을 4개 더 해봐요!");
        });
        assertThat(result.growthAreas().get(1)).satisfies(area -> {
            assertThat(area.stage()).isEqualTo(1);
            assertThat(area.nextStageProgressPercent()).isEqualTo(0);
            assertThat(area.nextStageHint()).isEqualTo("훈련을 3번 더 하면 자라나요!");
        });
        assertThat(result.growthAreas().get(2)).satisfies(area -> {
            assertThat(area.name()).isEqualTo("유창성");
            assertThat(area.stage()).isEqualTo(5);
            assertThat(area.stageName()).isEqualTo("만개");
            assertThat(area.masteredTemplateCount()).isEqualTo(4);
            assertThat(area.recentAverageAccuracy()).isEqualByComparingTo("0.00");
            assertThat(area.nextStageProgressPercent()).isEqualTo(100);
            assertThat(area.nextStageHint()).isNull();
        });
    }

    @Test
    void rejectsStudentOutsideTeacherOwnership() {
        when(studentRepository.findByIdAndTeacherId(20L, 1L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> growthService.getGrowth(1L, 20L))
                .isInstanceOf(ResourceNotFoundException.class);
        verifyNoInteractions(trainingRepository);
    }

    private TrainingEntity training(long templateId, int accuracy, int dayOffset) {
        TrainingEntity training = mock(TrainingEntity.class);
        TrainingTemplateEntity template = mock(TrainingTemplateEntity.class);
        when(template.getId()).thenReturn(templateId);
        when(training.getTrainingTemplate()).thenReturn(template);
        when(training.getAccuracy()).thenReturn(BigDecimal.valueOf(accuracy));
        when(training.getFinishedAt()).thenReturn(
                LocalDateTime.of(2026, 7, 1, 9, 0).plusDays(dayOffset)
        );
        return training;
    }

    private GrowthStageProperties defaultProperties() {
        return new GrowthStageProperties(
                10,
                70,
                3,
                2,
                8,
                25,
                15,
                50,
                70,
                25,
                70,
                80
        );
    }
}
