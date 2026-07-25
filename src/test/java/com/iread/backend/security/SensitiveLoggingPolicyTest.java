package com.iread.backend.security;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class SensitiveLoggingPolicyTest {

    @Test
    void 요청상세와AccessLog를비활성화한다() throws IOException {
        String properties = Files.readString(Path.of("src/main/resources/application.properties"));

        assertThat(properties)
                .contains("spring.mvc.log-request-details=false")
                .contains("server.tomcat.accesslog.enabled=false");
    }

    @Test
    void 애플리케이션소스에서직접콘솔출력을사용하지않는다() throws IOException {
        try (Stream<Path> paths = Files.walk(Path.of("src/main/java"))) {
            String source = paths
                    .filter(path -> path.toString().endsWith(".java"))
                    .map(this::read)
                    .reduce("", (left, right) -> left + "\n" + right);

            assertThat(source)
                    .doesNotContain("System.out")
                    .doesNotContain("System.err")
                    .doesNotContain(".printStackTrace(");
        }
    }

    @Test
    void 제거된외부공유보고서경로를익명허용하지않는다() throws IOException {
        String securityConfig = Files.readString(
                Path.of("src/main/java/com/iread/backend/security/SecurityConfig.java")
        );

        assertThat(securityConfig)
                .doesNotContain("/api/admin/report/shared/**")
                .contains(".requestMatchers(\"/api/auth/admin/logout\", \"/api/admin/**\")"
                        + ".hasAuthority(\"AUD_admin-app\")");
    }

    private String read(Path path) {
        try {
            return Files.readString(path);
        } catch (IOException exception) {
            throw new IllegalStateException("소스 파일을 읽을 수 없습니다: " + path, exception);
        }
    }
}
