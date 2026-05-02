package com.pollnet.poll.question.handler;

import com.fasterxml.jackson.databind.JsonNode;
import com.pollnet.common.error.ApiException;
import com.pollnet.poll.answer.Answer;
import com.pollnet.poll.question.Question;
import com.pollnet.poll.question.QuestionType;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class SingleChoiceHandler implements QuestionTypeHandler {

    @Override public QuestionType type() { return QuestionType.SINGLE; }

    @Override
    public void validateQuestionPayload(JsonNode payload) {
        JsonNode options = payload.get("options");
        if (options == null || !options.isArray() || options.size() < 2 || options.size() > 20) {
            throw ApiException.badRequest("INVALID_QUESTION", "SINGLE requires 2-20 'options'");
        }
        for (JsonNode opt : options) {
            if (!opt.isTextual() || opt.asText().isBlank() || opt.asText().length() > 200) {
                throw ApiException.badRequest("INVALID_QUESTION", "Each option must be 1-200 chars");
            }
        }
    }

    @Override
    public void validateAnswerPayload(JsonNode questionPayload, JsonNode answerPayload) {
        JsonNode idx = answerPayload.get("selectedIndex");
        int size = questionPayload.get("options").size();
        if (idx == null || !idx.isInt() || idx.asInt() < 0 || idx.asInt() >= size) {
            throw ApiException.badRequest("INVALID_ANSWER", "selectedIndex out of range");
        }
    }

    @Override
    public Map<String, Object> computeResults(Question question, List<Answer> answers, boolean includeOpenAnswers) {
        JsonNode options = question.getPayload().get("options");
        int[] counts = new int[options.size()];
        for (Answer a : answers) {
            int i = a.getPayload().get("selectedIndex").asInt();
            if (i >= 0 && i < counts.length) counts[i]++;
        }
        int total = answers.size();
        List<Map<String, Object>> rows = new ArrayList<>(options.size());
        for (int i = 0; i < options.size(); i++) {
            rows.add(Map.of(
                    "index", i,
                    "label", options.get(i).asText(),
                    "count", counts[i],
                    "percent", total == 0 ? 0.0 : (counts[i] * 100.0 / total)
            ));
        }
        return Map.of("type", "SINGLE", "totalVotes", total, "options", rows);
    }
}
