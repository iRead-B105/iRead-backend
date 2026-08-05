package com.iread.backend.student.service;

import com.iread.backend.student.dto.req.StudentRequest;
import com.iread.backend.student.domain.LearningEventType;
import com.iread.backend.student.dto.res.AccuracyRecordsResponse;
import com.iread.backend.student.dto.res.AccuracyTrendDataResponse;
import com.iread.backend.student.dto.res.LearningEventResponse;
import com.iread.backend.student.dto.res.LearningEventListResponse;
import com.iread.backend.student.dto.res.LearningSummaryResponse;
import com.iread.backend.student.dto.res.ReadingSpeedTrendResponse;
import com.iread.backend.student.dto.res.ReadingSpeedRecordsResponse;
import com.iread.backend.student.dto.res.StudentListResponse;
import com.iread.backend.student.dto.res.StudentListDataResponse;
import com.iread.backend.student.dto.res.StudentResponse;
import com.iread.backend.student.dto.res.StudentSummaryResponse;
import com.iread.backend.student.dto.res.TrainingHistoryResponse;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

public interface StudentService {
    StudentListDataResponse getStudents(
            Long teacherId,
            String keyword,
            Integer age,
            Integer recentDays,
            int page,
            int size
    );
    StudentSummaryResponse getStudentSummary(Long teacherId);
    StudentResponse getStudent(Long teacherId, Long studentId);
    Long createStudent(Long teacherId, StudentRequest request);
    Long createStudent(Long teacherId, StudentRequest request, MultipartFile imageFile);
    void deleteStudent(Long teacherId, Long studentId);
    void updateStudent(Long teacherId, Long studentId, StudentRequest request);
    void updateStudent(Long teacherId, Long studentId, StudentRequest request, MultipartFile imageFile);
    void updateTeacherMemo(Long teacherId, Long studentId, String teacherMemo);
    AccuracyTrendDataResponse getAccuracyTrend(Long teacherId, Long studentId);
    AccuracyRecordsResponse getAccuracyRecords(
            Long teacherId,
            Long studentId,
            LocalDate from,
            LocalDate to
    );
    List<TrainingHistoryResponse> getTrainingHistory(
            Long teacherId,
            Long studentId,
            LocalDate from,
            LocalDate to
    );
    LearningSummaryResponse getLearningSummary(Long teacherId, Long studentId);
    LearningEventListResponse getRecentLearningEvents(
            Long teacherId,
            Long studentId,
            int limit
    );
    LearningEventResponse getLearningEvent(
            Long teacherId,
            Long studentId,
            LearningEventType eventType,
            Long eventId
    );
    ReadingSpeedTrendResponse getReadingSpeedTrend(
            Long teacherId,
            Long studentId,
            LocalDate from,
            LocalDate to
    );
    ReadingSpeedRecordsResponse getReadingSpeedRecords(
            Long teacherId,
            Long studentId,
            LocalDate from,
            LocalDate to
    );
}
