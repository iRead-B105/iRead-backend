package com.iread.backend.student.service;

import com.iread.backend.student.dto.res.AccuracyRecordsResponse;
import com.iread.backend.student.dto.res.AccuracyTrendDataResponse;
import com.iread.backend.student.dto.res.AccuracyTrendResponse;
import com.iread.backend.student.dto.res.ReadingSpeedRecordsResponse;
import com.iread.backend.student.dto.res.ReadingSpeedTrendResponse;
import com.iread.backend.student.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReadingMetricAggregationService {

    public static final String CALCULATION_VERSION = "reading-metrics-v1";
    public static final String ACCURACY_UNIT = "PERCENT";
    public static final String READING_SPEED_UNIT = "CORRECT_WORDS_PER_MINUTE";

    private static final String SOURCE_TYPE_TRAINING = "TRAINING";
    private static final BigDecimal MILLIS_PER_MINUTE = BigDecimal.valueOf(60_000);
    private static final int DEFAULT_RANGE_DAYS = 30;

    private final StudentRepository studentRepository;

    public AccuracyRecordsResponse getAccuracyRecords(
            Long studentId,
            LocalDate from,
            LocalDate to
    ) {
        DateRange range = resolveRange(from, to);
        List<AccuracyRecordsResponse.Record> records = studentRepository.findAccuracyRecords(
                        studentId,
                        range.from().atStartOfDay(),
                        range.to().plusDays(1).atStartOfDay()
                ).stream()
                .map(row -> {
                    long correctAttemptCount = nonNegative(row.getCorrectAttemptCount());
                    long attemptCount = nonNegative(row.getAttemptCount());
                    return new AccuracyRecordsResponse.Record(
                            SOURCE_TYPE_TRAINING,
                            row.getSourceId(),
                            row.getMeasuredAt(),
                            correctAttemptCount,
                            attemptCount,
                            percentage(correctAttemptCount, attemptCount),
                            ACCURACY_UNIT,
                            CALCULATION_VERSION
                    );
                })
                .filter(record -> record.attemptCount() > 0)
                .toList();
        return new AccuracyRecordsResponse(
                range.from(),
                range.to(),
                ACCURACY_UNIT,
                CALCULATION_VERSION,
                records
        );
    }

    public AccuracyTrendDataResponse getAccuracyTrend(Long studentId) {
        AccuracyRecordsResponse source = getAccuracyRecords(studentId, null, null);
        Map<LocalDate, DailyAccuracy> daily = new LinkedHashMap<>();
        source.records().forEach(record -> daily
                .computeIfAbsent(record.measuredAt().toLocalDate(), ignored -> new DailyAccuracy())
                .add(record));
        List<AccuracyTrendResponse> points = daily.entrySet().stream()
                .map(entry -> entry.getValue().toPoint(entry.getKey()))
                .toList();
        return new AccuracyTrendDataResponse(ACCURACY_UNIT, CALCULATION_VERSION, points);
    }

    public ReadingSpeedRecordsResponse getReadingSpeedRecords(
            Long studentId,
            LocalDate from,
            LocalDate to
    ) {
        DateRange range = resolveRange(from, to);
        List<ReadingSpeedRecordsResponse.Record> records = loadReadingSpeedRows(studentId, range)
                .stream()
                .filter(row -> isPositive(row.getVoiceDurationMs()))
                .map(row -> {
                    long correctWordCount = nonNegative(row.getCorrectWordCount());
                    long measuredDurationMs = row.getVoiceDurationMs();
                    return new ReadingSpeedRecordsResponse.Record(
                            SOURCE_TYPE_TRAINING,
                            row.getTrainingId(),
                            row.getMeasuredAt(),
                            correctWordCount,
                            measuredDurationMs,
                            speed(correctWordCount, measuredDurationMs),
                            READING_SPEED_UNIT,
                            CALCULATION_VERSION
                    );
                })
                .toList();
        return new ReadingSpeedRecordsResponse(
                range.from(),
                range.to(),
                READING_SPEED_UNIT,
                CALCULATION_VERSION,
                records
        );
    }

    public ReadingSpeedTrendResponse getReadingSpeedTrend(
            Long studentId,
            LocalDate from,
            LocalDate to
    ) {
        DateRange range = resolveRange(from, to);
        Map<LocalDate, DailyReadingSpeed> daily = new LinkedHashMap<>();
        loadReadingSpeedRows(studentId, range).forEach(row -> {
            if (row.getMeasuredAt() == null) {
                return;
            }
            daily.computeIfAbsent(
                    row.getMeasuredAt().toLocalDate(),
                    ignored -> new DailyReadingSpeed()
            ).add(row);
        });
        List<ReadingSpeedTrendResponse.Point> points = daily.entrySet().stream()
                .map(entry -> entry.getValue().toPoint(entry.getKey()))
                .filter(point -> point.voiceSpeed() != null || point.gazeSpeed() != null)
                .toList();
        return new ReadingSpeedTrendResponse(
                range.from(),
                range.to(),
                READING_SPEED_UNIT,
                CALCULATION_VERSION,
                calculateChangeRate(points, ReadingSpeedTrendResponse.Point::voiceSpeed),
                calculateChangeRate(points, ReadingSpeedTrendResponse.Point::gazeSpeed),
                points
        );
    }

    private List<StudentRepository.ReadingSpeedTrainingProjection> loadReadingSpeedRows(
            Long studentId,
            DateRange range
    ) {
        return studentRepository.findReadingSpeedTrainings(
                studentId,
                range.from().atStartOfDay(),
                range.to().plusDays(1).atStartOfDay()
        );
    }

    private DateRange resolveRange(LocalDate from, LocalDate to) {
        LocalDate resolvedTo = to == null ? LocalDate.now() : to;
        LocalDate resolvedFrom = from == null
                ? resolvedTo.minusDays(DEFAULT_RANGE_DAYS - 1L)
                : from;
        if (resolvedFrom.isAfter(resolvedTo)) {
            throw new IllegalArgumentException("조회 시작일은 종료일보다 늦을 수 없습니다.");
        }
        return new DateRange(resolvedFrom, resolvedTo);
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

    private static BigDecimal percentage(long numerator, long denominator) {
        if (denominator <= 0) {
            return null;
        }
        return BigDecimal.valueOf(numerator)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(denominator), 2, RoundingMode.HALF_UP);
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

    private record DateRange(LocalDate from, LocalDate to) {
    }

    private static final class DailyAccuracy {
        private long correctAttemptCount;
        private long attemptCount;

        private void add(AccuracyRecordsResponse.Record record) {
            correctAttemptCount += record.correctAttemptCount();
            attemptCount += record.attemptCount();
        }

        private AccuracyTrendResponse toPoint(LocalDate date) {
            return new AccuracyTrendResponse(
                    date,
                    correctAttemptCount,
                    attemptCount,
                    percentage(correctAttemptCount, attemptCount)
            );
        }
    }

    private static final class DailyReadingSpeed {
        private long correctWordCount;
        private long voiceDurationMs;
        private long gazeWordCount;
        private long gazeDurationMs;
        private int trainingCount;

        private void add(StudentRepository.ReadingSpeedTrainingProjection row) {
            boolean validTraining = false;
            if (isPositive(row.getVoiceDurationMs())) {
                correctWordCount += nonNegative(row.getCorrectWordCount());
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
                    speed(correctWordCount, voiceDurationMs),
                    speed(gazeWordCount, gazeDurationMs),
                    voiceDurationMs > 0 ? correctWordCount : null,
                    voiceDurationMs > 0 ? voiceDurationMs : null,
                    gazeDurationMs > 0 ? gazeWordCount : null,
                    gazeDurationMs > 0 ? gazeDurationMs : null,
                    trainingCount
            );
        }
    }
}
