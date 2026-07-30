package com.iread.backend.report.admin.service;

import com.iread.backend.gaze.domain.GazeContentType;
import com.iread.backend.gaze.domain.GazeSessionEntity;
import com.iread.backend.gaze.domain.GazeSessionStatus;
import com.iread.backend.gaze.repository.GazeSessionRepository;
import com.iread.backend.exception.ResourceNotFoundException;
import com.iread.backend.report.admin.dto.req.CreateReportRequest;
import com.iread.backend.report.admin.dto.res.*;
import com.iread.backend.report.admin.exception.ReportCreationException;
import com.iread.backend.report.domain.ReportEntity;
import com.iread.backend.report.repository.ReportRepository;
import com.iread.backend.student.domain.StudentEntity;
import com.iread.backend.student.repository.StudentRepository;
import com.iread.backend.test.domain.StudentTestEntity;
import com.iread.backend.test.domain.TestStatus;
import com.iread.backend.test.repository.StudentTestRepository;
import com.iread.backend.training.domain.TrainingEntity;
import com.iread.backend.training.domain.TrainingStatus;
import com.iread.backend.training.repository.TrainingRepository;
import com.iread.backend.wordattempt.domain.WordAttemptLogEntity;
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
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.BinaryOperator;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportService {
    private record GazeMetricSummary(
            int totalVisitedDuration,
            int totalVisitedCount,
            int reverseReadCount,
            int avgVisitedDuration
    ) {
    }

    private final ReportRepository reportRepository;
    private final StudentRepository studentRepository;
    private final TrainingRepository trainingRepository;
    private final StudentTestRepository testRepository;
    private final WordAttemptLogRepository wordAttemptLogRepository;
    private final GazeSessionRepository gazeSessionRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public CreateReportResponse createReport(Long teacherId, CreateReportRequest request) {
        if (request.startDate().isAfter(request.endDate())) {
            throw new IllegalArgumentException("시작일은 종료일보다 늦을 수 없습니다.");
        }
        StudentEntity student = studentRepository.findByIdAndTeacherId(request.studentId(), teacherId)
                .orElseThrow(() -> new ResourceNotFoundException("학생을 찾을 수 없습니다."));

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
        List<StudentTestEntity> tests = testRepository
                .findAllByTestCurriculumStudentIdAndStatusAndCreatedAtBetweenOrderByCreatedAtAsc(
                        request.studentId(), TestStatus.COMPLETED, start, endExclusive);
        if (trainings.isEmpty() && tests.isEmpty()) {
            throw ReportCreationException.dataNotFound();
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
                    request.teacherMemo()
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
                snapshot.learningDays(),
                snapshot.totalTrainingTimeMinutes(),
                snapshot.completedTrainingCount(),
                snapshot.averageAccuracy(),
                snapshot.averageReadingSpeed(),
                snapshot.readingSpeedUnit(),
                snapshot.growthHistory(),
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
        long learningDays = trainings.stream().map(t -> t.getFinishedAt().toLocalDate()).distinct().count();
        long totalMinutes = trainings.stream()
                .filter(t -> t.getStartedAt() != null && t.getFinishedAt() != null)
                .mapToLong(t -> Duration.between(t.getStartedAt(), t.getFinishedAt()).getSeconds()).sum() / 60;
        BigDecimal averageAccuracy = average(trainings.stream().map(TrainingEntity::getAccuracy).toList());

        List<ReportSnapshot.Growth> growth = tests.stream().map(test -> {
            JsonNode result = parseJson(test.getResult());
            return new ReportSnapshot.Growth(test.getCreatedAt().toLocalDate(), test.getAccuracy(),
                    decimal(result.get("readingSpeed")), decimal(result.get("pronunciationScore")));
        }).toList();
        BigDecimal averageReadingSpeed = average(growth.stream().map(ReportSnapshot.Growth::readingSpeed).toList());

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
        return new ReportSnapshot(learningDays, totalMinutes, trainings.size(), averageAccuracy,
                averageReadingSpeed, "CPM", growth, achievements, incorrectWords,
                List.of(), List.of(), null, gazeTrend);
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
        List<ReportSnapshot.GazePoint> points = gazeSessionRepository
                .findAllByStudentIdAndContentTypeAndStatusAndStartedAtGreaterThanEqualAndStartedAtLessThanOrderByStartedAtAscIdAsc(
                        studentId,
                        contentType,
                        GazeSessionStatus.COMPLETED,
                        start,
                        endExclusive
                )
                .stream()
                .map(session -> toGazePoint(session, contentType))
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
            GazeSessionEntity session,
            GazeContentType contentType
    ) {
        Long sourceId;
        List<WordAttemptLogEntity> attempts;
        if (contentType == GazeContentType.TRAINING) {
            sourceId = session.getTraining() == null ? null : session.getTraining().getId();
            attempts = sourceId == null
                    ? List.of()
                    : wordAttemptLogRepository.findAllByTrainingIdAndFinalAttemptTrueOrderByIdAsc(sourceId);
        } else {
            sourceId = session.getTest() == null ? null : session.getTest().getId();
            attempts = sourceId == null
                    ? List.of()
                    : wordAttemptLogRepository.findAllByTestIdAndFinalAttemptTrueOrderByIdAsc(sourceId);
        }
        if (sourceId == null) {
            return null;
        }
        GazeMetricSummary summary = summarizeGazeMetrics(attempts);
        if (summary == null) {
            return null;
        }
        return new ReportSnapshot.GazePoint(
                null,
                session.getId(),
                contentType.name(),
                sourceId,
                session.getEndedAt() == null ? session.getCreatedAt() : session.getEndedAt(),
                summary.totalVisitedDuration(),
                summary.totalVisitedCount(),
                summary.reverseReadCount(),
                summary.avgVisitedDuration()
        );
    }

    private GazeMetricSummary summarizeGazeMetrics(List<WordAttemptLogEntity> attempts) {
        List<WordAttemptLogEntity> gazeAttempts = attempts.stream()
                .filter(this::hasGazeMetric)
                .toList();
        if (gazeAttempts.isEmpty()) {
            return null;
        }
        int totalDuration = gazeAttempts.stream()
                .map(WordAttemptLogEntity::getFixationDurationMs)
                .filter(Objects::nonNull)
                .mapToInt(Integer::intValue)
                .sum();
        int totalCount = gazeAttempts.stream()
                .map(WordAttemptLogEntity::getFixationCount)
                .filter(Objects::nonNull)
                .mapToInt(Integer::intValue)
                .sum();
        int reverseReadCount = gazeAttempts.stream()
                .map(WordAttemptLogEntity::getRegressionCount)
                .filter(Objects::nonNull)
                .mapToInt(Integer::intValue)
                .sum();
        return new GazeMetricSummary(
                totalDuration,
                totalCount,
                reverseReadCount,
                totalCount == 0 ? 0 : totalDuration / totalCount
        );
    }

    private boolean hasGazeMetric(WordAttemptLogEntity attempt) {
        return attempt.getFixationDurationMs() != null
                || attempt.getFixationCount() != null
                || attempt.getRegressionCount() != null
                || attempt.getGazeStartOffsetMs() != null
                || attempt.getGazeEndOffsetMs() != null;
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
