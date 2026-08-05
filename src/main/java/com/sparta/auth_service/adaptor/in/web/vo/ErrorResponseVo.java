package com.sparta.auth_service.adaptor.in.web.vo;

import lombok.Builder;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;

import java.time.Instant;
import java.util.List;

/** API 공통 Error Response — timestamp·status·code·message·path (Validation 시 fieldErrors) */
@Getter
@Jacksonized
@Builder
public class ErrorResponseVo {

    private final Instant timestamp;
    private final int status;
    private final String code;
    private final String message;
    private final String path;
    private final Long retryAfterSeconds;
    private final List<FieldErrorVo> fieldErrors;

    @Getter
    @Jacksonized
    @Builder
    public static class FieldErrorVo {
        private final String field;
        private final String code;
        private final String message;
    }
}
