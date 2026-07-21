package com.iread.backend.training.admin.dto.req;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record UpdateCurriculumRequest(@NotEmpty List<Long> trainingTemplateIds) {}
