package com.iread.backend.training.domain;

import com.iread.backend.student.domain.StudentEntity;
import com.iread.backend.teacher.domain.TeacherEntity;
import com.iread.backend.test.domain.TestCurriculumEntity;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class DailyCurriculumReviewTest {

    @Test
    void recommendedCurriculumRequiresReviewAfterAllContentIsGenerated() {
        DailyCurriculumEntity curriculum = recommendedCurriculum();

        assertThat(curriculum.getReviewStatus())
                .isEqualTo(CurriculumReviewStatus.GENERATION_PENDING);
        assertThat(curriculum.isAvailableToStudent()).isFalse();

        curriculum.getTrainings().forEach(TrainingEntity::markReady);
        curriculum.refreshReviewRequirement();

        assertThat(curriculum.getReviewStatus())
                .isEqualTo(CurriculumReviewStatus.REVIEW_REQUIRED);
    }

    @Test
    void contentChangeInvalidatesCompletedReviewAndCompositionChangeRequiresRegeneration() {
        DailyCurriculumEntity curriculum = recommendedCurriculum();
        TeacherEntity teacher = mock(TeacherEntity.class);
        LocalDateTime reviewedAt = LocalDateTime.of(2026, 8, 1, 15, 0);
        curriculum.getTrainings().forEach(TrainingEntity::markReady);
        curriculum.refreshReviewRequirement();
        curriculum.completeReview(teacher, reviewedAt);

        curriculum.markContentChanged();

        assertThat(curriculum.getReviewStatus())
                .isEqualTo(CurriculumReviewStatus.REVIEW_REQUIRED);
        assertThat(curriculum.getReviewedByTeacher()).isNull();
        assertThat(curriculum.getReviewedAt()).isNull();

        curriculum.replaceTrainings(templates());

        assertThat(curriculum.getReviewStatus())
                .isEqualTo(CurriculumReviewStatus.REGENERATION_REQUIRED);
        assertThat(curriculum.getTrainings())
                .allMatch(training -> training.getStatus() == TrainingStatus.NOT_READY);
    }

    @Test
    void normalCurriculumRemainsAvailableWithoutReview() {
        DailyCurriculumEntity curriculum = new DailyCurriculumEntity(
                mock(StudentEntity.class),
                templates()
        );

        assertThat(curriculum.getReviewStatus())
                .isEqualTo(CurriculumReviewStatus.NOT_REQUIRED);
        assertThat(curriculum.isAvailableToStudent()).isTrue();
    }

    @Test
    void legacyStartedRecommendedCurriculumRemainsAvailable() {
        DailyCurriculumEntity curriculum = recommendedCurriculum();
        TrainingEntity training = curriculum.getTrainings().getFirst();
        training.markReady();

        training.start(LocalDateTime.of(2026, 8, 1, 9, 0));

        assertThat(curriculum.getStatus()).isEqualTo(DailyCurriculumStatus.IN_PROGRESS);
        assertThat(curriculum.getReviewStatus())
                .isEqualTo(CurriculumReviewStatus.GENERATION_PENDING);
        assertThat(curriculum.isAvailableToStudent()).isTrue();
    }

    private DailyCurriculumEntity recommendedCurriculum() {
        return new DailyCurriculumEntity(
                mock(StudentEntity.class),
                templates(),
                mock(TestCurriculumEntity.class)
        );
    }

    private List<TrainingTemplateEntity> templates() {
        return IntStream.range(0, 5)
                .mapToObj(index -> mock(TrainingTemplateEntity.class))
                .toList();
    }
}
