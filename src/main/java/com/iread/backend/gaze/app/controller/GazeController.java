package com.iread.backend.gaze.app.controller;

import com.iread.backend.auth.annotation.CurrentTeacherId;
import com.iread.backend.gaze.app.dto.req.EndGazeSessionRequest;
import com.iread.backend.gaze.app.dto.req.FailGazeSessionRequest;
import com.iread.backend.gaze.app.dto.req.GazeAnalysisResultRequest;
import com.iread.backend.gaze.app.dto.req.StartGazeSessionRequest;
import com.iread.backend.gaze.app.dto.res.*;
import com.iread.backend.gaze.app.service.GazeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@Tag(name = "시선 트래킹", description = "훈련 앱 시선 트래킹 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/app/gaze")
public class GazeController {
    private final GazeService gazeService;

    @Operation(summary = "시선 추적 장치 연결 상태 확인")
    @GetMapping("/device/status")
    public GazeDeviceStatusResponse getDeviceStatus(
            @CurrentTeacherId Long teacherId,
            @RequestParam Long studentId
    ) {
        return gazeService.getDeviceStatus(teacherId, studentId);
    }

    @Operation(summary = "시선 추적 보정 안내 조회")
    @GetMapping("/calibration-guide")
    public GazeCalibrationGuideResponse getCalibrationGuide(
            @CurrentTeacherId Long teacherId,
            @RequestParam Long studentId
    ) {
        return gazeService.getCalibrationGuide(teacherId, studentId);
    }

    @Operation(summary = "시선 데이터 수집 시작")
    @PostMapping("/sessions")
    @ResponseStatus(HttpStatus.CREATED)
    public GazeSessionResponse startSession(
            @CurrentTeacherId Long teacherId,
            @Valid @RequestBody StartGazeSessionRequest request
    ) {
        return gazeService.startSession(teacherId, request);
    }

    @Operation(summary = "시선 추적 실패 처리")
    @PatchMapping("/sessions/{gazeSessionId}/failed")
    public GazeSessionResponse failSession(
            @CurrentTeacherId Long teacherId,
            @PathVariable Long gazeSessionId,
            @Valid @RequestBody FailGazeSessionRequest request
    ) {
        return gazeService.failSession(teacherId, gazeSessionId, request);
    }

    @Operation(summary = "시선 데이터 수집 종료")
    @PatchMapping("/sessions/{gazeSessionId}/end")
    public GazeSessionResponse endSession(
            @CurrentTeacherId Long teacherId,
            @PathVariable Long gazeSessionId,
            @Valid @RequestBody EndGazeSessionRequest request
    ) {
        return gazeService.endSession(teacherId, gazeSessionId, request);
    }

    @Operation(
            summary = "시선 분석 결과 저장",
            description = "아이트래커 측정 종료 후 분석한 시선 결과를 저장합니다."
    )
    @PostMapping("/sessions/{gazeSessionId}/analysis-results")
    @ResponseStatus(HttpStatus.CREATED)
    public GazeAnalysisResultResponse saveAnalysisResult(
            @CurrentTeacherId Long teacherId,
            @PathVariable Long gazeSessionId,
            @Valid @RequestBody GazeAnalysisResultRequest request
    ) {
        return gazeService.saveAnalysisResult(teacherId, gazeSessionId, request);
    }
}