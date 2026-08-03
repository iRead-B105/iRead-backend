package com.iread.backend.typecast;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TypecastTtsRequest(
        @NotBlank @Size(max = 2000) String text,
        @DecimalMin("0.5") @DecimalMax("2.0") Double tempo
) {
    public double effectiveTempo() {
        return tempo == null ? 1.0 : tempo;
    }
}
