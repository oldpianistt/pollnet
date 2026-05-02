package com.pollnet.common.error;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.OffsetDateTime;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiError(
        OffsetDateTime timestamp,
        int status,
        String code,
        String message,
        String path,
        List<FieldViolation> errors
) {
    public static ApiError of(int status, String code, String message, String path) {
        return new ApiError(OffsetDateTime.now(), status, code, message, path, null);
    }

    public static ApiError validation(int status, String message, String path, List<FieldViolation> errors) {
        return new ApiError(OffsetDateTime.now(), status, "VALIDATION_FAILED", message, path, errors);
    }

    public record FieldViolation(String field, String message) {}
}
