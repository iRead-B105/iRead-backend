package com.iread.backend.training.admin.controller;

import com.iread.backend.auth.annotation.CurrentTeacherId;
import com.iread.backend.training.admin.dto.res.AiCurriculumRecommendationResponse;
import com.iread.backend.training.admin.service.AiCurriculumRecommendationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "훈련 관리", description = "관리자 커리큘럼 및 훈련 관리 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/training")
public class AiCurriculumRecommendationController {

    private final AiCurriculumRecommendationService recommendationService;

    @Operation(summary = "학생 읽기 프로필 기반 AI 커리큘럼 추천")
    @PostMapping("/{studentId}/ai-recommendation")
    public AiCurriculumRecommendationResponse recommend(
            @CurrentTeacherId Long teacherId,
            @PathVariable Long studentId
    ) {
        return recommendationService.recommend(teacherId, studentId);
    }
}
