package com.iread.backend.training.curriculum;

import com.iread.backend.training.domain.DailyCurriculumEntity;
import com.iread.backend.training.domain.DailyCurriculumStatus;
import com.iread.backend.training.repository.DailyCurriculumRepository;

import java.util.Optional;

public final class ActiveCurriculumPolicy {

    private ActiveCurriculumPolicy() {
    }

    public static Optional<DailyCurriculumEntity> find(
            DailyCurriculumRepository repository,
            Long studentId
    ) {
        return repository
                .findByStudentIdAndStatus(studentId, DailyCurriculumStatus.IN_PROGRESS)
                .filter(ActiveCurriculumPolicy::isAvailableToStudent)
                .or(() -> repository.findByStudentIdAndStatus(
                        studentId,
                        DailyCurriculumStatus.NOT_STARTED
                ).filter(ActiveCurriculumPolicy::isAvailableToStudent));
    }

    private static boolean isAvailableToStudent(DailyCurriculumEntity curriculum) {
        return !curriculum.isRecommendedFromTest() || curriculum.isAvailableToStudent();
    }
}
