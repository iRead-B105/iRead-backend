package com.iread.backend.training.completion;

import com.iread.backend.readingfeature.service.StudentFeatureProfileService;
import com.iread.backend.student.domain.StudentEntity;
import com.iread.backend.student.repository.StudentRepository;
import com.iread.backend.training.curriculum.PersonalizedCurriculumPlanner;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TrainingCompletionFollowUpWorker {
    private final StudentRepository studentRepository;
    private final StudentFeatureProfileService profileService;
    private final PersonalizedCurriculumPlanner curriculumPlanner;

    @Transactional
    public void process(Long studentId, boolean createNextCurriculum) {
        StudentEntity student = studentRepository.findById(studentId)
                .orElseThrow(() -> new IllegalStateException("Student was not found."));
        profileService.recalculate(student);
        if (createNextCurriculum) {
            curriculumPlanner.createNextIfAbsent(student);
        }
    }
}
