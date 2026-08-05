package com.iread.backend.test.recommendation;

import com.iread.backend.test.domain.TestCurriculumEntity;
import com.iread.backend.test.repository.TestCurriculumRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class TestRecommendationStateService {
    private final TestCurriculumRepository testCurriculumRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean beginAttempt(Long testCurriculumId) {
        TestCurriculumEntity curriculum = findForUpdate(testCurriculumId);
        return curriculum.startRecommendation(LocalDateTime.now());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void complete(Long testCurriculumId) {
        findForUpdate(testCurriculumId).completeRecommendation();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void fail(Long testCurriculumId, Throwable failure) {
        String message = failure.getMessage();
        findForUpdate(testCurriculumId).failRecommendation(
                message == null || message.isBlank()
                        ? failure.getClass().getSimpleName()
                        : message
        );
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean prepareRetry(Long testCurriculumId) {
        return findForUpdate(testCurriculumId).requestRecommendationRetry();
    }

    private TestCurriculumEntity findForUpdate(Long testCurriculumId) {
        return testCurriculumRepository.findByIdForUpdate(testCurriculumId)
                .orElseThrow(() -> new IllegalStateException("실력도전 검사를 찾을 수 없습니다."));
    }
}
