package com.iread.backend.training.app.service;

import com.iread.backend.exception.ResourceNotFoundException;
import com.iread.backend.student.repository.StudentRepository;
import com.iread.backend.training.app.dto.res.CurrentTrainingListResponse;
import com.iread.backend.training.domain.DailyCurriculumEntity;
import com.iread.backend.training.domain.DailyCurriculumStatus;
import com.iread.backend.training.repository.DailyCurriculumRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AppTrainingCatalogService {
    private static final List<DailyCurriculumStatus> ACTIVE_STATUSES = List.of(
            DailyCurriculumStatus.NOT_STARTED,
            DailyCurriculumStatus.IN_PROGRESS
    );

    private final StudentRepository studentRepository;
    private final DailyCurriculumRepository dailyCurriculumRepository;

    public CurrentTrainingListResponse getCurrentTrainingList(
            Long teacherId,
            Long studentId
    ) {
        requireOwnedStudent(teacherId, studentId);
        DailyCurriculumEntity curriculum = dailyCurriculumRepository
                .findFirstByStudentIdAndStatusInOrderByCreatedAtDesc(
                        studentId,
                        ACTIVE_STATUSES
                )
                .orElseThrow(() -> new ResourceNotFoundException(
                        "현재 진행 가능한 커리큘럼을 찾을 수 없습니다."
                ));
        return new CurrentTrainingListResponse(
                curriculum.getId(),
                curriculum.getStatus(),
                curriculum.getTrainings().stream()
                        .map(training -> new CurrentTrainingListResponse.TrainingItem(
                                training.getId(),
                                training.getTrainingTemplate().getId(),
                                training.getSequenceNo(),
                                training.getTrainingTemplate()
                                        .getCurriculumUnit()
                                        .getUnitName(),
                                training.getTrainingTemplate().getName(),
                                training.getStatus()
                        ))
                        .toList()
        );
    }

    private void requireOwnedStudent(Long teacherId, Long studentId) {
        studentRepository.findByIdAndTeacherId(studentId, teacherId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "학생을 찾을 수 없습니다."
                ));
    }
}
