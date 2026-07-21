package com.iread.backend.student.service;

import com.iread.backend.student.dto.req.StudentCharacteristicsRequest;
import com.iread.backend.student.dto.req.StudentRequest;
import com.iread.backend.student.dto.res.*;
import com.iread.backend.student.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StudentServiceImpl implements StudentService{
    private final StudentRepository studentRepository;
    @Override
    public List<StudentListResponse> getStudents(Long teacherId) {
        return List.of();
    }

    @Override
    public StudentResponse getStudent(Long teacherId, Long studentId) {
        return null;
    }

    @Override
    public void createStudent(Long teacherId, StudentRequest request) {

    }

    @Override
    public void deleteStudent(Long teacherId, Long studentId) {

    }

    @Override
    public void updateStudent(Long teacherId, Long studentId, StudentRequest request) {

    }

    @Override
    public List<AccuracyTrendResponse> getAccuracyTrend(Long teacherId, Long studentId) {
        return List.of();
    }

    @Override
    public List<TrainingHistoryResponse> getTrainingHistory(Long teacherId, Long studentId) {
        return List.of();
    }

    @Override
    public StudentCharacteristicsResponse getCharacteristics(Long teacherId, Long studentId) {
        return null;
    }

    @Override
    public void updateCharacteristics(Long teacherId, Long studentId, StudentCharacteristicsRequest request) {

    }
}
