package com.pollnet.poll.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.pollnet.poll.question.QuestionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record QuestionCreateRequest(
        @NotNull QuestionType type,
        @NotBlank @Size(max = 500) String prompt,
        @NotNull JsonNode payload
) {}
