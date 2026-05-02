package com.pollnet.poll.dto;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AnswerEntry(
        @NotNull UUID questionId,
        @NotNull JsonNode payload
) {}
