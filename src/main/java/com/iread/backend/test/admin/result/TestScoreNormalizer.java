package com.iread.backend.test.admin.result;

import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

@Component
public class TestScoreNormalizer {
    private static final BigDecimal TEN = BigDecimal.TEN;

    public BigDecimal fromStoredTotal(JsonNode node) {
        if (node == null || node.isNull() || !node.isNumber()) {
            return null;
        }
        return node.decimalValue().divide(TEN, 2, RoundingMode.HALF_UP);
    }

    public BigDecimal fromStoredTotal(Integer score) {
        if (score == null) {
            return null;
        }
        return BigDecimal.valueOf(score).divide(TEN, 2, RoundingMode.HALF_UP);
    }

    public BigDecimal fromPronunciation(JsonNode node) {
        if (node == null || node.isNull() || !node.isNumber()) {
            return null;
        }
        BigDecimal score = node.decimalValue();
        if (score.compareTo(BigDecimal.valueOf(100)) > 0) {
            score = score.divide(TEN, 2, RoundingMode.HALF_UP);
        }
        return score.setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal average(Collection<BigDecimal> scores) {
        List<BigDecimal> values = scores.stream()
                .filter(Objects::nonNull)
                .toList();
        if (values.isEmpty()) {
            return null;
        }
        BigDecimal sum = values.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return sum.divide(BigDecimal.valueOf(values.size()), 2, RoundingMode.HALF_UP);
    }
}
