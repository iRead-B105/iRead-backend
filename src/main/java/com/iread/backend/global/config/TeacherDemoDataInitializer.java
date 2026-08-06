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

    // 마커는 qa-demo-reset.sql이 지우지 않는 행이어야 한다. 예전 마커(trainings 43001)는
    // 학생 2103 소속이라 리셋 때 삭제되어, 재부팅 시 showcase가 재적용되다 words 중복으로
    // 기동이 실패했다. words 10001은 다른 학생의 시도 로그가 참조해 리셋 후에도 남는다.
    private static final String SHOWCASE_MARKER_QUERY =
            "SELECT COUNT(*) FROM words WHERE id = ?";
    private static final long SHOWCASE_MARKER_ID = 10001L;
    private static final String SHOWCASE_SEED_RESOURCE = "db/demo-data/teacher-showcase.sql";

    // 예전 마커(230101)는 학생 2001 소속이라 qa-demo-reset 때 삭제되었다.
    // 230201은 리셋 대상이 아닌 학생 2101(페르소나 2)의 훈련이라 리셋 후에도 남는다.
    private static final String PERSONA_MARKER_QUERY =
            "SELECT COUNT(*) FROM trainings WHERE id = ? AND accuracy > 100";
    private static final long PERSONA_MARKER_ID = 230201L;
    private static final String PERSONA_SEED_RESOURCE = "db/demo-data/teacher-personas.sql";
    private static final String PERSONA_CORRECTION_RESOURCE =
            "db/demo-data/teacher-persona-current-curriculum-v2.sql";

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
        apply(PERSONA_CORRECTION_RESOURCE);
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

        apply(seedResource);
    }

    private void apply(String seedResource) {
        jdbcTemplate.execute((ConnectionCallback<Void>) connection -> {
            ScriptUtils.executeSqlScript(connection, new ClassPathResource(seedResource));
            return null;
        });
    }
}
