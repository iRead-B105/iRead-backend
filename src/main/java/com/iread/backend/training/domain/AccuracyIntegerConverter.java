package com.iread.backend.training.domain;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Converter
public class AccuracyIntegerConverter implements AttributeConverter<BigDecimal, Integer> {
    @Override
    public Integer convertToDatabaseColumn(BigDecimal attribute) {
        return attribute == null ? null : attribute.setScale(0, RoundingMode.HALF_UP).intValueExact();
    }

    @Override
    public BigDecimal convertToEntityAttribute(Integer dbData) {
        return dbData == null ? null : BigDecimal.valueOf(dbData);
    }
}
