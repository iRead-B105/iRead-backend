package com.iread.backend.test.admin.service;

import com.iread.backend.exception.ResourceNotFoundException;
import com.iread.backend.student.domain.StudentEntity;
import com.iread.backend.student.repository.StudentRepository;
import com.iread.backend.test.admin.dto.res.TestCurriculumDetailResponse;
import com.iread.backend.test.admin.dto.res.TestCurriculumListResponse;
import com.iread.backend.test.admin.result.TestCurriculumResultAggregator;
import com.iread.backend.test.domain.StudentTestEntity;
import com.iread.backend.test.domain.TestCurriculumEntity;
import com.iread.backend.test.repository.StudentTestRepository;
import com.iread.backend.test.repository.TestCurriculumRepository;
import com.iread.backend.training.repository.DailyCurriculumRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class TestCurriculumAdminServiceTest {
    private StudentRepository studentRepository;
    private TestCurriculumRepository testCurriculumRepository;
    private StudentTestRepository studentTestRepository;
    private DailyCurriculumRepository dailyCurriculumRepository;
    private TestCurriculumResultAggregator resultAggregator;
    private TestCurriculumAdminService service;

    @BeforeEach
    void setUp() {
        studentRepository = mock(StudentRepository.class);
        testCurriculumRepository = mock(TestCurriculumRepository.class);
        studentTestRepository = mock(StudentTestRepository.class);
        dailyCurriculumRepository = mock(DailyCurriculumRepository.class);
        resultAggregator = mock(TestCurriculumResultAggregator.class);
        service = new TestCurriculumAdminService(
                studentRepository,
                testCurriculumRepository,
                studentTestRepository,
                dailyCurriculumRepository,
                resultAggregator
        );
    }

    @Test
    void returnsOneListRowPerCompletedCurriculum() {
        StudentEntity student = mock(StudentEntity.class);
        TestCurriculumEntity curriculum = mock(TestCurriculumEntity.class);
        StudentTestEntity test = mock(StudentTestEntity.class);
        var row = new TestCurriculumListResponse.Item(
                30L, "COMPLETED", null, null, 9, 9, null
        );
        when(studentRepository.findByIdAndTeacherId(20L, 10L))
                .thenReturn(Optional.of(student));
        when(testCurriculumRepository.findAllByStudentIdAndStatusOrderByCreatedAtDescIdDesc(
                20L,
                "COMPLETED"
        ))
                .thenReturn(List.of(curriculum));
        when(curriculum.getId()).thenReturn(30L);
        when(studentTestRepository.findAllByTestCurriculumIdOrderBySequenceNoAscIdAsc(30L))
                .thenReturn(List.of(test));
        when(resultAggregator.summarize(curriculum, List.of(test))).thenReturn(row);

        var result = service.getCurriculums(10L, 20L);

        assertThat(result.curriculums()).containsExactly(row);
    }

    @Test
    void returnsOwnedCurriculumDetail() {
        StudentEntity student = mock(StudentEntity.class);
        TestCurriculumEntity curriculum = mock(TestCurriculumEntity.class);
        var detail = mock(TestCurriculumDetailResponse.class);
        when(studentRepository.findByIdAndTeacherId(20L, 10L))
                .thenReturn(Optional.of(student));
        when(testCurriculumRepository.findByIdAndStudentIdAndStatus(30L, 20L, "COMPLETED"))
                .thenReturn(Optional.of(curriculum));
        when(studentTestRepository.findAllByTestCurriculumIdOrderBySequenceNoAscIdAsc(30L))
                .thenReturn(List.of());
        when(curriculum.getId()).thenReturn(30L);
        when(resultAggregator.aggregate(curriculum, List.of(), null)).thenReturn(detail);

        assertThat(service.getCurriculum(10L, 20L, 30L)).isSameAs(detail);
    }

    @Test
    void rejectsIncompleteCurriculumDetailFromTestHistory() {
        StudentEntity student = mock(StudentEntity.class);
        when(studentRepository.findByIdAndTeacherId(20L, 10L))
                .thenReturn(Optional.of(student));
        when(testCurriculumRepository.findByIdAndStudentIdAndStatus(30L, 20L, "COMPLETED"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getCurriculum(10L, 20L, 30L))
                .isInstanceOf(ResourceNotFoundException.class);
        verifyNoInteractions(studentTestRepository, resultAggregator);
    }

    @Test
    void rejectsStudentOwnedByAnotherTeacherBeforeReadingResults() {
        when(studentRepository.findByIdAndTeacherId(20L, 10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getCurriculums(10L, 20L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("학생");
        verifyNoInteractions(testCurriculumRepository, studentTestRepository, resultAggregator);
    }
}
