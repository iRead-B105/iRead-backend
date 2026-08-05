package com.iread.backend.student.service;

import com.iread.backend.student.repository.StudentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReadingMetricAggregationServiceTest {

    @Mock StudentRepository studentRepository;

    private ReadingMetricAggregationService service;

    @BeforeEach
    void setUp() {
        service = new ReadingMetricAggregationService(studentRepository);
    }

    @Test
    void returnsAccuracySourceRecordsWithRawCountsAndContractMetadata() {
        LocalDate from = LocalDate.of(2026, 7, 1);
        LocalDate to = LocalDate.of(2026, 7, 31);
        var row = accuracyRow(101L, "받침 소리 구분", from.atTime(10, 0), 0L, 4L);
        when(studentRepository.findAccuracyRecords(
                10L,
                from.atStartOfDay(),
                to.plusDays(1).atStartOfDay()
        )).thenReturn(List.of(row));

        var result = service.getAccuracyRecords(10L, from, to);

        assertThat(result.from()).isEqualTo(from);
        assertThat(result.to()).isEqualTo(to);
        assertThat(result.unit()).isEqualTo("PERCENT");
        assertThat(result.calculationVersion()).isEqualTo("reading-metrics-v1");
        assertThat(result.records()).singleElement().satisfies(record -> {
            assertThat(record.sourceType()).isEqualTo("TRAINING");
            assertThat(record.sourceId()).isEqualTo(101L);
            assertThat(record.trainingName()).isEqualTo("받침 소리 구분");
            assertThat(record.correctAttemptCount()).isZero();
            assertThat(record.attemptCount()).isEqualTo(4L);
            assertThat(record.accuracyRate()).isEqualByComparingTo("0.00");
            assertThat(record.unit()).isEqualTo("PERCENT");
            assertThat(record.calculationVersion()).isEqualTo("reading-metrics-v1");
        });
    }

    @Test
    void aggregatesDailyAccuracyFromSummedNumeratorAndDenominator() {
        LocalDate date = LocalDate.now().minusDays(1);
        var firstRow = accuracyRow(101L, "첫소리 구분", date.atTime(10, 0), 1L, 1L);
        var secondRow = accuracyRow(102L, "받침 구분", date.atTime(11, 0), 9L, 99L);
        when(studentRepository.findAccuracyRecords(
                10L,
                date.minusDays(28).atStartOfDay(),
                date.plusDays(2).atStartOfDay()
        )).thenReturn(List.of(
                firstRow,
                secondRow
        ));

        var result = service.getAccuracyTrend(10L);

        assertThat(result.dailyAccuracy()).singleElement().satisfies(point -> {
            assertThat(point.date()).isEqualTo(date);
            assertThat(point.correctAttemptCount()).isEqualTo(10L);
            assertThat(point.attemptCount()).isEqualTo(100L);
            assertThat(point.accuracyRate()).isEqualByComparingTo("10.00");
        });
    }

    @Test
    void returnsOnlyVoiceReadingSpeedRecordsWithValidDuration() {
        LocalDate from = LocalDate.of(2026, 7, 1);
        LocalDate to = LocalDate.of(2026, 7, 31);
        var validRow = speedRow(201L, "짧은 이야기 읽기", from.atTime(10, 0), 60L, 60_000L, 40L, 40_000L);
        var invalidVoiceRow = speedRow(202L, "문장 따라 읽기", from.atTime(11, 0), 5L, 0L, 5L, 5_000L);
        when(studentRepository.findReadingSpeedTrainings(
                10L,
                from.atStartOfDay(),
                to.plusDays(1).atStartOfDay()
        )).thenReturn(List.of(
                validRow,
                invalidVoiceRow
        ));

        var result = service.getReadingSpeedRecords(10L, from, to);

        assertThat(result.unit()).isEqualTo("CORRECT_WORDS_PER_MINUTE");
        assertThat(result.calculationVersion()).isEqualTo("reading-metrics-v1");
        assertThat(result.records()).singleElement().satisfies(record -> {
            assertThat(record.sourceType()).isEqualTo("TRAINING");
            assertThat(record.sourceId()).isEqualTo(201L);
            assertThat(record.trainingName()).isEqualTo("짧은 이야기 읽기");
            assertThat(record.correctWordCount()).isEqualTo(60L);
            assertThat(record.measuredDurationMs()).isEqualTo(60_000L);
            assertThat(record.readingSpeed()).isEqualByComparingTo("60.00");
        });
    }

    @Test
    void aggregatesVoiceSpeedFromSourceCountsWithoutMixingGazeSpeed() {
        LocalDate date = LocalDate.of(2026, 7, 10);
        var firstRow = speedRow(201L, "첫 문장 읽기", date.atTime(10, 0), 10L, 10_000L, 10L, 20_000L);
        var secondRow = speedRow(202L, "둘째 문장 읽기", date.atTime(11, 0), 20L, 20_000L, 20L, 10_000L);
        when(studentRepository.findReadingSpeedTrainings(
                10L,
                date.atStartOfDay(),
                date.plusDays(1).atStartOfDay()
        )).thenReturn(List.of(
                firstRow,
                secondRow
        ));

        var result = service.getReadingSpeedTrend(10L, date, date);

        assertThat(result.points()).singleElement().satisfies(point -> {
            assertThat(point.voiceSpeed()).isEqualByComparingTo("60.00");
            assertThat(point.gazeSpeed()).isEqualByComparingTo("60.00");
            assertThat(point.correctWordCount()).isEqualTo(30L);
            assertThat(point.voiceDurationMs()).isEqualTo(30_000L);
            assertThat(point.gazeWordCount()).isEqualTo(30L);
            assertThat(point.gazeDurationMs()).isEqualTo(30_000L);
            assertThat(point.trainingCount()).isEqualTo(2);
        });
    }

    @Test
    void summarizesReportMetricsFromTheSameWeightedSourceRecords() {
        LocalDate from = LocalDate.of(2026, 7, 10);
        LocalDate to = LocalDate.of(2026, 7, 11);
        var firstAccuracy = accuracyRow(
                101L, "첫소리 구분", from.atTime(10, 0), 1L, 1L
        );
        var secondAccuracy = accuracyRow(
                102L, "받침 구분", to.atTime(10, 0), 9L, 99L
        );
        var firstSpeed = speedRow(
                101L, "첫소리 구분", from.atTime(10, 0), 10L, 10_000L, 99L, 1_000L
        );
        var secondSpeed = speedRow(
                102L, "받침 구분", to.atTime(10, 0), 30L, 20_000L, 99L, 1_000L
        );
        when(studentRepository.findAccuracyRecords(
                10L,
                from.atStartOfDay(),
                to.plusDays(1).atStartOfDay()
        )).thenReturn(List.of(
                firstAccuracy,
                secondAccuracy
        ));
        when(studentRepository.findReadingSpeedTrainings(
                10L,
                from.atStartOfDay(),
                to.plusDays(1).atStartOfDay()
        )).thenReturn(List.of(
                firstSpeed,
                secondSpeed
        ));

        var result = service.summarize(10L, from, to);

        assertThat(result.calculationVersion()).isEqualTo("reading-metrics-v1");
        assertThat(result.accuracyUnit()).isEqualTo("PERCENT");
        assertThat(result.readingSpeedUnit()).isEqualTo("CORRECT_WORDS_PER_MINUTE");
        assertThat(result.averageAccuracy()).isEqualByComparingTo("10.00");
        assertThat(result.averageReadingSpeed()).isEqualByComparingTo("80.00");
        assertThat(result.dailyMetrics())
                .extracting(
                        ReadingMetricSummary.DailyMetric::date,
                        ReadingMetricSummary.DailyMetric::accuracy,
                        ReadingMetricSummary.DailyMetric::readingSpeed
                )
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(
                                from,
                                new java.math.BigDecimal("100.00"),
                                new java.math.BigDecimal("60.00")
                        ),
                        org.assertj.core.groups.Tuple.tuple(
                                to,
                                new java.math.BigDecimal("9.09"),
                                new java.math.BigDecimal("90.00")
                        )
                );
    }

    @Test
    void rejectsReversedDateRangeForBothRecordTypes() {
        LocalDate from = LocalDate.of(2026, 7, 31);
        LocalDate to = LocalDate.of(2026, 7, 1);

        assertThatThrownBy(() -> service.getAccuracyRecords(10L, from, to))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("조회 시작일은 종료일보다 늦을 수 없습니다.");
        assertThatThrownBy(() -> service.getReadingSpeedRecords(10L, from, to))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("조회 시작일은 종료일보다 늦을 수 없습니다.");
    }

    private StudentRepository.AccuracyRecordProjection accuracyRow(
            Long sourceId,
            String trainingName,
            LocalDateTime measuredAt,
            Long correctAttemptCount,
            Long attemptCount
    ) {
        StudentRepository.AccuracyRecordProjection row = mock(
                StudentRepository.AccuracyRecordProjection.class
        );
        when(row.getSourceId()).thenReturn(sourceId);
        when(row.getTrainingName()).thenReturn(trainingName);
        when(row.getMeasuredAt()).thenReturn(measuredAt);
        when(row.getCorrectAttemptCount()).thenReturn(correctAttemptCount);
        when(row.getAttemptCount()).thenReturn(attemptCount);
        return row;
    }

    private StudentRepository.ReadingSpeedTrainingProjection speedRow(
            Long trainingId,
            String trainingName,
            LocalDateTime measuredAt,
            Long correctWordCount,
            Long voiceDurationMs,
            Long gazeWordCount,
            Long gazeDurationMs
    ) {
        StudentRepository.ReadingSpeedTrainingProjection row = mock(
                StudentRepository.ReadingSpeedTrainingProjection.class
        );
        org.mockito.Mockito.lenient().when(row.getTrainingId()).thenReturn(trainingId);
        org.mockito.Mockito.lenient().when(row.getTrainingName()).thenReturn(trainingName);
        org.mockito.Mockito.lenient().when(row.getMeasuredAt()).thenReturn(measuredAt);
        org.mockito.Mockito.lenient().when(row.getCorrectWordCount()).thenReturn(correctWordCount);
        org.mockito.Mockito.lenient().when(row.getVoiceDurationMs()).thenReturn(voiceDurationMs);
        org.mockito.Mockito.lenient().when(row.getGazeWordCount()).thenReturn(gazeWordCount);
        org.mockito.Mockito.lenient().when(row.getGazeDurationMs()).thenReturn(gazeDurationMs);
        return row;
    }
}
