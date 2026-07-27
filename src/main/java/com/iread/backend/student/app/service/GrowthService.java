package com.iread.backend.student.app.service;

import com.iread.backend.exception.ResourceNotFoundException;
import com.iread.backend.student.app.dto.res.GrowthResponse;
import com.iread.backend.student.app.dto.res.TrainingProgressResponse;
import com.iread.backend.student.repository.StudentRepository;
import com.iread.backend.training.repository.TrainingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GrowthService {
    private final StudentRepository studentRepository;
    private final TrainingRepository trainingRepository;

    public GrowthResponse getGrowth(Long teacherId, Long studentId) {
        studentRepository.findByIdAndTeacherId(studentId, teacherId)
                .orElseThrow(() -> new ResourceNotFoundException("학생을 찾을 수 없습니다."));

        return new GrowthResponse(
                trainingRepository.findCompletedTrainingProgress(studentId).stream()
                        .map(progress -> new TrainingProgressResponse(
                                progress.getTrainingTemplateId(),
                                progress.getTrainingTemplateName(),
                                progress.getCompletedCount()
                        ))
                        .toList()
        );
    }
}
