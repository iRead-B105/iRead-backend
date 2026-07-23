package com.iread.backend.report.admin.service;

import com.iread.backend.report.admin.dto.req.CreateReportRequest;
import com.iread.backend.report.admin.dto.res.*;
import com.iread.backend.report.domain.ReportEntity;
import com.iread.backend.report.domain.StudentWordStatEntity;
import com.iread.backend.report.repository.ReportRepository;
import com.iread.backend.report.repository.StudentWordStatRepository;
import com.iread.backend.student.domain.StudentEntity;
import com.iread.backend.student.repository.StudentRepository;
import com.iread.backend.test.domain.StudentTestEntity;
import com.iread.backend.test.domain.TestStatus;
import com.iread.backend.test.repository.StudentTestRepository;
import com.iread.backend.training.domain.StudentStudyProgressEntity;
import com.iread.backend.training.domain.TrainingEntity;
import com.iread.backend.training.domain.TrainingStatus;
import com.iread.backend.training.repository.StudentStudyProgressRepository;
import com.iread.backend.training.repository.TrainingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportService {
    private final ReportRepository reportRepository;
    private final StudentRepository studentRepository;
    private final TrainingRepository trainingRepository;
    private final StudentTestRepository testRepository;
    private final StudentStudyProgressRepository progressRepository;
    private final StudentWordStatRepository wordStatRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public CreateReportResponse createReport(Long teacherId, CreateReportRequest request) {
        if (request.startDate().isAfter(request.endDate())) {
            throw new IllegalArgumentException("시작일은 종료일보다 늦을 수 없습니다.");
        }
        StudentEntity student = studentRepository.findByIdAndTeacherId(request.studentId(), teacherId)
                .orElseThrow(() -> new IllegalArgumentException("학생을 찾을 수 없습니다."));

        LocalDateTime start = request.startDate().atStartOfDay();
        LocalDateTime endExclusive = request.endDate().plusDays(1).atStartOfDay();
        List<TrainingEntity> trainings = trainingRepository
                .findAllByDailyCurriculumStudentIdAndStatusAndFinishedAtBetweenOrderByFinishedAtAsc(
                        request.studentId(), TrainingStatus.COMPLETED, start, endExclusive);
        List<StudentTestEntity> tests = testRepository
                .findAllByStudentIdAndStatusAndCreatedAtBetweenOrderByCreatedAtAsc(
                        request.studentId(), TestStatus.COMPLETED, start, endExclusive);

        ReportSnapshot snapshot = buildSnapshot(request.studentId(), trainings, tests);
        ReportEntity report = reportRepository.save(new ReportEntity(
                student, request.startDate(), request.endDate(), writeJson(snapshot), request.teacherMemo()));
        return new CreateReportResponse(report.getId());
    }

    public ReportResponse getReport(Long teacherId, Long reportId) {
        ReportEntity report = findOwnedReport(teacherId, reportId);
        return new ReportResponse(report.getId(), report.getStudent().getId(), report.getStartDate(),
                report.getEndDate(), readSnapshot(report.getSnapshotData()), report.getTeacherMemo());
    }

    public List<ReportListResponse> getReports(Long teacherId, Long studentId) {
        List<ReportEntity> reports;
        if (studentId == null) {
            reports = reportRepository.findAllByStudentTeacherIdOrderByCreatedAtDesc(teacherId);
        } else {
            studentRepository.findByIdAndTeacherId(studentId, teacherId)
                    .orElseThrow(() -> new IllegalArgumentException("학생을 찾을 수 없습니다."));
            reports = reportRepository
                    .findAllByStudentIdAndStudentTeacherIdOrderByCreatedAtDesc(studentId, teacherId);
        }
        return reports.stream().map(this::toListResponse).toList();
    }

    @Transactional
    public void updateReportMemo(Long teacherId, Long reportId, String teacherMemo) {
        findOwnedReport(teacherId, reportId).updateTeacherMemo(teacherMemo);
    }

    @Transactional
    public void deleteReport(Long teacherId, Long reportId) {
        reportRepository.delete(findOwnedReport(teacherId, reportId));
    }

    private ReportEntity findOwnedReport(Long teacherId, Long reportId) {
        return reportRepository.findByIdAndStudentTeacherId(reportId, teacherId)
                .orElseThrow(() -> new IllegalArgumentException("리포트를 찾을 수 없습니다."));
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

    private ReportSnapshot buildSnapshot(Long studentId, List<TrainingEntity> trainings,
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

        Map<String, List<BigDecimal>> achievementByUnit = progressRepository.findAllByStudentId(studentId).stream()
                .filter(progress -> progress.getAchievement() != null)
                .collect(Collectors.groupingBy(
                        progress -> progress.getTrainingTemplate().getCurriculumUnit().getUnitName(),
                        Collectors.mapping(StudentStudyProgressEntity::getAchievement, Collectors.toList())));
        List<ReportSnapshot.AreaAchievement> achievements = achievementByUnit.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> new ReportSnapshot.AreaAchievement(entry.getKey(), average(entry.getValue())))
                .toList();

        List<ReportSnapshot.IncorrectWord> incorrectWords = wordStatRepository.findAllByStudentId(studentId).stream()
                .map(this::toIncorrectWord)
                .sorted(Comparator.comparing(ReportSnapshot.IncorrectWord::incorrectRate,
                                Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(ReportSnapshot.IncorrectWord::incorrectCount, Comparator.reverseOrder()))
                .limit(50)
                .toList();

        return new ReportSnapshot(learningDays, totalMinutes, trainings.size(), averageAccuracy,
                averageReadingSpeed, "CPM", growth, achievements, incorrectWords, List.of(), List.of());
    }

    private ReportSnapshot.IncorrectWord toIncorrectWord(StudentWordStatEntity stat) {
        BigDecimal rate = stat.getAttemptCount() == null || stat.getAttemptCount() == 0
                ? BigDecimal.ZERO.setScale(2)
                : BigDecimal.valueOf(stat.getFailedCount() * 100L)
                        .divide(BigDecimal.valueOf(stat.getAttemptCount()), 2, RoundingMode.HALF_UP);
        return new ReportSnapshot.IncorrectWord(stat.getWord().getId(), stat.getWord().getContent(),
                stat.getAttemptCount(), stat.getFailedCount(), rate);
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
