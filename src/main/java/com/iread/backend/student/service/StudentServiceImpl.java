package com.iread.backend.student.service;

import com.iread.backend.global.storage.FileStorage;
import com.iread.backend.global.storage.StoredFile;
import com.iread.backend.exception.ResourceNotFoundException;
import com.iread.backend.student.domain.StudentEntity;
import com.iread.backend.student.domain.LearningEventType;
import com.iread.backend.student.dto.req.StudentRequest;
import com.iread.backend.student.dto.res.AccuracyTrendResponse;
import com.iread.backend.student.dto.res.LearningEventResponse;
import com.iread.backend.student.dto.res.LearningEventListItemResponse;
import com.iread.backend.student.dto.res.LearningEventListResponse;
import com.iread.backend.student.dto.res.LearningSummaryResponse;
import com.iread.backend.student.dto.res.ReadingSpeedTrendResponse;
import com.iread.backend.student.dto.res.StudentListResponse;
import com.iread.backend.student.dto.res.StudentListDataResponse;
import com.iread.backend.student.dto.res.StudentResponse;
import com.iread.backend.student.dto.res.StudentSummaryResponse;
import com.iread.backend.student.dto.res.TrainingHistoryResponse;
import com.iread.backend.student.repository.StudentRepository;
import com.iread.backend.teacher.domain.TeacherEntity;
import com.iread.backend.teacher.repository.TeacherRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Period;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StudentServiceImpl implements StudentService {

    private static final BigDecimal MILLIS_PER_MINUTE = BigDecimal.valueOf(60_000);

    private final StudentRepository studentRepository;
    private final TeacherRepository teacherRepository;
    private final FileStorage fileStorage;
    private final ObjectMapper objectMapper;

    @Override
    public StudentListDataResponse getStudents(
            Long teacherId,
            String keyword,
            Integer age,
            Integer recentDays,
            int page,
            int size
    ) {
        validateTeacher(teacherId);
        validateStudentListQuery(age, recentDays, page, size);

        Map<Long, StudentRepository.StudentLearningSummaryProjection> summaries =
                studentRepository.findLearningSummaries(teacherId).stream()
                        .collect(Collectors.toMap(
                                StudentRepository.StudentLearningSummaryProjection::getStudentId,
                                Function.identity()
                        ));

        LocalDate today = LocalDate.now();
        List<StudentListResponse> filtered =
                studentRepository.findAllByTeacherIdOrderByIdAsc(teacherId).stream()
                .map(student -> toListResponse(student, summaries.get(student.getId())))
                .filter(student -> matchesKeyword(student, keyword))
                .filter(student -> age == null || age.equals(student.age()))
                .filter(student -> isWithinRecentDays(student, recentDays, today))
                .toList();
        long totalElements = filtered.size();
        int totalPages = totalElements == 0
                ? 0 : Math.toIntExact((totalElements + size - 1L) / size);
        long start = (long) page * size;
        List<StudentListResponse> students = start >= totalElements
                ? List.of()
                : filtered.subList(
                        Math.toIntExact(start),
                        Math.min(Math.toIntExact(start + size), filtered.size())
                );

        return new StudentListDataResponse(
                List.copyOf(students),
                page,
                size,
                totalElements,
                totalPages
        );
    }

    @Override
    public StudentSummaryResponse getStudentSummary(Long teacherId) {
        validateTeacher(teacherId);
        return new StudentSummaryResponse(
                studentRepository.countByTeacherId(teacherId),
                studentRepository.countScheduledToday(teacherId)
        );
    }

    @Override
    public StudentResponse getStudent(Long teacherId, Long studentId) {
        return toResponse(findOwnedStudent(teacherId, studentId));
    }

    @Override
    @Transactional
    public Long createStudent(Long teacherId, StudentRequest request) {
        return createStudent(teacherId, request, null);
    }

    @Override
    @Transactional
    public Long createStudent(Long teacherId, StudentRequest request, MultipartFile imageFile) {
        TeacherEntity teacher = validateTeacher(teacherId);
        if (request.name() == null || request.name().isBlank()) {
            throw new IllegalArgumentException("학생 이름은 필수입니다.");
        }
        boolean uploaded = imageFile != null && !imageFile.isEmpty();
        StoredFile storedFile = uploaded ? fileStorage.store(imageFile) : null;
        String imageUrl = uploaded ? storedFile.url() : request.imageUrl();

        StudentEntity student = StudentEntity.builder()
                .teacher(teacher)
                .name(request.name())
                .birthday(request.birthday())
                .gender(request.gender())
                .school(request.school())
                .guardian(request.guardian())
                .guardianContact(request.guardianContact())
                .guardianEmail(request.guardianEmail())
                .address(normalizeAddress(request.address()))
                .imageUrl(imageUrl)
                .build();

        try {
            studentRepository.save(student);
            return student.getId();
        } catch (RuntimeException exception) {
            if (storedFile != null) fileStorage.delete(storedFile.storeFileName());
            throw exception;
        }
    }

    @Override
    @Transactional
    public void deleteStudent(Long teacherId, Long studentId) {
        StudentEntity student = findOwnedStudent(teacherId, studentId);
        studentRepository.deleteWordAttemptLogsByStudentId(studentId);
        studentRepository.deleteTrainingsByStudentId(studentId);
        studentRepository.deleteDailyCurriculumsByStudentId(studentId);
        studentRepository.delete(student);
    }

    @Override
    @Transactional
    public void updateStudent(Long teacherId, Long studentId, StudentRequest request) {
        updateStudent(teacherId, studentId, request, null);
    }

    @Override
    @Transactional
    public void updateStudent(
            Long teacherId,
            Long studentId,
            StudentRequest request,
            MultipartFile imageFile
    ) {
        StudentEntity student = findOwnedStudent(teacherId, studentId);

        boolean uploaded = imageFile != null && !imageFile.isEmpty();
        boolean updateImage = uploaded || request.imageUrl() != null;
        String oldImageUrl = student.getImageUrl();
        StoredFile storedFile = uploaded ? fileStorage.store(imageFile) : null;
        String imageUrl = uploaded ? storedFile.url() : request.imageUrl();
        student.update(
                request.name(),
                request.birthday(),
                request.gender(),
                request.school(),
                request.guardian(),
                request.guardianContact(),
                request.guardianEmail(),
                normalizeAddress(request.address()),
                imageUrl,
                updateImage
        );
        if (request.teacherMemo() != null) {
            student.updateTeacherMemo(request.teacherMemo());
        }

        if (uploaded && oldImageUrl != null) {
            fileStorage.delete(fileNameOf(oldImageUrl));
        }
    }

    @Override
    @Transactional
    public void updateTeacherMemo(Long teacherId, Long studentId, String teacherMemo) {
        StudentEntity student = findOwnedStudent(teacherId, studentId);
        student.updateTeacherMemo(teacherMemo);
    }

    @Override
    public List<AccuracyTrendResponse> getAccuracyTrend(Long teacherId, Long studentId) {
        findOwnedStudent(teacherId, studentId);
        return studentRepository.findAccuracyTrend(studentId).stream()
                .map(row -> new AccuracyTrendResponse(
                        row.getLearningDate(),
                        row.getAverageScore().divide(BigDecimal.TEN, 2, RoundingMode.HALF_UP)
                ))
                .toList();
    }

    @Override
    public List<TrainingHistoryResponse> getTrainingHistory(
            Long teacherId,
            Long studentId,
            LocalDate from,
            LocalDate to
    ) {
        findOwnedStudent(teacherId, studentId);
        validateDateRange(from, to);
        return studentRepository.findTrainingHistory(studentId, from, to).stream()
                .map(row -> new TrainingHistoryResponse(
                        row.getTrainingId(),
                        row.getLearningDate(),
                        row.getLearningType(),
                        row.getLearningCategory(),
                        row.getStartedAt(),
                        row.getFinishedAt(),
                        row.getAchievement(),
                        parseTrainingQuestions(row.getResult())
                ))
                .toList();
    }

    @Override
    public LearningSummaryResponse getLearningSummary(Long teacherId, Long studentId) {
        findOwnedStudent(teacherId, studentId);
        StudentRepository.LearningOverviewProjection overview =
                studentRepository.findLearningOverview(studentId);
        List<String> attentionReasons = attentionReasons(overview);
        return new LearningSummaryResponse(
                studentId,
                overview == null ? null : overview.getCurrentStage(),
                overview == null ? null : overview.getLastLearningAt(),
                attentionReasons.size(),
                attentionReasons
        );
    }

    @Override
    public LearningEventListResponse getRecentLearningEvents(
            Long teacherId,
            Long studentId,
            int limit
    ) {
        findOwnedStudent(teacherId, studentId);
        if (limit < 1 || limit > 20) {
            throw new IllegalArgumentException("limit은 1 이상 20 이하여야 합니다.");
        }
        StudentRepository.LearningOverviewProjection overview =
                studentRepository.findLearningOverview(studentId);
        List<String> attentionReasons = attentionReasons(overview);
        List<LearningEventListItemResponse> events =
                studentRepository.findRecentLearningEvents(studentId, limit).stream()
                        .map(event -> new LearningEventListItemResponse(
                                event.getEventId(),
                                event.getEventType(),
                                event.getOccurredAt(),
                                event.getEventId(),
                                event.getAccuracy(),
                                !attentionReasons.isEmpty(),
                                attentionReasons
                        ))
                        .toList();
        return new LearningEventListResponse(events);
    }

    @Override
    public LearningEventResponse getLearningEvent(
            Long teacherId,
            Long studentId,
            LearningEventType eventType,
            Long eventId
    ) {
        findOwnedStudent(teacherId, studentId);
        if (eventType == null) {
            throw new IllegalArgumentException("eventType은 필수입니다.");
        }
        StudentRepository.LearningEventProjection event =
                findLearningEvent(studentId, eventType, eventId)
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "학습 이벤트를 찾을 수 없습니다."
                        ));
        StudentRepository.LearningOverviewProjection overview =
                studentRepository.findLearningOverview(studentId);
        List<String> attentionReasons = attentionReasons(overview);
        StudentRepository.TrainingRecommendationProjection recommendation =
                studentRepository.findTrainingRecommendation(studentId).orElse(null);

        return new LearningEventResponse(
                event.getEventId(),
                eventType.apiValue(),
                event.getOccurredAt(),
                event.getEventId(),
                event.getAccuracy(),
                event.getRetryCount() == null ? 0 : event.getRetryCount(),
                splitProblemSegments(event.getProblemSegments()),
                !attentionReasons.isEmpty(),
                attentionReasons,
                recommendation == null ? null : recommendation.getTrainingTemplateId(),
                recommendation == null ? null : recommendation.getCurriculumUnitId(),
                recommendation == null ? null : recommendation.getCurriculumUnitName(),
                recommendationReason(recommendation),
                recommendation == null ? null : 10,
                recommendation == null ? null : 2
        );
    }

    private java.util.Optional<StudentRepository.LearningEventProjection> findLearningEvent(
            Long studentId,
            LearningEventType eventType,
            Long eventId
    ) {
        return switch (eventType) {
            case TEST -> studentRepository.findTestLearningEvent(studentId, eventId);
            case TRAINING -> studentRepository.findLearningEvent(studentId, eventId);
            case STORY -> studentRepository.findStoryLearningEvent(studentId, eventId);
            case GAZE -> studentRepository.findGazeLearningEvent(studentId, eventId);
        };
    }

    @Override
    public ReadingSpeedTrendResponse getReadingSpeedTrend(
            Long teacherId,
            Long studentId,
            LocalDate from,
            LocalDate to
    ) {
        findOwnedStudent(teacherId, studentId);

        LocalDate resolvedTo = to == null ? LocalDate.now() : to;
        LocalDate resolvedFrom = from == null ? resolvedTo.minusDays(29) : from;
        if (resolvedFrom.isAfter(resolvedTo)) {
            throw new IllegalArgumentException("조회 시작일은 종료일보다 늦을 수 없습니다.");
        }

        Map<LocalDate, DailyReadingSpeed> dailySpeeds = new LinkedHashMap<>();
        studentRepository.findReadingSpeedTrainings(
                        studentId,
                        resolvedFrom.atStartOfDay(),
                        resolvedTo.plusDays(1).atStartOfDay()
                ).forEach(row -> {
                    if (row.getLearningDate() == null) {
                        return;
                    }
                    dailySpeeds.computeIfAbsent(row.getLearningDate(), ignored -> new DailyReadingSpeed())
                            .add(row);
                });

        List<ReadingSpeedTrendResponse.Point> points = dailySpeeds.entrySet().stream()
                .map(entry -> entry.getValue().toPoint(entry.getKey()))
                .filter(point -> point.voiceSpeed() != null || point.gazeSpeed() != null)
                .toList();

        return new ReadingSpeedTrendResponse(
                resolvedFrom,
                resolvedTo,
                "WORDS_PER_MINUTE",
                calculateChangeRate(points, ReadingSpeedTrendResponse.Point::voiceSpeed),
                calculateChangeRate(points, ReadingSpeedTrendResponse.Point::gazeSpeed),
                points
        );
    }

    private List<TrainingHistoryResponse.QuestionResult> parseTrainingQuestions(String result) {
        if (result == null || result.isBlank()) {
            return List.of();
        }
        try {
            JsonNode questions = objectMapper.readTree(result).path("questions");
            if (!questions.isArray()) {
                return List.of();
            }
            List<TrainingHistoryResponse.QuestionResult> response = new java.util.ArrayList<>();
            for (JsonNode question : questions) {
                response.add(new TrainingHistoryResponse.QuestionResult(
                        question.path("questionNumber").asInt(response.size() + 1),
                        question.path("question").asText(null),
                        question.path("isCorrect").asBoolean(false),
                        question.path("selectedAnswer").asText(null),
                        question.path("correctAnswer").asText(null)
                ));
            }
            return response;
        } catch (Exception exception) {
            return List.of();
        }
    }

    private TeacherEntity validateTeacher(Long teacherId) {
        return teacherRepository.findById(teacherId)
                .orElseThrow(() -> new ResourceNotFoundException("교사를 찾을 수 없습니다."));
    }

    private void validateStudentListQuery(
            Integer age,
            Integer recentDays,
            int page,
            int size
    ) {
        if (age != null && (age < 6 || age > 12)) {
            throw new IllegalArgumentException("age는 6 이상 12 이하여야 합니다.");
        }
        if (recentDays != null && recentDays != 7 && recentDays != 30) {
            throw new IllegalArgumentException("recentDays는 7 또는 30이어야 합니다.");
        }
        if (page < 0) {
            throw new IllegalArgumentException("page는 0 이상이어야 합니다.");
        }
        if (size < 1 || size > 100) {
            throw new IllegalArgumentException("size는 1 이상 100 이하여야 합니다.");
        }
    }

    private void validateDateRange(LocalDate from, LocalDate to) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new IllegalArgumentException("조회 시작일은 종료일보다 늦을 수 없습니다.");
        }
    }

    private boolean matchesKeyword(StudentListResponse student, String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return true;
        }
        String normalized = keyword.trim().toLowerCase();
        return student.name().toLowerCase().contains(normalized)
                || student.school() != null
                && student.school().toLowerCase().contains(normalized);
    }

    private boolean isWithinRecentDays(
            StudentListResponse student,
            Integer recentDays,
            LocalDate today
    ) {
        if (recentDays == null) {
            return true;
        }
        LocalDate learnedAt = student.recentLearningDate();
        return learnedAt != null
                && !learnedAt.isAfter(today)
                && learnedAt.isAfter(today.minusDays(recentDays));
    }

    private List<String> attentionReasons(
            StudentRepository.LearningOverviewProjection overview
    ) {
        if (overview == null || overview.getLastLearningAt() == null) {
            return List.of();
        }
        List<String> reasons = new ArrayList<>();
        long completedCount = overview.getRecentCompletedCount() == null
                ? 0 : overview.getRecentCompletedCount();
        if (completedCount >= 3
                && overview.getRecentAverageAccuracy() != null
                && overview.getRecentAverageAccuracy().compareTo(BigDecimal.valueOf(70)) < 0) {
            reasons.add("LOW_ACCURACY");
        }
        if (overview.getRecentGazeFailureCount() != null
                && overview.getRecentGazeFailureCount() > 0) {
            reasons.add("GAZE_ANALYSIS_FAILED");
        }
        if (overview.getLastLearningAt().isBefore(LocalDateTime.now().minusDays(14))) {
            reasons.add("INACTIVE");
        }
        return List.copyOf(reasons);
    }

    private List<String> splitProblemSegments(String problemSegments) {
        if (problemSegments == null || problemSegments.isBlank()) {
            return List.of();
        }
        return Arrays.stream(problemSegments.split("\\|\\|\\|"))
                .filter(segment -> !segment.isBlank())
                .toList();
    }

    private String recommendationReason(
            StudentRepository.TrainingRecommendationProjection recommendation
    ) {
        if (recommendation == null) {
            return null;
        }
        if (recommendation.getAverageAccuracy() != null) {
            return "최근 6주 평균 정확도가 가장 낮은 영역의 다음 미완료 훈련";
        }
        return "현재 학습 단계에서 아직 완료하지 않은 다음 훈련";
    }

    private StudentEntity findOwnedStudent(Long teacherId, Long studentId) {
        return studentRepository.findByIdAndTeacherId(studentId, teacherId)
                .orElseThrow(() -> new ResourceNotFoundException("학생을 찾을 수 없습니다."));
    }

    private String fileNameOf(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) return null;
        int slash = imageUrl.lastIndexOf('/');
        return slash < 0 ? imageUrl : imageUrl.substring(slash + 1);
    }

    private String normalizeAddress(Object address) {
        if (address == null) {
            return null;
        }
        String normalized;
        if (address instanceof String text) {
            normalized = text.trim();
        } else {
            try {
                normalized = objectMapper.writeValueAsString(address);
            } catch (Exception exception) {
                throw new IllegalArgumentException("주소 형식이 올바르지 않습니다.");
            }
        }
        if (normalized.length() > 100) {
            throw new IllegalArgumentException("주소는 100자 이하여야 합니다.");
        }
        return normalized.isBlank() ? null : normalized;
    }

    private Object contractAddress(String address) {
        if (address == null || address.isBlank()) {
            return List.of();
        }
        if (address.startsWith("[")) {
            try {
                return objectMapper.readTree(address);
            } catch (Exception exception) {
                throw new IllegalStateException("저장된 주소 형식이 올바르지 않습니다.");
            }
        }
        return List.of(Map.of("value", address));
    }

    private StudentListResponse toListResponse(
            StudentEntity student,
            StudentRepository.StudentLearningSummaryProjection summary
    ) {
        Integer age = student.getBirthday() == null
                ? null
                : Period.between(student.getBirthday(), LocalDate.now()).getYears();
        long weeklyScheduledCount = summary == null || summary.getWeeklyScheduledCount() == null
                ? 0 : summary.getWeeklyScheduledCount();
        long weeklyCompletedCount = summary == null || summary.getWeeklyCompletedCount() == null
                ? 0 : summary.getWeeklyCompletedCount();
        Integer weeklyParticipationRate = weeklyScheduledCount == 0
                ? null
                : Math.toIntExact(Math.round(weeklyCompletedCount * 100.0 / weeklyScheduledCount));

        return new StudentListResponse(
                student.getId(),
                student.getName(),
                student.getSchool(),
                age,
                student.getImageUrl(),
                summary == null ? null : summary.getRecentTrainingName(),
                summary == null || summary.getRecentFinishedAt() == null
                        ? null : summary.getRecentFinishedAt().toLocalDate(),
                weeklyScheduledCount,
                weeklyCompletedCount,
                weeklyParticipationRate,
                summary == null || summary.getTotalLearningMinutes() == null
                        ? 0L : summary.getTotalLearningMinutes()
        );
    }

    private StudentResponse toResponse(StudentEntity student) {
        return new StudentResponse(
                student.getId(),
                student.getName(),
                student.getBirthday(),
                student.getGender(),
                student.getSchool(),
                student.getGuardian(),
                student.getGuardianContact(),
                student.getGuardianEmail(),
                contractAddress(student.getAddress()),
                student.getImageUrl(),
                student.getTeacherMemo(),
                student.getCreatedAt()
        );
    }

    private BigDecimal calculateChangeRate(
            List<ReadingSpeedTrendResponse.Point> points,
            Function<ReadingSpeedTrendResponse.Point, BigDecimal> valueExtractor
    ) {
        List<BigDecimal> values = points.stream()
                .map(valueExtractor)
                .filter(value -> value != null)
                .toList();
        if (values.isEmpty() || values.getFirst().signum() == 0) {
            return null;
        }
        if (values.size() == 1) {
            return BigDecimal.ZERO.setScale(2);
        }
        return values.getLast()
                .subtract(values.getFirst())
                .multiply(BigDecimal.valueOf(100))
                .divide(values.getFirst(), 2, RoundingMode.HALF_UP);
    }

    private static final class DailyReadingSpeed {
        private long voiceWordCount;
        private long voiceDurationMs;
        private long gazeWordCount;
        private long gazeDurationMs;
        private int trainingCount;

        private void add(StudentRepository.ReadingSpeedTrainingProjection row) {
            boolean validTraining = false;
            if (isPositive(row.getVoiceDurationMs())) {
                voiceWordCount += nonNegative(row.getVoiceWordCount());
                voiceDurationMs += row.getVoiceDurationMs();
                validTraining = true;
            }
            if (isPositive(row.getGazeDurationMs())) {
                gazeWordCount += nonNegative(row.getGazeWordCount());
                gazeDurationMs += row.getGazeDurationMs();
                validTraining = true;
            }
            if (validTraining) {
                trainingCount++;
            }
        }

        private ReadingSpeedTrendResponse.Point toPoint(LocalDate date) {
            return new ReadingSpeedTrendResponse.Point(
                    date,
                    speed(voiceWordCount, voiceDurationMs),
                    speed(gazeWordCount, gazeDurationMs),
                    voiceDurationMs > 0 ? voiceWordCount : null,
                    gazeDurationMs > 0 ? gazeWordCount : null,
                    voiceDurationMs > 0 ? voiceDurationMs : null,
                    gazeDurationMs > 0 ? gazeDurationMs : null,
                    trainingCount
            );
        }

        private static BigDecimal speed(long wordCount, long durationMs) {
            if (durationMs <= 0) {
                return null;
            }
            return BigDecimal.valueOf(wordCount)
                    .multiply(MILLIS_PER_MINUTE)
                    .divide(BigDecimal.valueOf(durationMs), 2, RoundingMode.HALF_UP);
        }

        private static boolean isPositive(Long value) {
            return value != null && value > 0;
        }

        private static long nonNegative(Long value) {
            return value == null ? 0 : Math.max(0, value);
        }
    }
}
