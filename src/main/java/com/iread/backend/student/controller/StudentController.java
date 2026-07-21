package com.iread.backend.student.controller;

import com.iread.backend.auth.annotation.CurrentTeacherId;
import com.iread.backend.student.dto.req.StudentRequest;
import com.iread.backend.student.dto.res.AccuracyTrendResponse;
import com.iread.backend.student.dto.res.StudentListResponse;
import com.iread.backend.student.dto.res.StudentResponse;
import com.iread.backend.student.dto.res.TrainingHistoryResponse;
import com.iread.backend.student.service.StudentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/student")
public class StudentController {

    private final StudentService studentService;

    @GetMapping("/list")
    public List<StudentListResponse> getStudents(@CurrentTeacherId Long teacherId) {
        return studentService.getStudents(teacherId);
    }

    @GetMapping("/{studentId}")
    public StudentResponse getStudent(@CurrentTeacherId Long teacherId, @PathVariable Long studentId) {
        return studentService.getStudent(teacherId, studentId);
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> createStudent(
            @CurrentTeacherId Long teacherId,
            @Valid @RequestBody StudentRequest request
    ) {
        studentService.createStudent(teacherId, request);
        return ResponseEntity.ok().build();
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Void> createStudentWithImage(
            @CurrentTeacherId Long teacherId,
            @Valid @RequestPart("request") StudentRequest request,
            @RequestPart(value = "image", required = false) MultipartFile image
    ) {
        studentService.createStudent(teacherId, request, image);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{studentId}")
    public ResponseEntity<Void> deleteStudent(@CurrentTeacherId Long teacherId, @PathVariable Long studentId) {
        studentService.deleteStudent(teacherId, studentId);
        return ResponseEntity.ok().build();
    }

    @PatchMapping(value = "/{studentId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> updateStudent(
            @CurrentTeacherId Long teacherId,
            @PathVariable Long studentId,
            @Valid @RequestBody StudentRequest request
    ) {
        studentService.updateStudent(teacherId, studentId, request);
        return ResponseEntity.ok().build();
    }

    @PatchMapping(value = "/{studentId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Void> updateStudentWithImage(
            @CurrentTeacherId Long teacherId,
            @PathVariable Long studentId,
            @Valid @RequestPart("request") StudentRequest request,
            @RequestPart(value = "image", required = false) MultipartFile image
    ) {
        studentService.updateStudent(teacherId, studentId, request, image);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{studentId}/accuracy-trend")
    public List<AccuracyTrendResponse> getAccuracyTrend(
            @CurrentTeacherId Long teacherId,
            @PathVariable Long studentId
    ) {
        return studentService.getAccuracyTrend(teacherId, studentId);
    }

    @GetMapping("/{studentId}/training-history")
    public List<TrainingHistoryResponse> getTrainingHistory(
            @CurrentTeacherId Long teacherId,
            @PathVariable Long studentId
    ) {
        return studentService.getTrainingHistory(teacherId, studentId);
    }

}
