package com.iread.backend.auth.dto.res;

import com.iread.backend.student.domain.StudentEntity;
import com.iread.backend.teacher.domain.TeacherEntity;

import java.util.List;

public record AppTeacherLoginResponse(
        String teacherId,
        String teacherSessionToken,
        List<LinkedStudent> linkedStudents,
        long expiresIn,
        String loginStatus
) {
    public static AppTeacherLoginResponse selectionRequired(
            TeacherEntity teacher,
            String teacherSessionToken,
            long expiresIn,
            List<StudentEntity> students
    ) {
        return new AppTeacherLoginResponse(
                teacher.getId().toString(),
                teacherSessionToken,
                students.stream().map(LinkedStudent::from).toList(),
                expiresIn,
                "STUDENT_SELECTION_REQUIRED"
        );
    }

    public record LinkedStudent(String studentId, String name) {
        public static LinkedStudent from(StudentEntity student) {
            return new LinkedStudent(student.getId().toString(), student.getName());
        }
    }
}
