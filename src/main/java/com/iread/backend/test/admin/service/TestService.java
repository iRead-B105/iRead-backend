package com.iread.backend.test.admin.service;

import com.iread.backend.exception.ResourceNotFoundException;
import com.iread.backend.student.repository.StudentRepository;
import com.iread.backend.test.admin.dto.res.TestCompareResponse;
import com.iread.backend.test.admin.dto.res.TestListResponse;
import com.iread.backend.test.domain.StudentTestEntity;
import com.iread.backend.test.domain.TestStatus;
import com.iread.backend.test.repository.StudentTestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TestService {
    private final StudentRepository studentRepository;
    private final StudentTestRepository testRepository;
    private final ObjectMapper objectMapper;

    public List<TestListResponse> getTestList(Long teacherId, Long studentId) {
        validateStudentOwner(teacherId, studentId);
        return completedTests(studentId).stream()
                .map(test -> new TestListResponse(test.getId(), test.getCreatedAt().toLocalDate()))
                .toList();
    }

    public TestCompareResponse compareTests(Long teacherId, Long studentId, Long currentTestId,
                                            List<Long> comparisonTestIds) {
        validateStudentOwner(teacherId, studentId);
        List<Long> resolvedComparisonIds = comparisonTestIds == null
                ? List.of() : List.copyOf(comparisonTestIds);
        if (resolvedComparisonIds.contains(currentTestId)) {
            throw new IllegalArgumentException("현재 테스트는 비교 목록에 포함할 수 없습니다.");
        }
        if (new HashSet<>(resolvedComparisonIds).size() != resolvedComparisonIds.size()) {
            throw new IllegalArgumentException("비교 테스트 ID를 중복해서 지정할 수 없습니다.");
        }

        StudentTestEntity current = findCompletedTest(studentId, currentTestId);
        if (resolvedComparisonIds.isEmpty()) {
            return new TestCompareResponse(toDetail(current), List.of());
        }
        Map<Long, StudentTestEntity> comparisons = testRepository
                .findAllByIdInAndTestCurriculumStudentIdAndStatus(
                        resolvedComparisonIds, studentId, TestStatus.COMPLETED
                )
                .stream().collect(Collectors.toMap(StudentTestEntity::getId, Function.identity()));
        if (comparisons.size() != resolvedComparisonIds.size()) {
            throw new ResourceNotFoundException("완료된 비교 테스트를 찾을 수 없습니다.");
        }

        return new TestCompareResponse(
                toDetail(current),
                resolvedComparisonIds.stream().map(comparisons::get).map(this::toDetail).toList()
        );
    }

    private TestCompareResponse.TestDetail toDetail(StudentTestEntity test) {
        JsonNode root = parseResult(test.getResult());
        return new TestCompareResponse.TestDetail(
                test.getId(),
                test.getCreatedAt().toLocalDate(),
                nullableLong(root.get("readingTimeSeconds")),
                nullableLong(root.get("solvingTimeSeconds")),
                test.getAccuracy(),
                nullableInteger(root.get("gazeDepartureCount")),
                parseQuestions(root.path("questions"))
        );
    }

    private List<TestCompareResponse.QuestionResult> parseQuestions(JsonNode questions) {
        if (!questions.isArray()) return List.of();
        List<TestCompareResponse.QuestionResult> result = new ArrayList<>();
        questions.forEach(question -> result.add(new TestCompareResponse.QuestionResult(
                question.path("questionNumber").asInt(),
                question.path("question").asText(null),
                question.hasNonNull("isCorrect") ? question.path("isCorrect").asBoolean() : null,
                question.path("correctAnswer").asText(null),
                question.path("selectedAnswer").asText(null)
        )));
        return result;
    }

    private List<StudentTestEntity> completedTests(Long studentId) {
        return testRepository.findAllByTestCurriculumStudentIdAndStatusOrderByCreatedAtDesc(
                studentId, TestStatus.COMPLETED);
    }

    private StudentTestEntity findCompletedTest(Long studentId, Long testId) {
        return testRepository.findByIdAndTestCurriculumStudentIdAndStatus(
                        testId, studentId, TestStatus.COMPLETED)
                .orElseThrow(() -> new ResourceNotFoundException("완료된 테스트를 찾을 수 없습니다."));
    }

    private void validateStudentOwner(Long teacherId, Long studentId) {
        studentRepository.findByIdAndTeacherId(studentId, teacherId)
                .orElseThrow(() -> new ResourceNotFoundException("학생을 찾을 수 없습니다."));
    }

    private JsonNode parseResult(String result) {
        if (result == null || result.isBlank()) return objectMapper.createObjectNode();
        try {
            JsonNode node = objectMapper.readTree(result);
            if (node != null && node.isObject()) return node;
        } catch (Exception ignored) {
        }
        throw new IllegalArgumentException("저장된 테스트 결과 형식이 올바르지 않습니다.");
    }

    private BigDecimal decimal(JsonNode node) {
        return node == null || node.isNull() || !node.isNumber() ? null : node.decimalValue();
    }

    private Long nullableLong(JsonNode node) {
        return node == null || node.isNull() || !node.isNumber() ? null : node.asLong();
    }

    private Integer nullableInteger(JsonNode node) {
        return node == null || node.isNull() || !node.isNumber() ? null : node.asInt();
    }
}
