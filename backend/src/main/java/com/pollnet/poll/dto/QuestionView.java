package com.pollnet.poll.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.pollnet.poll.question.Question;
import com.pollnet.poll.question.QuestionType;

import java.util.UUID;

public record QuestionView(
        UUID id,
        QuestionType type,
        String prompt,
        JsonNode payload,
        int position
) {
    public static QuestionView from(Question q) {
        return new QuestionView(q.getId(), q.getType(), q.getPrompt(), q.getPayload(), q.getPosition());
    }
}
