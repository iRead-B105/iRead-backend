package com.iread.backend.test.recommendation;

import com.iread.backend.readingfeature.service.StudentFeatureProfileService;
import com.iread.backend.student.domain.StudentEntity;
import com.iread.backend.student.repository.StudentRepository;
import com.iread.backend.test.domain.TestCurriculumEntity;
import com.iread.backend.test.repository.TestCurriculumRepository;
import com.iread.backend.training.curriculum.PersonalizedCurriculumPlanner;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class TestRecommendationCoordinator {
    private final TestRecommendationStateService stateService;
    private final TestCurriculumRepository testCurriculumRepository;
    private final StudentRepository studentRepository;
    private final StudentFeatureProfileService profileService;
    private final PersonalizedCurriculumPlanner curriculumPlanner;

    public void process(Long testCurriculumId) {
        try {
            if (!stateService.beginAttempt(testCurriculumId)) {
                return;
            }
        } catch (RuntimeException failure) {
            log.warn(
                    "실력도전 추천 후속 처리를 시작하지 못했습니다. testCurriculumId={}",
                    testCurriculumId,
                    failure
            );
            return;
        }
        try {
            TestCurriculumEntity source = testCurriculumRepository.findById(testCurriculumId)
                    .orElseThrow(() -> new IllegalStateException(
                            "실력도전 검사를 찾을 수 없습니다."
                    ));
            StudentEntity student = studentRepository.findById(source.getStudent().getId())
                    .orElseThrow(() -> new IllegalStateException("학생을 찾을 수 없습니다."));
            profileService.recalculate(student);
            curriculumPlanner.createRecommendedFromTestIfAbsent(student, testCurriculumId);
            stateService.complete(testCurriculumId);
        } catch (RuntimeException failure) {
            recordFailure(testCurriculumId, failure);
            log.warn(
                    "실력도전 추천 후속 처리에 실패했습니다. testCurriculumId={}",
                    testCurriculumId,
                    failure
            );
        }
    }

    public boolean retry(Long testCurriculumId) {
        if (!stateService.prepareRetry(testCurriculumId)) {
            return false;
        }
        process(testCurriculumId);
        return true;
    }

    private void recordFailure(Long testCurriculumId, RuntimeException failure) {
        try {
            stateService.fail(testCurriculumId, failure);
        } catch (RuntimeException stateFailure) {
            log.error(
                    "실력도전 추천 실패 상태를 저장하지 못했습니다. testCurriculumId={}",
                    testCurriculumId,
                    stateFailure
            );
        }
    }
}
