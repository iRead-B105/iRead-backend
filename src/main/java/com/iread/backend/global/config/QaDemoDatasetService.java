package com.iread.backend.global.config;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class QaDemoDatasetService {

    static final String DATASET_RESOURCE = "db/demo-data/qa-demo-reset.sql";
    private static final long POST_SEED_MARKER_ID = 299011L;
    static final long QA_TEACHER_ID = 1001L;
    static final String QA_TEACHER_EMAIL = "test@test.com";
    private static final String LEGACY_DEMO_TEACHER_EMAIL = "demo@iread.local";
    static final long[] QA_STUDENT_IDS = {2001L, 2002L, 2103L};
    private static final String DEPLOYMENT_KEY = "teacher-qa-demo";

    private final JdbcTemplate jdbcTemplate;

    @Value("${iread.qa-demo-dataset.allow-legacy-demo-identity:false}")
    private boolean allowLegacyDemoIdentity;

    public boolean isPostSeedInstalled() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM student_feature_profiles WHERE id = ? AND student_id = 2001",
                Integer.class,
                POST_SEED_MARKER_ID
        );
        return count != null && count > 0;
    }

    public boolean isAppliedForDeployment(String deploymentTag) {
        Integer count = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                  FROM qa_demo_dataset_deployments
                 WHERE dataset_key = ?
                   AND backend_tag = ?
                """,
                Integer.class,
                DEPLOYMENT_KEY,
                deploymentTag
        );
        return count != null && count > 0;
    }

    @Transactional
    public void install() {
        assertReservedQaIdentity();
        jdbcTemplate.execute((ConnectionCallback<Void>) connection -> {
            ScriptUtils.executeSqlScript(connection, new ClassPathResource(DATASET_RESOURCE));
            return null;
        });
    }

    @Transactional
    public void recordAppliedDeployment(String deploymentTag) {
        int updated = jdbcTemplate.update(
                """
                UPDATE qa_demo_dataset_deployments
                   SET backend_tag = ?, applied_at = CURRENT_TIMESTAMP
                 WHERE dataset_key = ?
                """,
                deploymentTag,
                DEPLOYMENT_KEY
        );
        if (updated == 0) {
            jdbcTemplate.update(
                    """
                    INSERT INTO qa_demo_dataset_deployments(dataset_key, backend_tag, applied_at)
                    VALUES (?, ?, CURRENT_TIMESTAMP)
                    """,
                    DEPLOYMENT_KEY,
                    deploymentTag
            );
        }
    }

    private void assertReservedQaIdentity() {
        assertTeacherIdIsReserved();
        assertQaEmailIsReserved();
        assertStudentsBelongToQaTeacher();
    }

    private void assertTeacherIdIsReserved() {
        jdbcTemplate.query(
                "SELECT email FROM teachers WHERE id = ?",
                resultSet -> {
                    String email = resultSet.getString(1);
                    if (!QA_TEACHER_EMAIL.equalsIgnoreCase(email)
                            && !(allowLegacyDemoIdentity
                            && LEGACY_DEMO_TEACHER_EMAIL.equalsIgnoreCase(email))) {
                        throw new IllegalStateException(
                                "QA dataset cannot replace teacher " + QA_TEACHER_ID
                                        + ": the reserved id belongs to " + email
                        );
                    }
                },
                QA_TEACHER_ID
        );
    }

    private void assertQaEmailIsReserved() {
        jdbcTemplate.query(
                "SELECT id FROM teachers WHERE email = ?",
                resultSet -> {
                    long teacherId = resultSet.getLong(1);
                    if (teacherId != QA_TEACHER_ID) {
                        throw new IllegalStateException(
                                "QA dataset cannot replace " + QA_TEACHER_EMAIL
                                        + ": the email belongs to teacher " + teacherId
                        );
                    }
                },
                QA_TEACHER_EMAIL
        );
    }

    private void assertStudentsBelongToQaTeacher() {
        for (long studentId : QA_STUDENT_IDS) {
            jdbcTemplate.query(
                    "SELECT teacher_id FROM students WHERE id = ?",
                    resultSet -> {
                        long teacherId = resultSet.getLong(1);
                        if (teacherId != QA_TEACHER_ID) {
                            throw new IllegalStateException(
                                    "QA dataset cannot replace student " + studentId
                                            + ": the reserved id belongs to teacher " + teacherId
                            );
                        }
                    },
                    studentId
            );
        }
    }
}
