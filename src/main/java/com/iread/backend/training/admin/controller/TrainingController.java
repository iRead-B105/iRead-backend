package com.iread.backend.training.admin.controller;

import com.iread.backend.auth.annotation.CurrentTeacherId;
import com.iread.backend.training.admin.dto.req.ExpectedWordRequest;
import com.iread.backend.training.admin.dto.req.UpdateCurriculumRequest;
import com.iread.backend.training.admin.dto.res.*;
import com.iread.backend.training.admin.service.TrainingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "훈련 관리", description = "관리자 앱 커리큘럼 및 훈련 관리 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/training")
public class TrainingController {
    private final TrainingService trainingService;

    @Operation(summary = "학생의 완료된 일일 커리큘럼 기록 조회")
    @GetMapping("/{studentId}/curriculum-log")
    public List<CurriculumLogResponse> getCurriculumLogs(@CurrentTeacherId Long teacherId, @PathVariable Long studentId) {
        return trainingService.getCurriculumLogs(teacherId, studentId);
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
    public List<TrainingCatalogResponse> getTrainingCatalog(@CurrentTeacherId Long teacherId, @PathVariable Long studentId) {
        return trainingService.getTrainingCatalog(teacherId, studentId);
    }

    @Operation(summary = "학생의 일일 커리큘럼 조회")
    @GetMapping("/{studentId}/{curriculumId}")
    public DailyCurriculumResponse getDailyCurriculum(@CurrentTeacherId Long teacherId, @PathVariable Long studentId,
                                                       @PathVariable Long curriculumId) {
        return trainingService.getDailyCurriculum(teacherId, studentId, curriculumId);
    }

    @Operation(summary = "학생의 일일 커리큘럼 수정")
    @PatchMapping("/{studentId}/{curriculumId}")
    public ResponseEntity<Void> updateDailyCurriculum(@CurrentTeacherId Long teacherId, @PathVariable Long studentId,
                                                       @PathVariable Long curriculumId,
                                                       @Valid @RequestBody UpdateCurriculumRequest request) {
        trainingService.updateDailyCurriculum(teacherId, studentId, curriculumId, request);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "훈련에 사용할 예정 단어 목록 조회")
    @GetMapping("/{studentId}/{trainingId}/expected-word")
    public List<ExpectedWordResponse> getExpectedWords(@CurrentTeacherId Long teacherId, @PathVariable Long studentId,
                                                       @PathVariable Long trainingId) {
        return trainingService.getExpectedWords(teacherId, studentId, trainingId);
    }

    @Operation(summary = "훈련에 사용할 예정 단어 추가")
    @PostMapping("/{studentId}/{trainingId}/expected-word")
    public ResponseEntity<Void> addExpectedWord(@CurrentTeacherId Long teacherId, @PathVariable Long studentId,
                                                 @PathVariable Long trainingId,
                                                 @Valid @RequestBody ExpectedWordRequest request) {
        trainingService.addExpectedWord(teacherId, studentId, trainingId, request);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "훈련에 사용할 예정 단어 삭제")
    @DeleteMapping("/{studentId}/{trainingId}/expected-word/{wordId}")
    public ResponseEntity<Void> deleteExpectedWord(@CurrentTeacherId Long teacherId, @PathVariable Long studentId,
                                                    @PathVariable Long trainingId, @PathVariable Long wordId) {
        trainingService.deleteExpectedWord(teacherId, studentId, trainingId, wordId);
        return ResponseEntity.ok().build();
    }
}
