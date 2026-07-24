package com.iread.backend.student.service;

import com.iread.backend.global.storage.FileStorage;
import com.iread.backend.global.storage.StoredFile;
import com.iread.backend.student.domain.StudentEntity;
import com.iread.backend.student.dto.req.StudentRequest;
import com.iread.backend.student.dto.res.AccuracyTrendResponse;
import com.iread.backend.student.dto.res.ReadingSpeedTrendResponse;
import com.iread.backend.student.dto.res.StudentListResponse;
import com.iread.backend.student.dto.res.StudentResponse;
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
    public List<StudentListResponse> getStudents(Long teacherId) {
        validateTeacher(teacherId);

        Map<Long, StudentRepository.StudentLearningSummaryProjection> summaries =
                studentRepository.findLearningSummaries(teacherId).stream()
                        .collect(Collectors.toMap(
                                StudentRepository.StudentLearningSummaryProjection::getStudentId,
                                Function.identity()
                        ));

        return studentRepository.findAllByTeacherIdOrderByIdAsc(teacherId).stream()
                .map(student -> toListResponse(student, summaries.get(student.getId())))
                .toList();
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
                .address(request.address())
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
                request.address(),
                imageUrl,
                updateImage
        );

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
                .map(row -> new AccuracyTrendResponse(row.getLearningDate(), row.getAccuracy()))
                .toList();
    }

    @Override
    public List<TrainingHistoryResponse> getTrainingHistory(Long teacherId, Long studentId) {
        findOwnedStudent(teacherId, studentId);
        return studentRepository.findTrainingHistory(studentId).stream()
                .map(row -> new TrainingHistoryResponse(
                        row.getTrainingId(),
                        row.getLearningDate(),
                        row.getLearningType(),
                        row.getStartedAt(),
                        row.getFinishedAt(),
                        row.getAchievement(),
                        parseTrainingQuestions(row.getResult())
                ))
                .toList();
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
                .orElseThrow(() -> new IllegalArgumentException("교사를 찾을 수 없습니다."));
    }

    private StudentEntity findOwnedStudent(Long teacherId, Long studentId) {
        return studentRepository.findByIdAndTeacherId(studentId, teacherId)
                .orElseThrow(() -> new IllegalArgumentException("학생을 찾을 수 없습니다."));
    }

    private String fileNameOf(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) return null;
        int slash = imageUrl.lastIndexOf('/');
        return slash < 0 ? imageUrl : imageUrl.substring(slash + 1);
    }

    private StudentListResponse toListResponse(
            StudentEntity student,
            StudentRepository.StudentLearningSummaryProjection summary
    ) {
        Integer age = student.getBirthday() == null
                ? null
                : Period.between(student.getBirthday(), LocalDate.now()).getYears();

        return new StudentListResponse(
                student.getId(),
                student.getName(),
                age,
                summary == null || summary.getRecentFinishedAt() == null
                        ? null : summary.getRecentFinishedAt().toLocalDate(),
                summary == null ? 0L : summary.getTotalLearningMinutes(),
                summary == null ? null : summary.getRecentTrainingName()
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
                student.getAddress(),
                student.getImageUrl(),
                student.getTeacherMemo()
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
