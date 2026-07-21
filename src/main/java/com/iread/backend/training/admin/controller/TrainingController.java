package com.iread.backend.training.admin.controller;

import com.iread.backend.auth.annotation.CurrentTeacherId;
import com.iread.backend.training.admin.dto.req.ExpectedWordRequest;
import com.iread.backend.training.admin.dto.req.UpdateCurriculumRequest;
import com.iread.backend.training.admin.dto.res.*;
import com.iread.backend.training.admin.service.TrainingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/training")
public class TrainingController {
    private final TrainingService trainingService;

    @GetMapping("/{studentId}/curriculum-log")
    public List<CurriculumLogResponse> getCurriculumLogs(@CurrentTeacherId Long teacherId, @PathVariable Long studentId) {
        return trainingService.getCurriculumLogs(teacherId, studentId);
    }

    @GetMapping("/{studentId}/{curriculumId}/training-log")
    public TrainingLogResponse getTrainingLog(@CurrentTeacherId Long teacherId, @PathVariable Long studentId,
                                               @PathVariable Long curriculumId) {
        return trainingService.getTrainingLog(teacherId, studentId, curriculumId);
    }

    @GetMapping("/{studentId}/{curriculumId}/statistics")
    public TrainingStatisticsResponse getStatistics(@CurrentTeacherId Long teacherId, @PathVariable Long studentId,
                                                     @PathVariable Long curriculumId) {
        return trainingService.getStatistics(teacherId, studentId, curriculumId);
    }

    @GetMapping("/{studentId}")
    public List<TrainingCatalogResponse> getTrainingCatalog(@CurrentTeacherId Long teacherId, @PathVariable Long studentId) {
        return trainingService.getTrainingCatalog(teacherId, studentId);
    }

    @GetMapping("/{studentId}/{curriculumId}")
    public DailyCurriculumResponse getDailyCurriculum(@CurrentTeacherId Long teacherId, @PathVariable Long studentId,
                                                       @PathVariable Long curriculumId) {
        return trainingService.getDailyCurriculum(teacherId, studentId, curriculumId);
    }

    @PatchMapping("/{studentId}/{curriculumId}")
    public ResponseEntity<Void> updateDailyCurriculum(@CurrentTeacherId Long teacherId, @PathVariable Long studentId,
                                                       @PathVariable Long curriculumId,
                                                       @Valid @RequestBody UpdateCurriculumRequest request) {
        trainingService.updateDailyCurriculum(teacherId, studentId, curriculumId, request);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{studentId}/{trainingId}/expected-word")
    public List<ExpectedWordResponse> getExpectedWords(@CurrentTeacherId Long teacherId, @PathVariable Long studentId,
                                                       @PathVariable Long trainingId) {
        return trainingService.getExpectedWords(teacherId, studentId, trainingId);
    }

    @PostMapping("/{studentId}/{trainingId}/expected-word")
    public ResponseEntity<Void> addExpectedWord(@CurrentTeacherId Long teacherId, @PathVariable Long studentId,
                                                 @PathVariable Long trainingId,
                                                 @Valid @RequestBody ExpectedWordRequest request) {
        trainingService.addExpectedWord(teacherId, studentId, trainingId, request);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{studentId}/{trainingId}/expected-word/{wordId}")
    public ResponseEntity<Void> deleteExpectedWord(@CurrentTeacherId Long teacherId, @PathVariable Long studentId,
                                                    @PathVariable Long trainingId, @PathVariable Long wordId) {
        trainingService.deleteExpectedWord(teacherId, studentId, trainingId, wordId);
        return ResponseEntity.ok().build();
    }
}
