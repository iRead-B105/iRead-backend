package com.iread.backend.test.app.service;

import com.iread.backend.student.domain.StudentEntity;
import com.iread.backend.student.repository.StudentRepository;
import com.iread.backend.test.app.dto.req.TestCompleteRequest;
import com.iread.backend.test.domain.StudentTestEntity;
import com.iread.backend.test.domain.TestStatus;
import com.iread.backend.test.repository.StudentTestRepository;
import com.iread.backend.test.repository.TestDataRepository;
import com.iread.backend.training.repository.WordRepository;
import com.iread.backend.wordattempt.domain.WordAttemptLogEntity;
import com.iread.backend.wordattempt.repository.WordAttemptLogRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AppTestServiceTest {
    @Mock StudentRepository studentRepository;
    @Mock StudentTestRepository testRepository;
    @Mock TestDataRepository testDataRepository;
    @Mock WordRepository wordRepository;
    @Mock WordAttemptLogRepository wordAttemptLogRepository;
    @Mock ObjectMapper objectMapper;
    @InjectMocks AppTestService appTestService;

    @Test
    void startsCurrentNotStartedTest() {
        StudentEntity student = mock(StudentEntity.class);
        StudentTestEntity test = mock(StudentTestEntity.class);
        when(studentRepository.findByIdAndTeacherId(20L, 1L)).thenReturn(Optional.of(student));
        when(testRepository
                .findFirstByTestCurriculumStudentIdAndStatusInOrderByTestCurriculumCreatedAtDescSequenceNoAscIdAsc(
                        any(),
                        any()
                ))
                .thenReturn(Optional.of(test));
        when(test.getId()).thenReturn(30L);
        when(test.getStatus()).thenReturn(TestStatus.IN_PROGRESS);

        var result = appTestService.start(1L, 20L);

        verify(test).start(any(LocalDateTime.class));
        assertThat(result.testId()).isEqualTo(30L);
        assertThat(result.status()).isEqualTo(TestStatus.IN_PROGRESS);
        assertThat(result.startedAt()).isNotNull();
    }

    @Test
    void completesTestWithServerCalculatedAccuracy() throws Exception {
        StudentEntity student = mock(StudentEntity.class);
        StudentTestEntity test = mock(StudentTestEntity.class);
        WordAttemptLogEntity first = mock(WordAttemptLogEntity.class);
        WordAttemptLogEntity second = mock(WordAttemptLogEntity.class);
        LocalDateTime completedAt = LocalDateTime.of(2026, 7, 27, 15, 0);
        when(studentRepository.findByIdAndTeacherId(20L, 1L)).thenReturn(Optional.of(student));
        when(testRepository.findByIdAndTestCurriculumStudentId(30L, 20L))
                .thenReturn(Optional.of(test));
        when(test.getId()).thenReturn(30L);
        when(test.getStatus()).thenReturn(TestStatus.IN_PROGRESS, TestStatus.COMPLETED);
        when(test.getResult()).thenReturn(null);
        when(first.getTotalScore()).thenReturn(900);
        when(second.getTotalScore()).thenReturn(800);
        when(wordAttemptLogRepository.findAllByTestIdOrderByIdAsc(30L))
                .thenReturn(List.of(first, second));
        when(objectMapper.writeValueAsString(any())).thenReturn("{\"attemptCount\":2}");
        when(test.getAccuracy()).thenReturn(new BigDecimal("85.00"));
        when(test.getFinishedAt()).thenReturn(completedAt);

        var result = appTestService.complete(
                1L,
                20L,
                new TestCompleteRequest(30L, completedAt)
        );

        verify(test).complete("{\"attemptCount\":2}", new BigDecimal("85.00"), completedAt);
        assertThat(result.testId()).isEqualTo(30L);
        assertThat(result.status()).isEqualTo(TestStatus.COMPLETED);
        assertThat(result.accuracy()).isEqualByComparingTo("85.00");
        assertThat(result.completedAt()).isEqualTo(completedAt);
    }
}
