package com.iread.backend.ai.dto.res;

public record StoryBranchInputReviewResponse(
        String requestId,
        Decision decision,
        ReasonCode reasonCode,
        String policyVersion
) {
    public enum Decision {
        ALLOW,
        CONFIRM,
        RETRY,
        BLOCK
    }

    public enum ReasonCode {
        OK,
        AMBIGUOUS,
        OFF_TOPIC,
        SELF_HARM,
        SEXUAL,
        SEVERE_VIOLENCE,
        THREAT,
        HATE_HARASSMENT,
        PII,
        INJECTION
    }

    public boolean mayConfirm() {
        return decision == Decision.ALLOW || decision == Decision.CONFIRM;
    }
}
