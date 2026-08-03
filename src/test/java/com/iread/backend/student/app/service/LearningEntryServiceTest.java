package com.iread.backend.student.app.service;

import com.iread.backend.exception.ResourceNotFoundException;
import com.iread.backend.student.app.dto.res.LearningEntryStatus;
import com.iread.backend.student.domain.StudentEntity;
import com.iread.backend.student.repository.StudentRepository;
import com.iread.backend.test.domain.TestCurriculumEntity;
import com.iread.backend.test.domain.TestStatus;
import com.iread.backend.test.repository.StudentTestRepository;
import com.iread.backend.test.repository.TestCurriculumRepository;
import com.iread.backend.training.repository.DailyCurriculumRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LearningEntryServiceTest {
    @Mock StudentRepository studentRepository;
    @Mock TestCurriculumRepository testCurriculumRepository;
    @Mock StudentTestRepository studentTestRepository;
    @Mock DailyCurriculumRepository dailyCurriculumRepository;
    @InjectMocks LearningEntryService service;

    @Test
    void returnsChallengeRequiredForStudentWithoutTestOrTrainingHistory() {
        allowOwnedStudent();
        when(testCurriculumRepository.existsByStudentIdAndStatus(
                20L, TestStatus.COMPLETED.name()
        )).thenReturn(false);
        when(dailyCurriculumRepository.existsByStudentId(20L)).thenReturn(false);
        when(testCurriculumRepository
                .findFirstByStudentIdAndStatusInOrderByCreatedAtDescIdDesc(any(), any()))
                .thenReturn(Optional.empty());

        var response = service.getLearningEntry(1L, 20L);

        assertThat(response.studentId()).isEqualTo(20L);
        assertThat(response.entryStatus()).isEqualTo(LearningEntryStatus.CHALLENGE_REQUIRED);
        assertThat(response.testCurriculumId()).isNull();
        assertThat(response.completedQuestions()).isZero();
        assertThat(response.totalQuestions()).isEqualTo(9);
    }

    @Test
    void returnsChallengeInProgressForStudentWithoutPriorLearningHistory() {
        allowOwnedStudent();
        when(testCurriculumRepository.existsByStudentIdAndStatus(
                20L, TestStatus.COMPLETED.name()
        )).thenReturn(false);
        when(dailyCurriculumRepository.existsByStudentId(20L)).thenReturn(false);
        TestCurriculumEntity curriculum = mock(TestCurriculumEntity.class);
        when(curriculum.getId()).thenReturn(50L);
        when(testCurriculumRepository
                .findFirstByStudentIdAndStatusInOrderByCreatedAtDescIdDesc(any(), any()))
                .thenReturn(Optional.of(curriculum));
        when(studentTestRepository.countByTestCurriculumIdAndStatus(
                50L, TestStatus.COMPLETED
        )).thenReturn(4L);

        var response = service.getLearningEntry(1L, 20L);

        assertThat(response.entryStatus())
                .isEqualTo(LearningEntryStatus.CHALLENGE_IN_PROGRESS);
        assertThat(response.testCurriculumId()).isEqualTo(50L);
        assertThat(response.completedQuestions()).isEqualTo(4);
        assertThat(response.totalQuestions()).isEqualTo(9);
    }

    @Test
    void returnsHomeWhenInitialChallengeWasCompleted() {
        allowOwnedStudent();
        when(testCurriculumRepository.existsByStudentIdAndStatus(
                20L, TestStatus.COMPLETED.name()
        )).thenReturn(true);

        var response = service.getLearningEntry(1L, 20L);

        assertThat(response.entryStatus()).isEqualTo(LearningEntryStatus.HOME);
        assertThat(response.testCurriculumId()).isNull();
        verifyNoInteractions(dailyCurriculumRepository);
    }

    @Test
    void completedChallengeWinsOverStaleIncompleteChallenge() {
        allowOwnedStudent();
        when(testCurriculumRepository.existsByStudentIdAndStatus(
                20L, TestStatus.COMPLETED.name()
        )).thenReturn(true);

        var response = service.getLearningEntry(1L, 20L);

        assertThat(response.entryStatus()).isEqualTo(LearningEntryStatus.HOME);
        assertThat(response.testCurriculumId()).isNull();
        verify(testCurriculumRepository, never())
                .findFirstByStudentIdAndStatusInOrderByCreatedAtDescIdDesc(any(), any());
        verifyNoInteractions(studentTestRepository, dailyCurriculumRepository);
    }

    @Test
    void returnsHomeWhenTrainingCurriculumHistoryExists() {
        allowOwnedStudent();
        when(testCurriculumRepository.existsByStudentIdAndStatus(
                20L, TestStatus.COMPLETED.name()
        )).thenReturn(false);
        when(dailyCurriculumRepository.existsByStudentId(20L)).thenReturn(true);

        var response = service.getLearningEntry(1L, 20L);

        assertThat(response.entryStatus()).isEqualTo(LearningEntryStatus.HOME);
    }

    @Test
    void rejectsStudentOutsideTeacherOwnership() {
        when(studentRepository.findByIdAndTeacherId(20L, 1L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getLearningEntry(1L, 20L))
                .isInstanceOf(ResourceNotFoundException.class);
        verifyNoInteractions(
                testCurriculumRepository,
                studentTestRepository,
                dailyCurriculumRepository
        );
    }

    private void allowOwnedStudent() {
        when(studentRepository.findByIdAndTeacherId(20L, 1L))
                .thenReturn(Optional.of(mock(StudentEntity.class)));
    }
}
