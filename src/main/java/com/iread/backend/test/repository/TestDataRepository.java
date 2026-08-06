package com.iread.backend.test.repository;

import com.iread.backend.test.domain.TestDataEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TestDataRepository extends JpaRepository<TestDataEntity, Long> {
    Optional<TestDataEntity> findFirstByTestIdOrderByCreatedAtDescIdDesc(Long testId);

    /** 생성된 검사 문항 데이터가 해당 학생 소유이면서 지정 삽화 파일을 참조하는지. */
    @org.springframework.data.jpa.repository.Query("""
            select count(data) > 0
              from TestDataEntity data
             where data.test.testCurriculum.student.id = :studentId
               and data.generatedData like concat('%', :fileName, '%')
            """)
    boolean existsByStudentIdAndImageFileName(
            @org.springframework.data.repository.query.Param("studentId") Long studentId,
            @org.springframework.data.repository.query.Param("fileName") String fileName
    );
}
