package com.iread.backend.student.service;

import com.iread.backend.student.dto.req.StudentRequest;
import com.iread.backend.student.dto.res.AccuracyTrendResponse;
import com.iread.backend.student.dto.res.StudentListResponse;
import com.iread.backend.student.dto.res.StudentResponse;
import com.iread.backend.student.dto.res.TrainingHistoryResponse;

import java.util.List;

public interface StudentService {
    List<StudentListResponse> getStudents(Long teacherId);
    StudentResponse getStudent(Long teacherId, Long studentId);
    void createStudent(Long teacherId, StudentRequest request);
    void deleteStudent(Long teacherId, Long studentId);
    void updateStudent(Long teacherId, Long studentId, StudentRequest request);
    List<AccuracyTrendResponse> getAccuracyTrend(Long teacherId, Long studentId);
    List<TrainingHistoryResponse> getTrainingHistory(Long teacherId, Long studentId);
}
