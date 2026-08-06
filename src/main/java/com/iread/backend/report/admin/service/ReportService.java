package com.iread.backend.report.admin.service;

import com.iread.backend.gaze.domain.GazeAnalysisResultEntity;
import com.iread.backend.gaze.domain.GazeContentType;
import com.iread.backend.gaze.domain.GazeSessionEntity;
import com.iread.backend.gaze.domain.GazeSessionStatus;
import com.iread.backend.gaze.repository.GazeAnalysisResultRepository;
import com.iread.backend.gaze.repository.GazeSessionRepository;
import com.iread.backend.exception.ResourceNotFoundException;
import com.iread.backend.training.app.service.DemoLearningClock;
import com.iread.backend.report.admin.dto.req.CreateReportRequest;
import com.iread.backend.report.admin.dto.res.*;
import com.iread.backend.report.admin.exception.ReportCreationException;
import com.iread.backend.report.domain.ReportEntity;
import com.iread.backend.report.repository.ReportRepository;
import com.iread.backend.student.domain.StudentEntity;
import com.iread.backend.student.repository.StudentRepository;
import com.iread.backend.student.service.ReadingMetricAggregationService;
import com.iread.backend.student.service.ReadingMetricSummary;
import com.iread.backend.test.domain.StudentTestEntity;
import com.iread.backend.test.domain.TestStatus;
import com.iread.backend.test.repository.StudentTestRepository;
import com.iread.backend.training.domain.TrainingEntity;
import com.iread.backend.training.domain.TrainingStatus;
import com.iread.backend.training.repository.TrainingRepository;
import com.iread.backend.wordattempt.repository.WordAttemptLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.dao.DataIntegrityViolationException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportService {

    /**
     * 훈련 하나가 누적 학습 시간에 기여할 수 있는 최대 시간(초).
     * 훈련 한 개는 5~10분 분량이라 30분을 넘는 값은 켜 둔 채 방치한 흔적으로 본다.
     */
    private static final long MAX_TRAINING_SECONDS = 30 * 60;
    private static final String SNAPSHOT_VERSION = "teacher-report-v2";

    private final ReportRepository reportRepository;
    private final StudentRepository studentRepository;
    private final TrainingRepository trainingRepository;
    private final StudentTestRepository testRepository;
    private final WordAttemptLogRepository wordAttemptLogRepository;
    private final GazeAnalysisResultRepository gazeAnalysisResultRepository;
    private final GazeSessionRepository gazeSessionRepository;
    private final ReadingMetricAggregationService readingMetricAggregationService;
    private final DemoLearningClock demoLearningClock;
    private final ObjectMapper objectMapper;

    @Transactional
    public CreateReportResponse createReport(Long teacherId, CreateReportRequest request) {
        if (request.startDate().isAfter(request.endDate())) {
            throw new IllegalArgumentException("시작일은 종료일보다 늦을 수 없습니다.");
        }
        StudentEntity student = studentRepository.findByIdAndTeacherId(request.studentId(), teacherId)
                .orElseThrow(() -> new ResourceNotFoundException("학생을 찾을 수 없습니다."));
        // 상한은 달력상 오늘이 아니라 아동의 학습 날짜다. 데모 치트로 학습일을 넘기면
        // 학습 날짜가 오늘보다 앞서고, 그날 기록도 보고서에 담을 수 있어야 한다.
        LocalDate learningDate = demoLearningClock.currentDate(request.studentId());
        if (request.endDate().isAfter(learningDate)) {
            throw new IllegalArgumentException("종료일은 아동의 학습 날짜 이후일 수 없습니다.");
        }

        LocalDateTime start = request.startDate().atStartOfDay();
        LocalDateTime endExclusive = request.endDate().plusDays(1).atStartOfDay();
        reportRepository.findByStudentIdAndStartDateAndEndDate(
                        request.studentId(),
                        start,
                        endExclusive.minusNanos(1)
                )
                .ifPresent(existing -> {
                    throw ReportCreationException.periodAlreadyExists(existing.getId());
                });
        List<TrainingEntity> trainings = trainingRepository
                .findAllByDailyCurriculumStudentIdAndStatusAndFinishedAtBetweenOrderByFinishedAtAsc(
                        request.studentId(), TrainingStatus.COMPLETED, start, endExclusive);
        long learningDayCount = trainings.stream()
                .map(TrainingEntity::getFinishedAt)
                .filter(Objects::nonNull)
                .map(LocalDateTime::toLocalDate)
                .distinct()
                .count();
        List<StudentTestEntity> tests = testRepository
                .findAllByTestCurriculumStudentIdAndStatusAndCreatedAtBetweenOrderByCreatedAtAsc(
                        request.studentId(), TestStatus.COMPLETED, start, endExclusive);
        if (learningDayCount < 1) {
            throw ReportCreationException.insufficientLearningDays(learningDayCount);
        }

        ReportSnapshot snapshot = buildSnapshot(
                request.studentId(), start, endExclusive, trainings, tests);
        ReportEntity report;
        try {
            report = reportRepository.saveAndFlush(new ReportEntity(
                    student,
                    request.startDate(),
                    request.endDate(),
                    writeJson(snapshot),
                    null
            ));
        } catch (DataIntegrityViolationException exception) {
            throw ReportCreationException.periodAlreadyExists(exception);
        }
        return new CreateReportResponse(report.getId(), report.getCreatedAt());
    }

    public ReportResponse getReport(Long teacherId, Long reportId) {
        ReportEntity report = findOwnedReport(teacherId, reportId);
        ReportSnapshot snapshot = readSnapshot(report.getSnapshotData());
        return new ReportResponse(
                report.getId(),
                report.getStudent().getId(),
                report.getStartDate(),
                report.getEndDate(),
                report.getCreatedAt(),
                snapshot,
                report.getTeacherMemo()
        );
    }

    public List<ReportListResponse> getReports(Long teacherId, Long studentId) {
        List<ReportEntity> reports;
        if (studentId == null) {
            reports = reportRepository.findAllByStudentTeacherIdOrderByCreatedAtDesc(teacherId);
        } else {
            studentRepository.findByIdAndTeacherId(studentId, teacherId)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "학생을 찾을 수 없습니다."
                    ));
            reports = reportRepository
                    .findAllByStudentIdAndStudentTeacherIdOrderByCreatedAtDesc(studentId, teacherId);
        }
        return reports.stream().map(this::toListResponse).toList();
    }

    @Transactional
    public UpdateReportMemoResponse updateReportMemo(Long teacherId, Long reportId, String teacherMemo) {
        ReportEntity report = findOwnedReport(teacherId, reportId);
        report.updateTeacherMemo(teacherMemo);
        return new UpdateReportMemoResponse(
                report.getId(),
                report.getTeacherMemo(),
                report.getCreatedAt()
        );
    }

    @Transactional
    public RefreshReportGazeTrendResponse refreshGazeTrend(Long teacherId, Long reportId) {
        ReportEntity report = findOwnedReport(teacherId, reportId);
        ReportSnapshot snapshot = readSnapshot(report.getSnapshotData());
        ReportSnapshot.GazeTrend gazeTrend = buildGazeTrend(
                report.getStudent().getId(),
                report.getStartDate().atStartOfDay(),
                report.getEndDate().plusDays(1).atStartOfDay()
        );
        ReportSnapshot updated = new ReportSnapshot(
                snapshot.snapshotVersion(),
                snapshot.calculationVersion(),
                snapshot.learningDays(),
                snapshot.totalTrainingTimeMinutes(),
                snapshot.completedTrainingCount(),
                snapshot.averageAccuracy(),
                snapshot.averageReadingSpeed(),
                snapshot.readingSpeedUnit(),
                snapshot.growthHistory(),
                snapshot.growthComparisonStatus(),
                snapshot.automaticAnalysis(),
                snapshot.areaAchievements(),
                snapshot.frequentlyIncorrectWords(),
                snapshot.improvedPatterns(),
                snapshot.persistentDifficultyPatterns(),
                snapshot.gazeAnalysis(),
                gazeTrend
        );
        report.updateSnapshotData(writeJson(updated));
        return new RefreshReportGazeTrendResponse(report.getId());
    }

    private ReportEntity findOwnedReport(Long teacherId, Long reportId) {
        return reportRepository.findByIdAndStudentTeacherId(reportId, teacherId)
                .orElseThrow(() -> new ResourceNotFoundException("리포트를 찾을 수 없습니다."));
    }

    private ReportListResponse toListResponse(ReportEntity report) {
        return new ReportListResponse(
                report.getId(),
                report.getStudent().getId(),
                report.getStudent().getName(),
                report.getStartDate(),
                report.getEndDate(),
                report.getTeacherMemo(),
                report.getCreatedAt()
        );
    }

    private ReportSnapshot buildSnapshot(
                                         Long studentId,
                                         LocalDateTime start,
                                         LocalDateTime endExclusive,
                                         List<TrainingEntity> trainings,
                                         List<StudentTestEntity> tests) {
        long learningDays = trainings.stream()
                .map(TrainingEntity::getFinishedAt)
                .filter(Objects::nonNull)
                .map(LocalDateTime::toLocalDate)
                .distinct()
                .count();
        // 훈련 하나에 담는 시간은 상한을 둔다. started_at~finished_at 은 벽시계라
        // 아이가 훈련을 켜 둔 채 나갔다 한참 뒤에 끝내면 그 시간이 모두 학습 시간으로
        // 잡혀 누적 시간이 비현실적으로 커진다(한 훈련이 열 시간으로 잡힌 사례가 있다).
        long totalMinutes = trainings.stream()
                .filter(t -> t.getStartedAt() != null && t.getFinishedAt() != null)
                .mapToLong(t -> Math.min(
                        MAX_TRAINING_SECONDS,
                        Math.max(0, Duration.between(t.getStartedAt(), t.getFinishedAt()).getSeconds())
                ))
                .sum() / 60;
        ReadingMetricSummary readingMetrics = readingMetricAggregationService.summarize(
                studentId,
                start.toLocalDate(),
                endExclusive.minusDays(1).toLocalDate()
        );
        List<ReportSnapshot.Growth> growth = buildGrowthHistory(readingMetrics, tests);
        ReportSnapshot.AutomaticAnalysis automaticAnalysis = buildAutomaticAnalysis(growth);

        Map<Long, TrainingEntity> bestTrainingByTemplate = trainings.stream()
                .filter(training -> training.getAccuracy() != null)
                .collect(Collectors.toMap(
                        training -> training.getTrainingTemplate().getId(),
                        training -> training,
                        BinaryOperator.maxBy(Comparator.comparing(TrainingEntity::getAccuracy))
                ));
        Map<String, List<BigDecimal>> achievementByUnit = bestTrainingByTemplate.values().stream()
                .collect(Collectors.groupingBy(
                        training -> training.getTrainingTemplate().getCurriculumUnit().getUnitName(),
                        Collectors.mapping(TrainingEntity::getAccuracy, Collectors.toList())));
        List<ReportSnapshot.AreaAchievement> achievements = achievementByUnit.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> new ReportSnapshot.AreaAchievement(entry.getKey(), average(entry.getValue())))
                .toList();

        List<ReportSnapshot.IncorrectWord> incorrectWords = wordAttemptLogRepository
                .findIncorrectWordStats(studentId, start, endExclusive).stream()
                .map(this::toIncorrectWord)
                .sorted(Comparator.comparing(ReportSnapshot.IncorrectWord::incorrectRate,
                                Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(ReportSnapshot.IncorrectWord::incorrectCount, Comparator.reverseOrder()))
                .limit(50)
                .toList();

        ReportSnapshot.GazeTrend gazeTrend = buildGazeTrend(studentId, start, endExclusive);
        return new ReportSnapshot(SNAPSHOT_VERSION, readingMetrics.calculationVersion(),
                learningDays, totalMinutes, trainings.size(), readingMetrics.averageAccuracy(),
                readingMetrics.averageReadingSpeed(), readingMetrics.readingSpeedUnit(), growth,
                automaticAnalysis.status(), automaticAnalysis, achievements, incorrectWords,
                List.of(), List.of(), null, gazeTrend);
    }

    private List<ReportSnapshot.Growth> buildGrowthHistory(
            ReadingMetricSummary readingMetrics,
            List<StudentTestEntity> tests
    ) {
        Map<LocalDate, List<BigDecimal>> pronunciationValues = new TreeMap<>();
        tests.stream()
                .filter(test -> test.getCreatedAt() != null)
                .forEach(test -> {
                    BigDecimal score = decimal(parseJson(test.getResult()).get("pronunciationScore"));
                    if (score != null) {
                        pronunciationValues
                                .computeIfAbsent(test.getCreatedAt().toLocalDate(), ignored -> new ArrayList<>())
                                .add(score);
                    }
                });
        Map<LocalDate, BigDecimal> pronunciationByDate = pronunciationValues.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> average(entry.getValue()),
                        (left, right) -> right,
                        TreeMap::new
                ));
        Map<LocalDate, ReadingMetricSummary.DailyMetric> readingByDate = readingMetrics.dailyMetrics()
                .stream()
                .collect(Collectors.toMap(
                        ReadingMetricSummary.DailyMetric::date,
                        Function.identity(),
                        (left, right) -> right,
                        TreeMap::new
                ));
        TreeSet<LocalDate> dates = new TreeSet<>(readingByDate.keySet());
        dates.addAll(pronunciationByDate.keySet());
        return dates.stream()
                .map(date -> {
                    ReadingMetricSummary.DailyMetric metric = readingByDate.get(date);
                    return new ReportSnapshot.Growth(
                            date,
                            metric == null ? null : metric.accuracy(),
                            metric == null ? null : metric.readingSpeed(),
                            pronunciationByDate.get(date)
                    );
                })
                .toList();
    }

    private ReportSnapshot.AutomaticAnalysis buildAutomaticAnalysis(
            List<ReportSnapshot.Growth> growth
    ) {
        List<ReportSnapshot.MetricChange> changes = java.util.stream.Stream.of(
                        metricChange(
                                ReportSnapshot.MetricType.ACCURACY,
                                growth,
                                ReportSnapshot.Growth::accuracy
                        ),
                        metricChange(
                                ReportSnapshot.MetricType.READING_SPEED,
                                growth,
                                ReportSnapshot.Growth::readingSpeed
                        ),
                        metricChange(
                                ReportSnapshot.MetricType.PRONUNCIATION_SCORE,
                                growth,
                                ReportSnapshot.Growth::pronunciationScore
                        )
                )
                .filter(Objects::nonNull)
                .toList();
        boolean hasMetric = growth.stream().anyMatch(point ->
                point.accuracy() != null
                        || point.readingSpeed() != null
                        || point.pronunciationScore() != null);
        ReportSnapshot.AnalysisStatus status = !changes.isEmpty()
                ? ReportSnapshot.AnalysisStatus.AVAILABLE
                : hasMetric
                    ? ReportSnapshot.AnalysisStatus.INSUFFICIENT_DATA
                    : ReportSnapshot.AnalysisStatus.NO_DATA;
        List<String> descriptions = switch (status) {
            case AVAILABLE -> changes.stream().map(this::describeMetricChange).toList();
            case INSUFFICIENT_DATA -> List.of("비교할 기록이 부족합니다.");
            case NO_DATA -> List.of("분석할 학습 기록이 없습니다.");
        };
        return new ReportSnapshot.AutomaticAnalysis(status, changes, descriptions);
    }

    private ReportSnapshot.MetricChange metricChange(
            ReportSnapshot.MetricType metric,
            List<ReportSnapshot.Growth> growth,
            Function<ReportSnapshot.Growth, BigDecimal> valueExtractor
    ) {
        List<BigDecimal> values = growth.stream()
                .map(valueExtractor)
                .filter(Objects::nonNull)
                .toList();
        if (values.size() < 2) {
            return null;
        }
        BigDecimal first = values.getFirst().setScale(2, RoundingMode.HALF_UP);
        BigDecimal latest = values.getLast().setScale(2, RoundingMode.HALF_UP);
        BigDecimal delta = latest.subtract(first).setScale(2, RoundingMode.HALF_UP);
        ReportSnapshot.ChangeDirection direction = delta.signum() > 0
                ? ReportSnapshot.ChangeDirection.INCREASED
                : delta.signum() < 0
                    ? ReportSnapshot.ChangeDirection.DECREASED
                    : ReportSnapshot.ChangeDirection.UNCHANGED;
        return new ReportSnapshot.MetricChange(metric, first, latest, delta, direction);
    }

    private String describeMetricChange(ReportSnapshot.MetricChange change) {
        String label = switch (change.metric()) {
            case ACCURACY -> "읽기 정확도";
            case READING_SPEED -> "읽기 속도";
            case PRONUNCIATION_SCORE -> "발음 점수";
        };
        String direction = switch (change.direction()) {
            case INCREASED -> "증가";
            case DECREASED -> "감소";
            case UNCHANGED -> "유지";
        };
        return "%s가 %s에서 %s로 %s했습니다.".formatted(
                label,
                change.first().toPlainString(),
                change.latest().toPlainString(),
                direction
        );
    }

    private ReportSnapshot.GazeTrend buildGazeTrend(
            Long studentId,
            LocalDateTime start,
            LocalDateTime endExclusive
    ) {
        return new ReportSnapshot.GazeTrend(
                LocalDateTime.now(),
                buildGazeSeries(studentId, GazeContentType.TRAINING, start, endExclusive),
                buildGazeSeries(studentId, GazeContentType.TEST, start, endExclusive)
        );
    }

    private ReportSnapshot.GazeSeries buildGazeSeries(
            Long studentId,
            GazeContentType contentType,
            LocalDateTime start,
            LocalDateTime endExclusive
    ) {
        List<ReportSnapshot.GazePoint> points = gazeAnalysisResultRepository
                .findAllByGazeSessionStudentIdAndGazeSessionContentTypeAndGazeSessionStartedAtGreaterThanEqualAndGazeSessionStartedAtLessThanOrderByCreatedAtAscIdAsc(
                        studentId, contentType, start, endExclusive)
                .stream()
                .map(result -> toGazePoint(result, contentType))
                .filter(Objects::nonNull)
                .toList();
        long failedSessionCount = gazeSessionRepository
                .countByStudentIdAndContentTypeAndStatusAndStartedAtGreaterThanEqualAndStartedAtLessThan(
                        studentId,
                        contentType,
                        GazeSessionStatus.FAILED,
                        start,
                        endExclusive
                );
        ReportSnapshot.GazeSeriesStatus status;
        if (!points.isEmpty()) {
            status = ReportSnapshot.GazeSeriesStatus.AVAILABLE;
        } else if (failedSessionCount > 0) {
            status = ReportSnapshot.GazeSeriesStatus.FAILED;
        } else {
            status = ReportSnapshot.GazeSeriesStatus.NO_DATA;
        }
        return new ReportSnapshot.GazeSeries(
                status,
                points.size() >= 2,
                points,
                buildGazeChanges(points),
                List.of(),
                failedSessionCount
        );
    }

    private ReportSnapshot.GazePoint toGazePoint(
            GazeAnalysisResultEntity result,
            GazeContentType contentType
    ) {
        GazeSessionEntity session = result.getGazeSession();
        Long sourceId;
        if (contentType == GazeContentType.TRAINING) {
            sourceId = session.getTraining() == null ? null : session.getTraining().getId();
        } else {
            sourceId = session.getTest() == null ? null : session.getTest().getId();
        }
        if (sourceId == null) {
            return null;
        }
        return new ReportSnapshot.GazePoint(
                result.getId(),
                session.getId(),
                contentType.name(),
                sourceId,
                result.getCreatedAt(),
                result.getTotalVisitedDuration(),
                result.getTotalVisitedCount(),
                result.getReverseReadCount(),
                result.getAvgVisitedDuration()
        );
    }

    private ReportSnapshot.GazeChanges buildGazeChanges(
            List<ReportSnapshot.GazePoint> points
    ) {
        if (points.size() < 2) {
            return null;
        }
        ReportSnapshot.GazePoint first = points.getFirst();
        ReportSnapshot.GazePoint latest = points.getLast();
        return new ReportSnapshot.GazeChanges(
                metricChange(first.totalVisitedDurationMs(), latest.totalVisitedDurationMs()),
                metricChange(first.totalVisitedCount(), latest.totalVisitedCount()),
                metricChange(first.reverseReadCount(), latest.reverseReadCount()),
                metricChange(first.avgVisitedDurationMs(), latest.avgVisitedDurationMs())
        );
    }

    private ReportSnapshot.GazeMetricChange metricChange(Integer first, Integer latest) {
        return new ReportSnapshot.GazeMetricChange(
                first,
                latest,
                first == null || latest == null ? null : latest - first
        );
    }

    private ReportSnapshot.IncorrectWord toIncorrectWord(
            WordAttemptLogRepository.IncorrectWordProjection stat
    ) {
        long attemptCount = stat.getAttemptCount() == null ? 0 : stat.getAttemptCount();
        long incorrectCount = stat.getIncorrectCount() == null ? 0 : stat.getIncorrectCount();
        BigDecimal rate = attemptCount == 0
                ? BigDecimal.ZERO.setScale(2)
                : BigDecimal.valueOf(incorrectCount * 100L)
                        .divide(BigDecimal.valueOf(attemptCount), 2, RoundingMode.HALF_UP);
        return new ReportSnapshot.IncorrectWord(
                stat.getWordId(),
                stat.getWordName(),
                Math.toIntExact(attemptCount),
                Math.toIntExact(incorrectCount),
                rate
        );
    }

    private BigDecimal average(List<BigDecimal> values) {
        List<BigDecimal> present = values.stream().filter(Objects::nonNull).toList();
        if (present.isEmpty()) return null;
        return present.stream().reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(present.size()), 2, RoundingMode.HALF_UP);
    }

    private JsonNode parseJson(String json) {
        if (json == null || json.isBlank()) return objectMapper.createObjectNode();
        try { return objectMapper.readTree(json); }
        catch (Exception exception) { throw new IllegalArgumentException("테스트 결과 JSON 형식이 올바르지 않습니다."); }
    }

    private BigDecimal decimal(JsonNode node) {
        return node == null || node.isNull() || !node.isNumber() ? null : node.decimalValue();
    }

    private String writeJson(ReportSnapshot snapshot) {
        try { return objectMapper.writeValueAsString(snapshot); }
        catch (Exception exception) { throw new IllegalStateException("리포트 스냅샷 저장에 실패했습니다."); }
    }

    private ReportSnapshot readSnapshot(String json) {
        try { return objectMapper.readValue(json, ReportSnapshot.class); }
        catch (Exception exception) { throw new IllegalStateException("리포트 스냅샷 조회에 실패했습니다."); }
    }
}
