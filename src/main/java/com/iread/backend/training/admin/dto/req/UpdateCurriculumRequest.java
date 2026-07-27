package com.iread.backend.training.admin.dto.req;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record UpdateCurriculumRequest(
        @NotEmpty @Size(max = 100) List<@NotNull Long> trainingTemplateIds
) {
}
