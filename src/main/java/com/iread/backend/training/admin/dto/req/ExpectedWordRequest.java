package com.iread.backend.training.admin.dto.req;

import jakarta.validation.constraints.NotBlank;

public record ExpectedWordRequest(@NotBlank String wordName) {}
