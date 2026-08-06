package com.iread.backend.test.config;

import com.iread.backend.student.domain.StudentEntity;
import com.iread.backend.student.repository.StudentRepository;
import com.iread.backend.test.app.service.AppTestService;
import com.iread.backend.test.domain.StudentTestEntity;
import com.iread.backend.test.domain.TestCurriculumEntity;
import com.iread.backend.test.domain.TestStatus;
import com.iread.backend.test.repository.TestDataRepository;
import com.iread.backend.test.repository.StudentTestRepository;
import com.iread.backend.test.repository.TestCurriculumRepository;
import com.iread.backend.training.generation.TrainingGenerationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 앱 시연용 데모 학습자 3명(샛별·한결·박서아)의 실력도전 상태를 현행 9문항 형식으로 맞춘다.
 *
 * - 이미 완료된 초기 검사 이력 1회분: 과거 시점의 COMPLETED 9문항 검사
 * - 진행 가능한 검사 1회분: 문항 데이터까지 생성된 NOT_STARTED 9문항 검사
 *
 * 신규 학생은 앱 최초 진입 시 AppTestService.createChallenge가 9문항을 만들고,
 * 이후 회차는 새벽 배치가 생성하는 구조이므로 여기서는 데모 학습자만 보정한다.
 * 9문항이 아닌 과거(레거시) 검사 이력은 플랜 대상이 아니므로 건드리지 않는다.
 */
@Slf4j
@Component
@Order(60)
@RequiredArgsConstructor
@ConditionalOnProperty(
        name = "iread.demo-personalized-curriculum.enabled",
        havingValue = "true"
)
public class DemoChallengeSeedInitializer implements ApplicationRunner {

    static final int CHALLENGE_QUESTION_COUNT = 9;
    private static final List<Long> DEMO_LEARNER_IDS = List.of(2001L, 2002L, 2103L);
    private static final LocalDateTime HISTORY_CREATED_AT =
            LocalDateTime.of(2026, 7, 21, 10, 0);

    private final StudentRepository studentRepository;
    private final TestCurriculumRepository testCurriculumRepository;
    private final StudentTestRepository testRepository;
    private final TestDataRepository testDataRepository;
    private final AppTestService appTestService;
    private final ObjectMapper objectMapper;
    private final TransactionTemplate transactionTemplate;

    @Override
    public void run(ApplicationArguments args) {
        for (Long studentId : DEMO_LEARNER_IDS) {
            try {
                transactionTemplate.executeWithoutResult(status ->
                        studentRepository.findById(studentId).ifPresent(this::seedStudent)
                );
            } catch (TrainingGenerationException exception) {
                // 시드 실패가 서버 기동을 막지 않도록 하고, 원인 진단용 이슈를 남긴다
                log.error(
                        "데모 실력도전 시드 실패: studentId={}, issues={}",
                        studentId,
                        exception.getIssues(),
                        exception
                );
            }
        }
    }

    private void seedStudent(StudentEntity student) {
        List<TestCurriculumEntity> curriculums = testCurriculumRepository
                .findAllByStudentIdOrderByCreatedAtDescIdDesc(student.getId());
        List<TestCurriculumEntity> currentFormat = curriculums
                .stream()
                .filter(curriculum -> testCount(curriculum) == CHALLENGE_QUESTION_COUNT)
                .toList();

        boolean hasCompletedHistory = curriculums.stream()
                .filter(curriculum ->
                        TestStatus.COMPLETED.name().equals(curriculum.getStatus())
                )
                .anyMatch(curriculum ->
                        questionCount(curriculum) == CHALLENGE_QUESTION_COUNT
                );
        if (!hasCompletedHistory) {
            seedCompletedHistory(student);
        }

        boolean hasPendingChallenge = currentFormat.stream().anyMatch(
                curriculum -> !TestStatus.COMPLETED.name().equals(curriculum.getStatus())
        );
        // 학습자가 직접 완료한 9문항 검사가 이미 2회 이상이면(이력+응시 완료)
        // 재시드하지 않는다. 다음 회차는 새벽 배치가 만든다.
        boolean alreadyTookSeededChallenge = currentFormat.size() >= 2;
        if (!hasPendingChallenge && !alreadyTookSeededChallenge) {
            appTestService.createChallenge(student, LocalDateTime.now(), true);
        }
    }

    /** 과거 시점에 완료한 초기 검사 이력을 만든다. */
    private void seedCompletedHistory(StudentEntity student) {
        TestCurriculumEntity curriculum =
                appTestService.createChallenge(student, HISTORY_CREATED_AT, true);
        List<StudentTestEntity> tests = testRepository
                .findAllByTestCurriculumIdOrderBySequenceNoAscIdAsc(curriculum.getId());
        curriculum.start();
        LocalDateTime startedAt = HISTORY_CREATED_AT.plusMinutes(5);
        for (int index = 0; index < tests.size(); index++) {
            StudentTestEntity test = tests.get(index);
            LocalDateTime testStartedAt = startedAt.plusMinutes(index * 2L);
            int score = historyScore(student.getId(), index);
            test.start(testStartedAt);
            test.complete(
                    writeJson(historyResult(score, index)),
                    BigDecimal.valueOf(score),
                    testStartedAt.plusMinutes(2)
            );
        }
        curriculum.complete(startedAt.plusMinutes(tests.size() * 2L + 3));
        // 시드 이력은 추천 배치 대상이 아니므로 추천 처리를 마감해 둔다
        curriculum.completeRecommendation();
        testCurriculumRepository.saveAndFlush(curriculum);
    }

    private int historyScore(Long studentId, int index) {
        // 학생·문항별로 60~89 사이의 결정적 점수를 만든다
        return 60 + (int) ((studentId + index * 7L) % 30);
    }

    private ObjectNode historyResult(int score, int index) {
        ObjectNode result = objectMapper.createObjectNode();
        result.put("overallScore", score);
        if (index == 0) {
            result.putNull("changeFromPrevious");
        } else {
            result.put("changeFromPrevious", 1.5);
        }
        result.putArray("strengthAreas").add("음운 인식");
        result.putArray("improvementAreas").add("유창성");
        result.put("recommendedCourse", "유창성 집중 훈련");
        result.put("nextTestRecommendation", "2주 후 유창성 영역의 같은 난이도 재검사를 권장합니다.");
        return result;
    }

    private int testCount(TestCurriculumEntity curriculum) {
        return testRepository
                .findAllByTestCurriculumIdOrderBySequenceNoAscIdAsc(curriculum.getId())
                .size();
    }

    private int questionCount(TestCurriculumEntity curriculum) {
        return testRepository
                .findAllByTestCurriculumIdOrderBySequenceNoAscIdAsc(curriculum.getId())
                .stream()
                .mapToInt(test -> testDataRepository
                        .findFirstByTestIdOrderByCreatedAtDescIdDesc(test.getId())
                        .map(data -> questionCount(data.getGeneratedData()))
                        .orElse(0))
                .sum();
    }

    private int questionCount(String generatedData) {
        try {
            return objectMapper.readTree(generatedData).path("questions").size();
        } catch (Exception exception) {
            return 0;
        }
    }

    private String writeJson(ObjectNode value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception exception) {
            throw new IllegalStateException("데모 검사 이력 결과를 저장하지 못했습니다.", exception);
        }
    }
}
