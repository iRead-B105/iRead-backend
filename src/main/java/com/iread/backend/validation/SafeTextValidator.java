package com.iread.backend.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class SafeTextValidator implements ConstraintValidator<SafeText, String> {
    private boolean allowLineBreaks;

    @Override
    public void initialize(SafeText annotation) {
        allowLineBreaks = annotation.allowLineBreaks();
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) return true;
        for (int index = 0; index < value.length(); index++) {
            char current = value.charAt(index);
            if (Character.isHighSurrogate(current)) {
                if (index + 1 >= value.length() || !Character.isLowSurrogate(value.charAt(index + 1))) {
                    return false;
                }
                index++;
                continue;
            }
            if (Character.isLowSurrogate(current)) return false;
            if (Character.isISOControl(current)
                    && !(allowLineBreaks && (current == '\n' || current == '\r' || current == '\t'))) {
                return false;
            }
        }
        return true;
    }
}
