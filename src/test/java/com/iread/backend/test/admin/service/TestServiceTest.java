package com.iread.backend.test.admin.service;

import com.iread.backend.student.domain.StudentEntity;
import com.iread.backend.student.repository.StudentRepository;
import com.iread.backend.test.domain.StudentTestEntity;
import com.iread.backend.test.domain.TestStatus;
import com.iread.backend.test.repository.StudentTestRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TestServiceTest {
    @Mock StudentRepository studentRepository;
    @Mock StudentTestRepository testRepository;

    private TestService testService;

    @BeforeEach
    void setUp() {
        testService = new TestService(studentRepository, testRepository, JsonMapper.builder().build());
    }

    @Test
    void 완료된_테스트_목록만_최신순으로_반환한다() {
        allowStudent();
        StudentTestEntity test = test(10L, LocalDateTime.of(2026, 7, 21, 10, 0), "{}", "85.00");
        when(testRepository.findAllByTestCurriculumStudentIdAndStatusOrderByCreatedAtDesc(1L, TestStatus.COMPLETED))
                .thenReturn(List.of(test));

        var result = testService.getTestList(100L, 1L);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().testId()).isEqualTo(10L);
        assertThat(result.getFirst().date()).isEqualTo(LocalDate.of(2026, 7, 21));
    }

    @Test
    void 현재_테스트와_비교_테스트의_측정값과_문항을_반환한다() {
        allowStudent();
        StudentTestEntity current = test(10L, LocalDateTime.of(2026, 7, 21, 10, 0), resultJson(120, 180), "85.50");
        StudentTestEntity previous = test(9L, LocalDateTime.of(2026, 6, 21, 10, 0), resultJson(140, 200), "80.00");
        when(testRepository.findByIdAndTestCurriculumStudentIdAndStatus(10L, 1L, TestStatus.COMPLETED))
                .thenReturn(Optional.of(current));
        when(testRepository.findAllByIdInAndTestCurriculumStudentIdAndStatus(List.of(9L), 1L, TestStatus.COMPLETED))
                .thenReturn(List.of(previous));

        var result = testService.compareTests(100L, 1L, 10L, List.of(9L));

        assertThat(result.currentTest().readingTimeSeconds()).isEqualTo(120L);
        assertThat(result.currentTest().accuracy()).isEqualByComparingTo("85.50");
        assertThat(result.currentTest().questions().getFirst().isCorrect()).isTrue();
        assertThat(result.comparisonTests().getFirst().testId()).isEqualTo(9L);
    }

    @Test
    void 비교_대상을_생략하면_기준_테스트만_반환한다() {
        allowStudent();
        StudentTestEntity current = test(
                10L,
                LocalDateTime.of(2026, 7, 21, 10, 0),
                resultJson(120, 180),
                "85.50"
        );
        when(testRepository.findByIdAndTestCurriculumStudentIdAndStatus(10L, 1L, TestStatus.COMPLETED))
                .thenReturn(Optional.of(current));

        var result = testService.compareTests(100L, 1L, 10L, null);

        assertThat(result.currentTest().testId()).isEqualTo(10L);
        assertThat(result.comparisonTests()).isEmpty();
    }

    @Test
    void 완료되지_않은_비교_테스트가_포함되면_오류가_발생한다() {
        allowStudent();
        StudentTestEntity current = test(10L, LocalDateTime.now(), "{}", "85.00");
        when(testRepository.findByIdAndTestCurriculumStudentIdAndStatus(10L, 1L, TestStatus.COMPLETED))
                .thenReturn(Optional.of(current));
        when(testRepository.findAllByIdInAndTestCurriculumStudentIdAndStatus(List.of(9L), 1L, TestStatus.COMPLETED))
                .thenReturn(List.of());

        assertThatThrownBy(() -> testService.compareTests(100L, 1L, 10L, List.of(9L)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("완료된 비교 테스트를 찾을 수 없습니다.");
    }

    @Test
    void 담당하지_않는_학생의_테스트는_조회할_수_없다() {
        when(studentRepository.findByIdAndTeacherId(1L, 100L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> testService.getTestList(100L, 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("학생을 찾을 수 없습니다.");
    }

    private void allowStudent() {
        when(studentRepository.findByIdAndTeacherId(1L, 100L)).thenReturn(Optional.of(mock(StudentEntity.class)));
    }

    private StudentTestEntity test(Long id, LocalDateTime createdAt, String result, String accuracy) {
        StudentTestEntity test = instantiate();
        ReflectionTestUtils.setField(test, "id", id);
        ReflectionTestUtils.setField(test, "createdAt", createdAt);
        ReflectionTestUtils.setField(test, "status", TestStatus.COMPLETED);
        ReflectionTestUtils.setField(test, "result", result);
        ReflectionTestUtils.setField(test, "accuracy", new BigDecimal(accuracy));
        return test;
    }

    private StudentTestEntity instantiate() {
        try {
            var constructor = StudentTestEntity.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    private String resultJson(int readingTime, int solvingTime) {
        return """
                {
                  "readingTimeSeconds":%d,
                  "solvingTimeSeconds":%d,
                  "gazeDepartureCount":3,
                  "questions":[{
                    "questionNumber":1,
                    "question":"문항",
                    "isCorrect":true,
                    "correctAnswer":"정답",
                    "selectedAnswer":"정답"
                  }]
                }
                """.formatted(readingTime, solvingTime);
    }
}
