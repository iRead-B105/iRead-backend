package com.iread.backend.student.service;

import com.iread.backend.global.storage.FileStorage;
import com.iread.backend.global.storage.StoredFile;
import com.iread.backend.student.domain.StudentEntity;
import com.iread.backend.student.dto.req.StudentRequest;
import com.iread.backend.student.dto.res.AccuracyTrendResponse;
import com.iread.backend.student.dto.res.StudentListResponse;
import com.iread.backend.student.dto.res.StudentResponse;
import com.iread.backend.student.dto.res.TrainingHistoryResponse;
import com.iread.backend.student.repository.StudentRepository;
import com.iread.backend.teacher.domain.TeacherEntity;
import com.iread.backend.teacher.repository.TeacherRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StudentServiceImpl implements StudentService {

    private final StudentRepository studentRepository;
    private final TeacherRepository teacherRepository;
    private final FileStorage fileStorage;

    @Override
    public List<StudentListResponse> getStudents(Long teacherId) {
        validateTeacher(teacherId);

        Map<Long, StudentRepository.StudentLearningSummaryProjection> summaries =
                studentRepository.findLearningSummaries(teacherId).stream()
                        .collect(Collectors.toMap(
                                StudentRepository.StudentLearningSummaryProjection::getStudentId,
                                Function.identity()
                        ));

        return studentRepository.findAllByTeacherIdOrderByIdAsc(teacherId).stream()
                .map(student -> toListResponse(student, summaries.get(student.getId())))
                .toList();
    }

    @Override
    public StudentResponse getStudent(Long teacherId, Long studentId) {
        return toResponse(findOwnedStudent(teacherId, studentId));
    }

    @Override
    @Transactional
    public void createStudent(Long teacherId, StudentRequest request) {
        createStudent(teacherId, request, null);
    }

    @Override
    @Transactional
    public void createStudent(Long teacherId, StudentRequest request, MultipartFile imageFile) {
        TeacherEntity teacher = validateTeacher(teacherId);
        if (request.name() == null || request.name().isBlank()) {
            throw new IllegalArgumentException("학생 이름은 필수입니다.");
        }
        boolean uploaded = imageFile != null && !imageFile.isEmpty();
        StoredFile storedFile = uploaded ? fileStorage.store(imageFile) : null;
        String imageUrl = uploaded ? storedFile.url() : request.imageUrl();

        StudentEntity student = StudentEntity.builder()
                .teacher(teacher)
                .name(request.name())
                .birthday(request.birthday())
                .gender(request.gender())
                .school(request.school())
                .guardian(request.guardian())
                .guardianContact(request.guardianContact())
                .guardianEmail(request.guardianEmail())
                .address(request.address())
                .imageUrl(imageUrl)
                .build();

        try {
            studentRepository.save(student);
        } catch (RuntimeException exception) {
            if (storedFile != null) fileStorage.delete(storedFile.storeFileName());
            throw exception;
        }
    }

    @Override
    @Transactional
    public void deleteStudent(Long teacherId, Long studentId) {
        StudentEntity student = findOwnedStudent(teacherId, studentId);
        studentRepository.deleteTrainingsByStudentId(studentId);
        studentRepository.deleteDailyCurriculumsByStudentId(studentId);
        studentRepository.delete(student);
    }

    @Override
    @Transactional
    public void updateStudent(Long teacherId, Long studentId, StudentRequest request) {
        updateStudent(teacherId, studentId, request, null);
    }

    @Override
    @Transactional
    public void updateStudent(
            Long teacherId,
            Long studentId,
            StudentRequest request,
            MultipartFile imageFile
    ) {
        StudentEntity student = findOwnedStudent(teacherId, studentId);

        boolean uploaded = imageFile != null && !imageFile.isEmpty();
        boolean updateImage = uploaded || request.imageUrl() != null;
        String oldImageUrl = student.getImageUrl();
        StoredFile storedFile = uploaded ? fileStorage.store(imageFile) : null;
        String imageUrl = uploaded ? storedFile.url() : request.imageUrl();
        student.update(
                request.name(),
                request.birthday(),
                request.gender(),
                request.school(),
                request.guardian(),
                request.guardianContact(),
                request.guardianEmail(),
                request.address(),
                imageUrl,
                updateImage
        );

        if (uploaded && oldImageUrl != null) {
            fileStorage.delete(fileNameOf(oldImageUrl));
        }
    }

    @Override
    @Transactional
    public void updateTeacherMemo(Long teacherId, Long studentId, String teacherMemo) {
        StudentEntity student = findOwnedStudent(teacherId, studentId);
        student.updateTeacherMemo(teacherMemo);
    }

    @Override
    public List<AccuracyTrendResponse> getAccuracyTrend(Long teacherId, Long studentId) {
        findOwnedStudent(teacherId, studentId);
        return studentRepository.findAccuracyTrend(studentId).stream()
                .map(row -> new AccuracyTrendResponse(row.getLearningDate(), row.getAccuracy()))
                .toList();
    }

    @Override
    public List<TrainingHistoryResponse> getTrainingHistory(Long teacherId, Long studentId) {
        findOwnedStudent(teacherId, studentId);
        return studentRepository.findTrainingHistory(studentId).stream()
                .map(row -> new TrainingHistoryResponse(
                        row.getLearningDate(),
                        row.getLearningType(),
                        row.getStartedAt(),
                        row.getFinishedAt(),
                        row.getAchievement()
                ))
                .toList();
    }

    private TeacherEntity validateTeacher(Long teacherId) {
        return teacherRepository.findById(teacherId)
                .orElseThrow(() -> new IllegalArgumentException("교사를 찾을 수 없습니다."));
    }

    private StudentEntity findOwnedStudent(Long teacherId, Long studentId) {
        return studentRepository.findByIdAndTeacherId(studentId, teacherId)
                .orElseThrow(() -> new IllegalArgumentException("학생을 찾을 수 없습니다."));
    }

    private String fileNameOf(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) return null;
        int slash = imageUrl.lastIndexOf('/');
        return slash < 0 ? imageUrl : imageUrl.substring(slash + 1);
    }

    private StudentListResponse toListResponse(
            StudentEntity student,
            StudentRepository.StudentLearningSummaryProjection summary
    ) {
        Integer age = student.getBirthday() == null
                ? null
                : Period.between(student.getBirthday(), LocalDate.now()).getYears();

        return new StudentListResponse(
                student.getId(),
                student.getName(),
                age,
                summary == null || summary.getRecentFinishedAt() == null
                        ? null : summary.getRecentFinishedAt().toLocalDate(),
                summary == null ? 0L : summary.getTotalLearningMinutes(),
                summary == null ? null : summary.getRecentTrainingName()
        );
    }

    private StudentResponse toResponse(StudentEntity student) {
        return new StudentResponse(
                student.getId(),
                student.getName(),
                student.getBirthday(),
                student.getGender(),
                student.getSchool(),
                student.getGuardian(),
                student.getGuardianContact(),
                student.getGuardianEmail(),
                student.getAddress(),
                student.getImageUrl(),
                student.getTeacherMemo()
        );
    }
}
