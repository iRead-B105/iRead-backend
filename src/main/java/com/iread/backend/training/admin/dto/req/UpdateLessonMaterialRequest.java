package com.iread.backend.training.admin.dto.req;

import com.iread.backend.validation.SafeText;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import tools.jackson.databind.JsonNode;

import java.util.List;

public record UpdateLessonMaterialRequest(
        @Min(0) int revision,
        @NotNull @Size(min = 1, max = 5) List<@Valid Material> materials
) {
    public UpdateLessonMaterialRequest {
        materials = materials == null ? null : List.copyOf(materials);
    }

    public record Material(
            @Min(1) @Max(5) int questionNo,
            @NotBlank @Size(max = 64) @SafeText String questionType,
            JsonNode presentation,
            @NotNull JsonNode content,
            @NotNull JsonNode answer
    ) {
        public Material {
            presentation = presentation == null ? null : presentation.deepCopy();
            content = content == null ? null : content.deepCopy();
            answer = answer == null ? null : answer.deepCopy();
        }
    }
}
