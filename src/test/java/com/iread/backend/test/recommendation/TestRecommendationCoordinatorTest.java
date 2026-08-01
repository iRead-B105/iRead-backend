package com.iread.backend.test.recommendation;

import com.iread.backend.readingfeature.service.StudentFeatureProfileService;
import com.iread.backend.student.domain.StudentEntity;
import com.iread.backend.student.repository.StudentRepository;
import com.iread.backend.test.domain.TestCurriculumEntity;
import com.iread.backend.test.repository.TestCurriculumRepository;
import com.iread.backend.training.curriculum.PersonalizedCurriculumPlanner;
import com.iread.backend.training.domain.DailyCurriculumEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TestRecommendationCoordinatorTest {
    private TestRecommendationStateService stateService;
    private TestCurriculumRepository testCurriculumRepository;
    private StudentRepository studentRepository;
    private StudentFeatureProfileService profileService;
    private PersonalizedCurriculumPlanner curriculumPlanner;
    private TestRecommendationCoordinator coordinator;

    @BeforeEach
    void setUp() {
        stateService = mock(TestRecommendationStateService.class);
        testCurriculumRepository = mock(TestCurriculumRepository.class);
        studentRepository = mock(StudentRepository.class);
        profileService = mock(StudentFeatureProfileService.class);
        curriculumPlanner = mock(PersonalizedCurriculumPlanner.class);
        coordinator = new TestRecommendationCoordinator(
                stateService,
                testCurriculumRepository,
                studentRepository,
                profileService,
                curriculumPlanner
        );
    }

    @Test
    void recalculatesProfileAndCreatesRecommendationThenCompletesState() {
        StudentEntity student = mock(StudentEntity.class);
        when(student.getId()).thenReturn(15L);
        TestCurriculumEntity source = mock(TestCurriculumEntity.class);
        when(source.getStudent()).thenReturn(student);
        when(stateService.beginAttempt(500L)).thenReturn(true);
        when(testCurriculumRepository.findById(500L)).thenReturn(Optional.of(source));
        when(studentRepository.findById(15L)).thenReturn(Optional.of(student));
        when(curriculumPlanner.createRecommendedFromTestIfAbsent(student, 500L))
                .thenReturn(mock(DailyCurriculumEntity.class));

        coordinator.process(500L);

        verify(profileService).recalculate(student);
        verify(curriculumPlanner).createRecommendedFromTestIfAbsent(student, 500L);
        verify(stateService).complete(500L);
        verify(stateService, never()).fail(org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.any());
    }

    @Test
    void recordsFailureWithoutRollingBackCompletedTest() {
        StudentEntity student = mock(StudentEntity.class);
        when(student.getId()).thenReturn(15L);
        TestCurriculumEntity source = mock(TestCurriculumEntity.class);
        when(source.getStudent()).thenReturn(student);
        when(stateService.beginAttempt(500L)).thenReturn(true);
        when(testCurriculumRepository.findById(500L)).thenReturn(Optional.of(source));
        when(studentRepository.findById(15L)).thenReturn(Optional.of(student));
        IllegalStateException failure = new IllegalStateException("프로필 계산 실패");
        when(profileService.recalculate(student)).thenThrow(failure);

        coordinator.process(500L);

        verify(stateService).fail(500L, failure);
        verify(stateService, never()).complete(500L);
        verify(curriculumPlanner, never()).createRecommendedFromTestIfAbsent(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.anyLong()
        );
    }

    @Test
    void ignoresDuplicateProcessingRequest() {
        when(stateService.beginAttempt(500L)).thenReturn(false);

        coordinator.process(500L);

        verify(testCurriculumRepository, never()).findById(500L);
        verifyNoRecommendationWork();
    }

    @Test
    void retriesFailedRecommendationAndCompletesIt() {
        StudentEntity student = mock(StudentEntity.class);
        when(student.getId()).thenReturn(15L);
        TestCurriculumEntity source = mock(TestCurriculumEntity.class);
        when(source.getStudent()).thenReturn(student);
        when(stateService.prepareRetry(500L)).thenReturn(true);
        when(stateService.beginAttempt(500L)).thenReturn(true);
        when(testCurriculumRepository.findById(500L)).thenReturn(Optional.of(source));
        when(studentRepository.findById(15L)).thenReturn(Optional.of(student));
        when(curriculumPlanner.createRecommendedFromTestIfAbsent(student, 500L))
                .thenReturn(mock(DailyCurriculumEntity.class));

        boolean accepted = coordinator.retry(500L);

        org.assertj.core.api.Assertions.assertThat(accepted).isTrue();
        verify(stateService).prepareRetry(500L);
        verify(stateService).complete(500L);
    }

    @Test
    void isolatesFailureWhileStartingRecommendationProcessing() {
        when(stateService.beginAttempt(500L))
                .thenThrow(new IllegalStateException("상태 저장 실패"));

        org.assertj.core.api.Assertions.assertThatCode(() -> coordinator.process(500L))
                .doesNotThrowAnyException();

        verifyNoRecommendationWork();
    }

    @Test
    void isolatesFailureWhileRecordingRecommendationFailure() {
        StudentEntity student = mock(StudentEntity.class);
        when(student.getId()).thenReturn(15L);
        TestCurriculumEntity source = mock(TestCurriculumEntity.class);
        when(source.getStudent()).thenReturn(student);
        when(stateService.beginAttempt(500L)).thenReturn(true);
        when(testCurriculumRepository.findById(500L)).thenReturn(Optional.of(source));
        when(studentRepository.findById(15L)).thenReturn(Optional.of(student));
        when(profileService.recalculate(student))
                .thenThrow(new IllegalStateException("프로필 계산 실패"));
        doThrow(new IllegalStateException("실패 상태 저장 실패"))
                .when(stateService)
                .fail(org.mockito.ArgumentMatchers.eq(500L), org.mockito.ArgumentMatchers.any());

        org.assertj.core.api.Assertions.assertThatCode(() -> coordinator.process(500L))
                .doesNotThrowAnyException();
    }

    private void verifyNoRecommendationWork() {
        verify(profileService, never()).recalculate(org.mockito.ArgumentMatchers.any());
        verify(curriculumPlanner, never()).createRecommendedFromTestIfAbsent(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.anyLong()
        );
    }
}
