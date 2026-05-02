package com.pollnet.poll.dto;

import com.pollnet.poll.question.QuestionType;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public record ResultsView(
        UUID pollId,
        List<QuestionResults> results
) {
    public record QuestionResults(
            UUID questionId,
            QuestionType type,
            String prompt,
            Map<String, Object> data
    ) {}
}
