package com.iread.backend.training.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class AccuracyIntegerConverterTest {
    private final AccuracyIntegerConverter converter = new AccuracyIntegerConverter();

    @Test
    void storesTrainingAccuracyAsRoundedInteger() {
        assertThat(converter.convertToDatabaseColumn(new BigDecimal("85.50"))).isEqualTo(86);
        assertThat(converter.convertToEntityAttribute(860)).isEqualByComparingTo("860");
    }

    @Test
    void preservesNullAccuracy() {
        assertThat(converter.convertToDatabaseColumn(null)).isNull();
        assertThat(converter.convertToEntityAttribute(null)).isNull();
    }
}
