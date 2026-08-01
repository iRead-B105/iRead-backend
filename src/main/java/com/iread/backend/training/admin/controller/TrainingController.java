package com.iread.backend.training.admin.controller;

import com.iread.backend.auth.annotation.CurrentTeacherId;
import com.iread.backend.training.admin.dto.req.CompleteTrainingRequest;
import com.iread.backend.training.admin.dto.req.UpdateLessonMaterialRequest;
import com.iread.backend.training.admin.dto.req.UpdateCurriculumRequest;
import com.iread.backend.training.admin.dto.res.*;
import com.iread.backend.training.admin.service.TrainingService;
import com.iread.backend.training.admin.service.LessonMaterialService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import tools.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@Tag(name = "훈련 관리", description = "관리자 앱 커리큘럼 및 훈련 관리 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/training")
public class TrainingController {
    private final TrainingService trainingService;
    private final LessonMaterialService lessonMaterialService;

    @Operation(summary = "학생의 완료된 일일 커리큘럼 기록 조회")
    @GetMapping("/{studentId}/curriculum-log")
    public List<CurriculumLogResponse> getCurriculumLogs(
            @CurrentTeacherId Long teacherId,
            @PathVariable Long studentId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return trainingService.getCurriculumLogs(teacherId, studentId, from, to);
    }

    @Operation(summary = "일일 커리큘럼의 훈련 이력 조회")
    @GetMapping("/{studentId}/{curriculumId}/training-log")
    public TrainingLogResponse getTrainingLog(@CurrentTeacherId Long teacherId, @PathVariable Long studentId,
                                               @PathVariable Long curriculumId) {
        return trainingService.getTrainingLog(teacherId, studentId, curriculumId);
    }

    @Operation(summary = "일일 커리큘럼의 훈련 통계 조회")
    @GetMapping("/{studentId}/{curriculumId}/statistics")
    public TrainingStatisticsResponse getStatistics(@CurrentTeacherId Long teacherId, @PathVariable Long studentId,
                                                     @PathVariable Long curriculumId) {
        return trainingService.getStatistics(teacherId, studentId, curriculumId);
    }

    @Operation(summary = "학생에게 제공할 훈련 목록 조회")
    @GetMapping("/{studentId}")
    public TrainingCatalogDataResponse getTrainingCatalog(
            @CurrentTeacherId Long teacherId,
            @PathVariable Long studentId
    ) {
        return new TrainingCatalogDataResponse(
                trainingService.getTrainingCatalog(teacherId, studentId)
        );
    }

    @Operation(summary = "학생의 일일 커리큘럼 조회")
    @GetMapping("/{studentId}/{curriculumId}")
    public DailyCurriculumResponse getDailyCurriculum(@CurrentTeacherId Long teacherId, @PathVariable Long studentId,
                                                       @PathVariable Long curriculumId) {
        return trainingService.getDailyCurriculum(teacherId, studentId, curriculumId);
    }

    @Operation(summary = "수정 가능한 현재 커리큘럼 조회")
    @GetMapping("/{studentId}/current")
    public DailyCurriculumResponse getCurrentDailyCurriculum(
            @CurrentTeacherId Long teacherId,
            @PathVariable Long studentId
    ) {
        return trainingService.getCurrentDailyCurriculum(teacherId, studentId);
    }

    @Operation(summary = "아동의 활성 커리큘럼 조회")
    @GetMapping("/{studentId}/active")
    public DailyCurriculumResponse getActiveDailyCurriculum(
            @CurrentTeacherId Long teacherId,
            @PathVariable Long studentId
    ) {
        return trainingService.getActiveDailyCurriculum(teacherId, studentId);
    }

    @Operation(summary = "학생의 일일 커리큘럼 생성")
    @PostMapping("/{studentId}/curriculum")
    public DailyCurriculumResponse createDailyCurriculum(
            @CurrentTeacherId Long teacherId,
            @PathVariable Long studentId,
            @Valid @RequestBody UpdateCurriculumRequest request
    ) {
        return trainingService.createDailyCurriculum(teacherId, studentId, request);
    }

