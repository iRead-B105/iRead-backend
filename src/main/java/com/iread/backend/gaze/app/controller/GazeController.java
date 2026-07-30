package com.iread.backend.gaze.app.controller;

import com.iread.backend.auth.annotation.CurrentStudentId;
import com.iread.backend.auth.annotation.CurrentTeacherId;
import com.iread.backend.gaze.app.dto.req.EndGazeSessionRequest;
import com.iread.backend.gaze.app.dto.req.FailGazeSessionRequest;
import com.iread.backend.gaze.app.dto.req.StartGazeSessionRequest;
import com.iread.backend.gaze.app.dto.res.GazeCalibrationGuideResponse;
import com.iread.backend.gaze.app.dto.res.GazeDeviceStatusResponse;
import com.iread.backend.gaze.app.dto.res.GazeSessionResponse;
import com.iread.backend.gaze.app.service.GazeService;
import com.iread.backend.security.StudentResourceAccessPolicy;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Gaze tracking", description = "Learner app gaze tracking APIs")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/app/gaze")
public class GazeController {
    private final GazeService gazeService;
    private final StudentResourceAccessPolicy studentResourceAccessPolicy;

    @Operation(summary = "Get gaze device status")
    @GetMapping("/device/status")
    public GazeDeviceStatusResponse getDeviceStatus(
            @CurrentTeacherId Long teacherId,
            @CurrentStudentId Long authenticatedStudentId,
            @RequestParam Long studentId
    ) {
        authorizeStudent(authenticatedStudentId, studentId);
        return gazeService.getDeviceStatus(teacherId, studentId);
    }

    @Operation(summary = "Get gaze calibration guide")
    @GetMapping("/calibration-guide")
    public GazeCalibrationGuideResponse getCalibrationGuide(
            @CurrentTeacherId Long teacherId,
            @CurrentStudentId Long authenticatedStudentId,
            @RequestParam Long studentId
    ) {
        authorizeStudent(authenticatedStudentId, studentId);
        return gazeService.getCalibrationGuide(teacherId, studentId);
    }

    @Operation(summary = "Start gaze data collection")
    @PostMapping("/sessions")
    public GazeSessionResponse startSession(
            @CurrentTeacherId Long teacherId,
            @CurrentStudentId Long authenticatedStudentId,
            @Valid @RequestBody StartGazeSessionRequest request
    ) {
        authorizeStudent(authenticatedStudentId, request.studentId());
        return gazeService.startSession(teacherId, request);
    }

    @Operation(summary = "Mark gaze tracking as failed")
    @PatchMapping("/sessions/{gazeSessionId}/failed")
    public GazeSessionResponse failSession(
            @CurrentTeacherId Long teacherId,
            @CurrentStudentId Long authenticatedStudentId,
            @PathVariable Long gazeSessionId,
            @Valid @RequestBody FailGazeSessionRequest request
    ) {
        authorizeStudent(authenticatedStudentId, request.studentId());
        return gazeService.failSession(teacherId, gazeSessionId, request);
    }

    @Operation(summary = "End gaze data collection")
    @PatchMapping("/sessions/{gazeSessionId}/end")
    public GazeSessionResponse endSession(
            @CurrentTeacherId Long teacherId,
            @CurrentStudentId Long authenticatedStudentId,
            @PathVariable Long gazeSessionId,
            @Valid @RequestBody EndGazeSessionRequest request
    ) {
        authorizeStudent(authenticatedStudentId, request.studentId());
        return gazeService.endSession(teacherId, gazeSessionId, request);
    }

    private void authorizeStudent(Long authenticatedStudentId, Long requestedStudentId) {
        studentResourceAccessPolicy.requireSameStudent(authenticatedStudentId, requestedStudentId);
    }
}
