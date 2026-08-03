package com.iread.backend.exception;

import com.iread.backend.ai.exception.AiClientException;
import com.iread.backend.report.admin.exception.ReportCreationException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {
    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void mapsMissingResourceToOpenApiNotFoundError() {
        var response = handler.handleNotFound(
                new ResourceNotFoundException("학생을 찾을 수 없습니다.")
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().error().code()).isEqualTo("RESOURCE_NOT_FOUND");
        assertThat(response.getBody().error().message()).isEqualTo(
                "학생을 찾을 수 없습니다."
        );
    }

    @Test
    void preservesDomainSpecificMissingResourceCode() {
        var response = handler.handleNotFound(
                new ResourceNotFoundException(
                        "NEXT_CURRICULUM_NOT_FOUND",
                        "수정 가능한 커리큘럼을 찾을 수 없습니다."
                )
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().error().code()).isEqualTo("NEXT_CURRICULUM_NOT_FOUND");
    }

    @Test
    void mapsStateConflictToConflictInsteadOfUnauthorized() {
        var response = handler.handleConflict(
                new ConflictException("이미 시작한 훈련입니다.")
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().error().code()).isEqualTo("CONFLICT");
    }

    @Test
    void mapsDuplicateReportToSpecificCodeAndExistingReportId() {
        var response = handler.handleReportCreation(
                ReportCreationException.periodAlreadyExists(25L)
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().error().code())
                .isEqualTo("REPORT_PERIOD_ALREADY_EXISTS");
        assertThat(response.getBody().error().details())
                .containsEntry("existingReportId", 25L);
    }

    @Test
    void doesNotExposeUnexpectedExceptionMessage() {
        var response = handler.handleUnexpected(
                new RuntimeException("database-password=secret")
        );

        assertThat(response.getStatusCode())
                .isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().error().code()).isEqualTo("INTERNAL_ERROR");
        assertThat(response.getBody().error().message()).doesNotContain("secret");
    }

    @Test
    void mapsAiFailureToBadGatewayWithoutExposingUpstreamDetails() {
        var response = handler.handleAiClient(
                new AiClientException("upstream api-key=secret", 500)
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
        assertThat(response.getBody().error().code()).isEqualTo("AI_UPSTREAM_ERROR");
        assertThat(response.getBody().error().message()).doesNotContain("secret");
    }

    @Test
    void mapsInternalStateFailureToSanitizedServerError() {
        var response = handler.handleInvalidState(
                new IllegalStateException("audio-path=C:/private/student")
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().error().code()).isEqualTo("INTERNAL_ERROR");
        assertThat(response.getBody().error().message()).doesNotContain("private");
    }

    @Test
    void mapsOversizedMultipartToInvalidRequestEnvelope() {
        var response = handler.handleMalformedRequest(
                new MaxUploadSizeExceededException(20L * 1024 * 1024)
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().error().code()).isEqualTo("INVALID_REQUEST");
        assertThat(response.getBody().error().message()).doesNotContain("20971520");
    }

    @Test
    void mapsUnsupportedMultipartPartTypeToInvalidRequestEnvelope() {
        var response = handler.handleMalformedRequest(
                new HttpMediaTypeNotSupportedException("application/octet-stream")
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().error().code()).isEqualTo("INVALID_REQUEST");
    }
}