    @Operation(summary = "학생의 일일 커리큘럼 수정")
    @PatchMapping("/{studentId}/{curriculumId}")
    public ResponseEntity<Void> updateDailyCurriculum(@CurrentTeacherId Long teacherId, @PathVariable Long studentId,
                                                       @PathVariable Long curriculumId,
                                                       @Valid @RequestBody UpdateCurriculumRequest request) {
        trainingService.updateDailyCurriculum(teacherId, studentId, curriculumId, request);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Complete final review of a test-recommended curriculum")
    @PostMapping("/{studentId}/{curriculumId}/review-complete")
    public CurriculumReviewResponse completeCurriculumReview(
            @CurrentTeacherId Long teacherId,
            @PathVariable Long studentId,
            @PathVariable Long curriculumId
    ) {
        return trainingService.completeCurriculumReview(
                teacherId,
                studentId,
                curriculumId
        );
    }

    @Operation(summary = "Get editable lesson materials")
    @GetMapping("/{studentId}/{trainingId}/lesson-material")
    public LessonMaterialResponse getLessonMaterial(
            @CurrentTeacherId Long teacherId,
            @PathVariable Long studentId,
            @PathVariable Long trainingId
    ) {
        return lessonMaterialService.getLessonMaterial(teacherId, studentId, trainingId);
    }

    @Operation(summary = "Validate and save all lesson materials")
    @PutMapping("/{studentId}/{trainingId}/lesson-material")
    public SaveLessonMaterialResponse updateLessonMaterial(
            @CurrentTeacherId Long teacherId,
            @PathVariable Long studentId,
            @PathVariable Long trainingId,
            @Valid @RequestBody UpdateLessonMaterialRequest request
    ) {
        return lessonMaterialService.updateLessonMaterial(
                teacherId,
                studentId,
                trainingId,
                request
        );
    }

    @Operation(summary = "선택한 훈련의 템플릿, 생성 데이터와 결과 조회")
    @GetMapping("/{studentId}/{trainingId}/detail")
    public TrainingDetailResponse getTrainingDetail(
            @CurrentTeacherId Long teacherId,
            @PathVariable Long studentId,
            @PathVariable Long trainingId
    ) {
        return trainingService.getTrainingDetail(teacherId, studentId, trainingId);
    }

    @Operation(summary = "훈련 결과를 JSON 또는 CSV 파일로 내보내기")
    @PostMapping(
            value = "/{studentId}/{trainingId}/export",
            produces = MediaType.APPLICATION_OCTET_STREAM_VALUE
    )
    public ResponseEntity<byte[]> exportTraining(
            @CurrentTeacherId Long teacherId,
            @PathVariable Long studentId,
            @PathVariable Long trainingId,
            @RequestParam String format
    ) {
        TrainingExportFile file = trainingService.exportTraining(
                teacherId, studentId, trainingId, format
        );
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + file.fileName() + "\""
                )
                .body(file.content());
    }

    @Operation(summary = "AI 훈련 문제 생성")
    @PostMapping("/{studentId}/{trainingId}/generate")
    public JsonNode generateTraining(@CurrentTeacherId Long teacherId, @PathVariable Long studentId,
                                     @PathVariable Long trainingId) {
        return trainingService.generateTraining(teacherId, studentId, trainingId);
    }

    @Operation(summary = "훈련 결과 AI 평가 및 완료")
    @PostMapping("/{studentId}/{trainingId}/complete")
    public TrainingEvaluationResponse completeTraining(
            @CurrentTeacherId Long teacherId,
            @PathVariable Long studentId,
            @PathVariable Long trainingId,
            @Valid @RequestBody CompleteTrainingRequest request
        ) {
        return new TrainingEvaluationResponse(
                trainingService.completeTraining(
                        teacherId,
                        studentId,
                        trainingId,
                        request.result(),
                        request.completedAt()
                )
        );
    }
}
