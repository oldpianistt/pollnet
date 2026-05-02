package com.pollnet.poll.question.handler;

import com.fasterxml.jackson.databind.JsonNode;
import com.pollnet.common.error.ApiException;
import com.pollnet.poll.answer.Answer;
import com.pollnet.poll.question.Question;
import com.pollnet.poll.question.QuestionType;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class MultipleChoiceHandler implements QuestionTypeHandler {

    @Override public QuestionType type() { return QuestionType.MULTIPLE; }

    @Override
    public void validateQuestionPayload(JsonNode payload) {
        JsonNode options = payload.get("options");
        if (options == null || !options.isArray() || options.size() < 2 || options.size() > 20) {
            throw ApiException.badRequest("INVALID_QUESTION", "MULTIPLE requires 2-20 'options'");
        }
        for (JsonNode opt : options) {
            if (!opt.isTextual() || opt.asText().isBlank() || opt.asText().length() > 200) {
                throw ApiException.badRequest("INVALID_QUESTION", "Each option must be 1-200 chars");
            }
        }
        int min = payload.path("minSelect").asInt(1);
        int max = payload.path("maxSelect").asInt(options.size());
        if (min < 1 || max < min || max > options.size()) {
            throw ApiException.badRequest("INVALID_QUESTION",
                    "minSelect/maxSelect out of range (1 <= min <= max <= options.size)");
        }
    }

    @Override
    public void validateAnswerPayload(JsonNode questionPayload, JsonNode answerPayload) {
        JsonNode arr = answerPayload.get("selectedIndices");
        if (arr == null || !arr.isArray()) {
            throw ApiException.badRequest("INVALID_ANSWER", "selectedIndices must be an array");
        }
        int size = questionPayload.get("options").size();
        int min = questionPayload.path("minSelect").asInt(1);
        int max = questionPayload.path("maxSelect").asInt(size);
        if (arr.size() < min || arr.size() > max) {
            throw ApiException.badRequest("INVALID_ANSWER",
                    "selectedIndices length must be between " + min + " and " + max);
        }
        Set<Integer> seen = new HashSet<>();
        for (JsonNode v : arr) {
            if (!v.isInt() || v.asInt() < 0 || v.asInt() >= size) {
                throw ApiException.badRequest("INVALID_ANSWER", "selectedIndices contains out-of-range value");
            }
            if (!seen.add(v.asInt())) {
                throw ApiException.badRequest("INVALID_ANSWER", "selectedIndices contains duplicates");
            }
        }
    }

    @Override
    public Map<String, Object> computeResults(Question question, List<Answer> answers, boolean includeOpenAnswers) {
        JsonNode options = question.getPayload().get("options");
        int[] counts = new int[options.size()];
        for (Answer a : answers) {
            for (JsonNode v : a.getPayload().get("selectedIndices")) {
                int i = v.asInt();
                if (i >= 0 && i < counts.length) counts[i]++;
            }
        }
        int voters = answers.size();
        List<Map<String, Object>> rows = new ArrayList<>(options.size());
        for (int i = 0; i < options.size(); i++) {
            rows.add(Map.of(
                    "index", i,
                    "label", options.get(i).asText(),
                    "count", counts[i],
                    "percent", voters == 0 ? 0.0 : (counts[i] * 100.0 / voters)
            ));
        }
        return Map.of("type", "MULTIPLE", "totalVoters", voters, "options", rows);
    }
}
