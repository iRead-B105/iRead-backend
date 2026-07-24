package com.iread.backend.student.controller;

import com.iread.backend.auth.annotation.CurrentTeacherId;
import com.iread.backend.student.dto.req.StudentRequest;
import com.iread.backend.student.dto.req.TeacherMemoRequest;
import com.iread.backend.student.dto.res.AccuracyTrendResponse;
import com.iread.backend.student.dto.res.CreateStudentResponse;
import com.iread.backend.student.dto.res.ReadingSpeedTrendResponse;
import com.iread.backend.student.dto.res.StudentListResponse;
import com.iread.backend.student.dto.res.StudentResponse;
import com.iread.backend.student.dto.res.TrainingHistoryResponse;
import com.iread.backend.student.service.StudentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

@Tag(name = "학생 관리", description = "관리자 앱 학생 관리 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/student")
public class StudentController {

    private final StudentService studentService;

    @Operation(summary = "담당 학생 목록 조회")
    @GetMapping("/list")
    public List<StudentListResponse> getStudents(@CurrentTeacherId Long teacherId) {
        return studentService.getStudents(teacherId);
    }

    @Operation(summary = "학생 상세 정보 조회")
    @GetMapping("/{studentId}")
    public StudentResponse getStudent(@CurrentTeacherId Long teacherId, @PathVariable Long studentId) {
        return studentService.getStudent(teacherId, studentId);
    }

    @Operation(summary = "학생 등록")
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CreateStudentResponse> createStudent(
            @CurrentTeacherId Long teacherId,
            @Valid @RequestBody StudentRequest request
    ) {
        return ResponseEntity.ok(new CreateStudentResponse(studentService.createStudent(teacherId, request)));
    }

    @Operation(summary = "프로필 이미지와 함께 학생 등록")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CreateStudentResponse> createStudentWithImage(
            @CurrentTeacherId Long teacherId,
            @Valid @RequestPart("request") StudentRequest request,
            @RequestPart(value = "image", required = false) MultipartFile image
    ) {
        return ResponseEntity.ok(new CreateStudentResponse(
                studentService.createStudent(teacherId, request, image)
        ));
    }

    @Operation(summary = "학생 삭제")
    @DeleteMapping("/{studentId}")
    public ResponseEntity<Void> deleteStudent(@CurrentTeacherId Long teacherId, @PathVariable Long studentId) {
        studentService.deleteStudent(teacherId, studentId);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "학생 정보 수정")
    @PatchMapping(value = "/{studentId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> updateStudent(
            @CurrentTeacherId Long teacherId,
            @PathVariable Long studentId,
            @Valid @RequestBody StudentRequest request
    ) {
        studentService.updateStudent(teacherId, studentId, request);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "프로필 이미지와 함께 학생 정보 수정")
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

    @Operation(summary = "학생 교수자 내부 메모 저장")
    @PatchMapping("/{studentId}/teacher-memo")
    public ResponseEntity<Void> updateTeacherMemo(
            @CurrentTeacherId Long teacherId,
            @PathVariable Long studentId,
            @RequestBody TeacherMemoRequest request
    ) {
        studentService.updateTeacherMemo(teacherId, studentId, request.teacherMemo());
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "단어 시도 점수 기반 학생의 일별 읽기 정확도 추이 조회")
    @GetMapping("/{studentId}/accuracy-trend")
    public List<AccuracyTrendResponse> getAccuracyTrend(
            @CurrentTeacherId Long teacherId,
            @PathVariable Long studentId
    ) {
        return studentService.getAccuracyTrend(teacherId, studentId);
    }

    @Operation(summary = "학생의 학습 기록 조회")
    @GetMapping("/{studentId}/training-history")
    public List<TrainingHistoryResponse> getTrainingHistory(
            @CurrentTeacherId Long teacherId,
            @PathVariable Long studentId
    ) {
        return studentService.getTrainingHistory(teacherId, studentId);
    }

    @Operation(summary = "학생의 일별 읽기 속도 추이 조회")
    @GetMapping("/{studentId}/reading-speed-trend")
    public ReadingSpeedTrendResponse getReadingSpeedTrend(
            @CurrentTeacherId Long teacherId,
            @PathVariable Long studentId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return studentService.getReadingSpeedTrend(teacherId, studentId, from, to);
    }

}
