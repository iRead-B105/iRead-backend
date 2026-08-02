package com.iread.backend.test.recommendation;

import com.iread.backend.readingfeature.service.StudentFeatureProfileService;
import com.iread.backend.student.domain.StudentEntity;
import com.iread.backend.student.repository.StudentRepository;
import com.iread.backend.test.domain.TestCurriculumEntity;
import com.iread.backend.test.repository.TestCurriculumRepository;
import com.iread.backend.training.curriculum.PersonalizedCurriculumPlanner;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TestRecommendationWorkServiceTest {

    @Test
    void recalculatesProfileAndCreatesRecommendationInOneWorkBoundary() {
        TestCurriculumRepository testCurriculums = mock(TestCurriculumRepository.class);
        StudentRepository students = mock(StudentRepository.class);
        StudentFeatureProfileService profiles = mock(StudentFeatureProfileService.class);
        PersonalizedCurriculumPlanner planner = mock(PersonalizedCurriculumPlanner.class);
        StudentEntity student = mock(StudentEntity.class);
        TestCurriculumEntity source = mock(TestCurriculumEntity.class);
        when(source.getStudent()).thenReturn(student);
        when(student.getId()).thenReturn(15L);
        when(testCurriculums.findById(500L)).thenReturn(Optional.of(source));
        when(students.findById(15L)).thenReturn(Optional.of(student));
        TestRecommendationWorkService service = new TestRecommendationWorkService(
                testCurriculums,
                students,
                profiles,
                planner
        );

        service.process(500L);

        verify(profiles).recalculate(student);
        verify(planner).createRecommendedFromTestIfAbsent(student, 500L);
    }

    @Test
    void alwaysStartsNewTransactionAfterOriginalCompletionCommit() throws Exception {
        Method method = TestRecommendationWorkService.class
                .getMethod("process", Long.class);
        Transactional transactional = method.getAnnotation(Transactional.class);

        assertThat(transactional).isNotNull();
        assertThat(transactional.propagation()).isEqualTo(Propagation.REQUIRES_NEW);
    }
}
