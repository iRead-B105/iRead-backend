package com.iread.backend.learning.app.controller;

import com.iread.backend.auth.annotation.CurrentStudentId;
import com.iread.backend.auth.annotation.CurrentTeacherId;
import com.iread.backend.exception.ResourceNotFoundException;
import com.iread.backend.global.storage.FileStorage;
import com.iread.backend.global.storage.LoadedFile;
import com.iread.backend.test.repository.TestDataRepository;
import com.iread.backend.training.repository.TrainingDataRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 학습(훈련·검사) 문항 삽화를 아동 세션 인증 하에 서빙한다. */
@Tag(name = "학습 앱 문항 삽화", description = "그림 문항 삽화 조회 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/app/learning")
public class LearningImageController {

    private final TrainingDataRepository trainingDataRepository;
    private final TestDataRepository testDataRepository;
    private final FileStorage fileStorage;

    @Operation(summary = "학습 문항 삽화 조회")
    @GetMapping("/{studentId}/images/{fileName}")
    public ResponseEntity<byte[]> getQuestionImage(
            @CurrentTeacherId Long teacherId,
            @CurrentStudentId Long authenticatedStudentId,
            @PathVariable Long studentId,
            @PathVariable String fileName
    ) {
        if (!authenticatedStudentId.equals(studentId)) {
            throw new ResourceNotFoundException("학습 문항 삽화를 찾을 수 없습니다.");
        }
        if (fileName == null || !fileName.matches("[0-9a-f-]{36}\\.(png|jpg|jpeg)")) {
            throw new IllegalArgumentException("올바르지 않은 이미지 파일 이름입니다.");
        }
        boolean owned = trainingDataRepository.existsByStudentIdAndImageFileName(studentId, fileName)
                || testDataRepository.existsByStudentIdAndImageFileName(studentId, fileName);
        if (!owned) {
            throw new ResourceNotFoundException("학습 문항 삽화를 찾을 수 없습니다.");
        }
        LoadedFile image = fileStorage.load(fileName);
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "private, max-age=3600")
                .contentType(MediaType.parseMediaType(image.contentType()))
                .body(image.content());
    }
}
