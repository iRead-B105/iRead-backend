package com.iread.backend.global.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Profile("demo")
@RequiredArgsConstructor
public class QaDemoDatasetService {

    static final String DATASET_RESOURCE = "db/demo-data/qa-demo-reset.sql";
    private static final long POST_SEED_MARKER_ID = 299001L;

    private final JdbcTemplate jdbcTemplate;

    public boolean isPostSeedInstalled() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM student_feature_profiles WHERE id = ? AND student_id = 2001",
                Integer.class,
                POST_SEED_MARKER_ID
        );
        return count != null && count > 0;
    }

    @Transactional
    public void install() {
        jdbcTemplate.execute((ConnectionCallback<Void>) connection -> {
            ScriptUtils.executeSqlScript(connection, new ClassPathResource(DATASET_RESOURCE));
            return null;
        });
    }
}
