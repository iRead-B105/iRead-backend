package com.iread.backend.test.recommendation;

import com.iread.backend.readingfeature.service.StudentFeatureProfileService;
import com.iread.backend.student.domain.StudentEntity;
import com.iread.backend.student.repository.StudentRepository;
import com.iread.backend.test.domain.TestCurriculumEntity;
import com.iread.backend.test.repository.TestCurriculumRepository;
import com.iread.backend.training.curriculum.PersonalizedCurriculumPlanner;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TestRecommendationWorkService {

    private final TestCurriculumRepository testCurriculumRepository;
    private final StudentRepository studentRepository;
    private final StudentFeatureProfileService profileService;
    private final PersonalizedCurriculumPlanner curriculumPlanner;
    private final com.iread.backend.training.curriculum.CurriculumGenerationAfterCommitTrigger
            curriculumGenerationTrigger;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void process(Long testCurriculumId) {
        TestCurriculumEntity source = testCurriculumRepository.findById(testCurriculumId)
                .orElseThrow(() -> new IllegalStateException(
                        "실력도전 검사를 찾을 수 없습니다."
                ));
        StudentEntity student = studentRepository.findById(source.getStudent().getId())
                .orElseThrow(() -> new IllegalStateException("학생을 찾을 수 없습니다."));
        profileService.recalculate(student);
        var curriculum = curriculumPlanner.createRecommendedFromTestIfAbsent(student, testCurriculumId);
        // 새벽 배치를 기다리지 않고 커밋 직후 교안을 채워, 교수자 검토가 바로 가능하게 한다.
        curriculumGenerationTrigger.generateAfterCommit(curriculum.getId());
    }
}
