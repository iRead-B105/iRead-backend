package com.iread.backend.test.config;

import com.iread.backend.student.domain.StudentEntity;
import com.iread.backend.student.repository.StudentRepository;
import com.iread.backend.test.app.service.AppTestService;
import com.iread.backend.test.domain.StudentTestEntity;
import com.iread.backend.test.domain.TestCurriculumEntity;
import com.iread.backend.test.domain.TestDataEntity;
import com.iread.backend.test.domain.TestStatus;
import com.iread.backend.test.repository.StudentTestRepository;
import com.iread.backend.test.repository.TestCurriculumRepository;
import com.iread.backend.test.repository.TestDataRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DemoChallengeSeedInitializerTest {

    @Mock StudentRepository studentRepository;
    @Mock TestCurriculumRepository testCurriculumRepository;
    @Mock StudentTestRepository testRepository;
    @Mock TestDataRepository testDataRepository;
    @Mock AppTestService appTestService;
    @Mock TransactionTemplate transactionTemplate;

    private DemoChallengeSeedInitializer initializer;

    @BeforeEach
    void setUp() {
        initializer = new DemoChallengeSeedInitializer(
                studentRepository,
                testCurriculumRepository,
                testRepository,
                testDataRepository,
                appTestService,
                JsonMapper.builder().build(),
                transactionTemplate
        );
        doAnswer(invocation -> {
            Consumer<TransactionStatus> action = invocation.getArgument(0);
            action.accept(mock(TransactionStatus.class));
            return null;
        }).when(transactionTemplate).executeWithoutResult(any());
    }

    @Test
    void doesNotAddIncompleteHistoryWhenThreeStoredTestsContainNineQuestions() {
        StudentEntity student = mock(StudentEntity.class);
        when(student.getId()).thenReturn(2001L);
        when(studentRepository.findById(2001L)).thenReturn(Optional.of(student));
        when(studentRepository.findById(2002L)).thenReturn(Optional.empty());
        when(studentRepository.findById(2103L)).thenReturn(Optional.empty());

        TestCurriculumEntity completed = mock(TestCurriculumEntity.class);
        when(completed.getId()).thenReturn(339001L);
        when(completed.getStatus()).thenReturn(TestStatus.COMPLETED.name());
        when(testCurriculumRepository.findAllByStudentIdOrderByCreatedAtDescIdDesc(2001L))
                .thenReturn(List.of(completed));

        List<StudentTestEntity> tests = List.of(
                storedTest(343011L),
                storedTest(343012L),
                storedTest(343013L)
        );
        when(testRepository.findAllByTestCurriculumIdOrderBySequenceNoAscIdAsc(339001L))
                .thenReturn(tests);
        for (StudentTestEntity test : tests) {
            TestDataEntity data = mock(TestDataEntity.class);
            when(data.getGeneratedData()).thenReturn(
                    "{\"questions\":[{\"questionNo\":1},{\"questionNo\":2},{\"questionNo\":3}]}"
            );
            when(testDataRepository.findFirstByTestIdOrderByCreatedAtDescIdDesc(test.getId()))
                    .thenReturn(Optional.of(data));
        }

        initializer.run(null);

        verify(appTestService, times(1)).createChallenge(
                eq(student),
                any(LocalDateTime.class),
                eq(true)
        );
    }

    private StudentTestEntity storedTest(long id) {
        StudentTestEntity test = mock(StudentTestEntity.class);
        when(test.getId()).thenReturn(id);
        return test;
    }
}
