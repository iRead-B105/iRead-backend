package com.iread.backend.gaze.admin.controller;

import com.iread.backend.auth.annotation.CurrentTeacherId;
import com.iread.backend.gaze.app.dto.res.GazeAnalysisDetailResponse;
import com.iread.backend.gaze.app.dto.res.TestQuestionGazeAnalysisResponse;
import com.iread.backend.gaze.app.service.GazeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "시선 트래킹 관리", description = "관리자 앱 시선 분석 조회 API")
@RestController
@RequiredArgsConstructor
public class GazeAdminController {
    private final GazeService gazeService;

    @Operation(summary = "테스트 시선 분석 결과 조회")
    @GetMapping("/api/admin/test/{studentId}/{testId}/gaze-analysis")
    public GazeAnalysisDetailResponse getTestGazeAnalysis(
            @CurrentTeacherId Long teacherId,
            @PathVariable Long studentId,
            @PathVariable Long testId
    ) {
        return gazeService.getTestGazeAnalysis(teacherId, studentId, testId);
    }

    @Operation(summary = "Get gaze analysis for one test question")
    @GetMapping("/api/admin/test/{studentId}/{testId}/questions/{questionNo}/gaze-analysis")
    public TestQuestionGazeAnalysisResponse getTestQuestionGazeAnalysis(
            @CurrentTeacherId Long teacherId,
            @PathVariable Long studentId,
            @PathVariable Long testId,
            @PathVariable Integer questionNo
    ) {
        return gazeService.getTestQuestionGazeAnalysis(
                teacherId,
                studentId,
                testId,
                questionNo
        );
    }

    @Operation(summary = "훈련 시선 분석 결과 조회")
    @GetMapping("/api/admin/training/{studentId}/{trainingId}/gaze-analysis")
    public GazeAnalysisDetailResponse getTrainingGazeAnalysis(
            @CurrentTeacherId Long teacherId,
            @PathVariable Long studentId,
            @PathVariable Long trainingId
    ) {
        return gazeService.getTrainingGazeAnalysis(teacherId, studentId, trainingId);
    }
}
