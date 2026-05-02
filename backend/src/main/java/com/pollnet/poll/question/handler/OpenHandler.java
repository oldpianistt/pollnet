package com.pollnet.poll.question.handler;

import com.fasterxml.jackson.databind.JsonNode;
import com.pollnet.common.error.ApiException;
import com.pollnet.poll.answer.Answer;
import com.pollnet.poll.question.Question;
import com.pollnet.poll.question.QuestionType;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class OpenHandler implements QuestionTypeHandler {

    private static final int DEFAULT_MAX_LENGTH = 500;

    @Override public QuestionType type() { return QuestionType.OPEN; }

    @Override
    public void validateQuestionPayload(JsonNode payload) {
        int max = payload.path("maxLength").asInt(DEFAULT_MAX_LENGTH);
        if (max < 1 || max > 2000) {
            throw ApiException.badRequest("INVALID_QUESTION", "maxLength must be 1-2000");
        }
    }

    @Override
    public void validateAnswerPayload(JsonNode questionPayload, JsonNode answerPayload) {
        int max = questionPayload.path("maxLength").asInt(DEFAULT_MAX_LENGTH);
        JsonNode text = answerPayload.get("text");
        if (text == null || !text.isTextual() || text.asText().isBlank() || text.asText().length() > max) {
            throw ApiException.badRequest("INVALID_ANSWER", "text must be 1-" + max + " chars");
        }
    }

    @Override
    public Map<String, Object> computeResults(Question question, List<Answer> answers, boolean includeOpenAnswers) {
        Map<String, Object> out = new HashMap<>();
        out.put("type", "OPEN");
        out.put("count", answers.size());

        if (includeOpenAnswers) {
            List<Answer> sorted = new ArrayList<>(answers);
            sorted.sort(Comparator.comparing(Answer::getCreatedAt).reversed());
            List<Map<String, Object>> items = new ArrayList<>(sorted.size());
            for (Answer a : sorted) {
                items.add(Map.of(
                        "answerId", a.getId(),
                        "text", a.getPayload().get("text").asText(),
                        "createdAt", a.getCreatedAt()
                ));
            }
            out.put("items", items);
        }
        return out;
    }
}
