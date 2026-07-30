package com.iread.backend.training.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class AccuracyIntegerConverterTest {
    private final AccuracyIntegerConverter converter = new AccuracyIntegerConverter();

    @Test
    void convertsApiPercentageToDatabaseThousandPointScale() {
        assertThat(converter.convertToDatabaseColumn(new BigDecimal("85.55"))).isEqualTo(856);
        assertThat(converter.convertToEntityAttribute(856)).isEqualByComparingTo("85.6");
    }

    @Test
    void preservesNullAccuracy() {
        assertThat(converter.convertToDatabaseColumn(null)).isNull();
        assertThat(converter.convertToEntityAttribute(null)).isNull();
    }
}
