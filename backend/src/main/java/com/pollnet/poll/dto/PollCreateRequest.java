package com.pollnet.poll.dto;

import com.pollnet.poll.OpenAnswersVisibility;
import com.pollnet.poll.ResultsVisibility;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record PollCreateRequest(
        @NotBlank @Size(max = 280) String title,
        @Size(max = 2000) String description,
        ResultsVisibility resultsVisibility,
        OpenAnswersVisibility openAnswersVisibility,
        @NotEmpty @Size(min = 1, max = 20) @Valid List<QuestionCreateRequest> questions
) {
    public ResultsVisibility resultsVisibilityOrDefault() {
        return resultsVisibility != null ? resultsVisibility : ResultsVisibility.AFTER_VOTE;
    }

    public OpenAnswersVisibility openAnswersVisibilityOrDefault() {
        return openAnswersVisibility != null ? openAnswersVisibility : OpenAnswersVisibility.PUBLIC;
    }
}
