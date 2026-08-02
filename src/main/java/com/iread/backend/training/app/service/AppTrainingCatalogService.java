package com.iread.backend.training.app.service;

import com.iread.backend.exception.ResourceNotFoundException;
import com.iread.backend.student.repository.StudentRepository;
import com.iread.backend.training.app.dto.res.CurrentTrainingListResponse;
import com.iread.backend.training.curriculum.ActiveCurriculumPolicy;
import com.iread.backend.training.domain.DailyCurriculumEntity;
import com.iread.backend.training.repository.DailyCurriculumRepository;
import com.iread.backend.training.generation.TrainingTemplateContract;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AppTrainingCatalogService {
    private final StudentRepository studentRepository;
    private final DailyCurriculumRepository dailyCurriculumRepository;
    private final ObjectMapper objectMapper;

    public CurrentTrainingListResponse getCurrentTrainingList(
            Long teacherId,
            Long studentId
    ) {
        requireOwnedStudent(teacherId, studentId);
        DailyCurriculumEntity curriculum = ActiveCurriculumPolicy
                .find(dailyCurriculumRepository, studentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "ACTIVE_CURRICULUM_NOT_FOUND",
                        "현재 진행 가능한 커리큘럼을 찾을 수 없습니다."
                ));
        return new CurrentTrainingListResponse(
                curriculum.getId(),
                curriculum.getStatus(),
                curriculum.getTrainings().stream()
                        .map(training -> new CurrentTrainingListResponse.TrainingItem(
                                training.getId(),
                                training.getTrainingTemplate().getId(),
                                TrainingTemplateContract.trainingType(
                                        training.getTrainingTemplate(),
                                        objectMapper
                                ),
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
