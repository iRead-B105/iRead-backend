package com.iread.backend.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Collection;
import java.util.Map;

public class ValidAddressValidator implements ConstraintValidator<ValidAddress, Object> {
    private static final int MAX_LENGTH = 100;
    private static final int MAX_PARTS = 5;

    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        if (value == null) return true;
        if (value instanceof String text) return safe(text) && text.trim().length() <= MAX_LENGTH;
        if (!(value instanceof Collection<?> parts) || parts.size() > MAX_PARTS) return false;

        int totalLength = 0;
        for (Object part : parts) {
            if (!(part instanceof Map<?, ?> map)
                    || map.size() != 1
                    || !(map.get("value") instanceof String text)
                    || !safe(text)) {
                return false;
            }
            totalLength += text.trim().length();
            if (totalLength > MAX_LENGTH) return false;
        }
        return true;
    }

    private boolean safe(String value) {
        for (int index = 0; index < value.length(); index++) {
            char current = value.charAt(index);
            if (Character.isHighSurrogate(current)) {
                if (index + 1 >= value.length() || !Character.isLowSurrogate(value.charAt(index + 1))) {
                    return false;
                }
                index++;
                continue;
            }
            if (Character.isLowSurrogate(current) || Character.isISOControl(current)) return false;
        }
        return true;
    }
}
