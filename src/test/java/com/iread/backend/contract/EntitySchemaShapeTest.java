package com.iread.backend.contract;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ResourceLoader;
import org.springframework.jdbc.core.JdbcTemplate;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class EntitySchemaShapeTest {
    private static final Pattern TABLE_PATTERN = Pattern.compile(
            "CREATE TABLE `([^`]+)` \\((.*?)\\n\\);",
            Pattern.DOTALL
    );
    private static final Pattern COLUMN_PATTERN = Pattern.compile(
            "^\\s*`([^`]+)`\\s+([^\\r\\n]+)",
            Pattern.MULTILINE
    );

    @Autowired JdbcTemplate jdbcTemplate;
    @Autowired ResourceLoader resourceLoader;

    @Test
    void entityTablesUseOnlyColumnsFromAcceptedFlywaySchema() throws Exception {
        Map<String, Map<String, Boolean>> accepted = acceptedSchemaColumns();
        Set<String> entityTables = new LinkedHashSet<>(jdbcTemplate.queryForList(
                """
                SELECT table_name
                  FROM information_schema.tables
                 WHERE table_schema = 'public'
                   AND table_type = 'BASE TABLE'
                """,
                String.class
        ));

        assertThat(entityTables).isSubsetOf(accepted.keySet());
        for (String table : entityTables) {
            Map<String, Boolean> actualColumns = jdbcTemplate.query(
                    """
                    SELECT column_name, is_nullable
                      FROM information_schema.columns
                     WHERE table_schema = 'public'
                       AND table_name = ?
                    """,
                    resultSet -> {
                        Map<String, Boolean> columns = new LinkedHashMap<>();
                        while (resultSet.next()) {
                            columns.put(
                                    resultSet.getString("column_name"),
                                    "YES".equalsIgnoreCase(resultSet.getString("is_nullable"))
                            );
                        }
                        return columns;
                    },
                    table
            );
            assertThat(actualColumns.keySet())
                    .as("entity table %s columns", table)
                    .containsExactlyInAnyOrderElementsOf(accepted.get(table).keySet());
            for (Map.Entry<String, Boolean> column : accepted.get(table).entrySet()) {
                assertThat(actualColumns.get(column.getKey()))
                        .as("entity table %s column %s nullable", table, column.getKey())
                        .isEqualTo(column.getValue());
            }
        }
    }

    private Map<String, Map<String, Boolean>> acceptedSchemaColumns() throws Exception {
        String sql;
        try (var input = resourceLoader
                .getResource("classpath:db/migration/V1__baseline_schema.sql")
                .getInputStream()) {
            sql = new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
        Map<String, Map<String, Boolean>> tables = new LinkedHashMap<>();
        Matcher tableMatcher = TABLE_PATTERN.matcher(sql);
        while (tableMatcher.find()) {
            Map<String, Boolean> columns = new LinkedHashMap<>();
            Matcher columnMatcher = COLUMN_PATTERN.matcher(tableMatcher.group(2));
            while (columnMatcher.find()) {
                String definition = columnMatcher.group(2).toUpperCase(Locale.ROOT);
                columns.put(
                        columnMatcher.group(1).toLowerCase(Locale.ROOT),
                        !definition.contains("NOT NULL")
                );
            }
            tables.put(tableMatcher.group(1).toLowerCase(Locale.ROOT), columns);
        }
        Map<String, Boolean> gazeAnalysisColumns = tables.get("gaze_analysis_results");
        gazeAnalysisColumns.put("sentence_metrics", true);
        gazeAnalysisColumns.put("regressions", true);
        gazeAnalysisColumns.put("analysis_meta", true);
        Map<String, Boolean> testCurriculumColumns = tables.get("test_curriculums");
        testCurriculumColumns.put("recommendation_status", false);
        testCurriculumColumns.put("recommendation_error", true);
        testCurriculumColumns.put("recommendation_last_attempt_at", true);
        testCurriculumColumns.put("recommendation_retry_count", false);
        tables.get("daily_curriculums").put("source_test_curriculum_id", true);
        return tables;
    }
}
