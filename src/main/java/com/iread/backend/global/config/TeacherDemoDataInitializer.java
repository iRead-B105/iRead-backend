package com.iread.backend.global.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Order(30)
@RequiredArgsConstructor
@ConditionalOnProperty(name = "iread.teacher-demo-seed.enabled", havingValue = "true")
public class TeacherDemoDataInitializer implements ApplicationRunner {

    private static final String SHOWCASE_MARKER_QUERY =
            "SELECT COUNT(*) FROM trainings WHERE id = ?";
    private static final long SHOWCASE_MARKER_ID = 43001L;
    private static final String SHOWCASE_SEED_RESOURCE = "db/demo-data/teacher-showcase.sql";

    private static final String PERSONA_MARKER_QUERY =
            "SELECT COUNT(*) FROM trainings WHERE id = ? AND accuracy > 100";
    private static final long PERSONA_MARKER_ID = 230101L;
    private static final String PERSONA_SEED_RESOURCE = "db/demo-data/teacher-personas.sql";

    private final JdbcTemplate jdbcTemplate;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        applyIfMissing(
                SHOWCASE_MARKER_QUERY,
                SHOWCASE_MARKER_ID,
                SHOWCASE_SEED_RESOURCE
        );
        applyIfMissing(
                PERSONA_MARKER_QUERY,
                PERSONA_MARKER_ID,
                PERSONA_SEED_RESOURCE
        );
    }

    private void applyIfMissing(String markerQuery, long markerId, String seedResource) {
        Integer count = jdbcTemplate.queryForObject(
                markerQuery,
                Integer.class,
                markerId
        );
        if (count != null && count > 0) {
            return;
        }

        jdbcTemplate.execute((ConnectionCallback<Void>) connection -> {
            ScriptUtils.executeSqlScript(connection, new ClassPathResource(seedResource));
            return null;
        });
    }
}
