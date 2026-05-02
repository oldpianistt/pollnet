package com.pollnet.poll.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record AnswersSubmitRequest(
        @NotEmpty @Valid List<AnswerEntry> answers
) {}
