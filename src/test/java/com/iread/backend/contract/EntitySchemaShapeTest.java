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
    private static final Pattern COLUMN_PATTERN = Pattern.compile("^\\s*`([^`]+)`\\s+", Pattern.MULTILINE);

    @Autowired JdbcTemplate jdbcTemplate;
    @Autowired ResourceLoader resourceLoader;

    @Test
    void entityTablesUseOnlyColumnsFromAcceptedFlywaySchema() throws Exception {
        Map<String, Set<String>> accepted = acceptedSchemaColumns();
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
            Set<String> actualColumns = new LinkedHashSet<>(jdbcTemplate.queryForList(
                    """
                    SELECT column_name
                      FROM information_schema.columns
                     WHERE table_schema = 'public'
                       AND table_name = ?
                    """,
                    String.class,
                    table
            ));
            assertThat(actualColumns)
                    .as("entity table %s columns", table)
                    .containsExactlyInAnyOrderElementsOf(accepted.get(table));
        }
    }

    private Map<String, Set<String>> acceptedSchemaColumns() throws Exception {
        String sql;
        try (var input = resourceLoader
                .getResource("classpath:db/migration/V1__baseline_schema.sql")
                .getInputStream()) {
            sql = new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
        Map<String, Set<String>> tables = new LinkedHashMap<>();
        Matcher tableMatcher = TABLE_PATTERN.matcher(sql);
        while (tableMatcher.find()) {
            Set<String> columns = new LinkedHashSet<>();
            Matcher columnMatcher = COLUMN_PATTERN.matcher(tableMatcher.group(2));
            while (columnMatcher.find()) {
                columns.add(columnMatcher.group(1).toLowerCase(Locale.ROOT));
            }
            tables.put(tableMatcher.group(1).toLowerCase(Locale.ROOT), columns);
        }
        return tables;
    }
}
