package com.pollnet.poll.question.handler;

import com.fasterxml.jackson.databind.JsonNode;
import com.pollnet.common.error.ApiException;
import com.pollnet.poll.answer.Answer;
import com.pollnet.poll.question.Question;
import com.pollnet.poll.question.QuestionType;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class RankingHandler implements QuestionTypeHandler {

    @Override public QuestionType type() { return QuestionType.RANKING; }

    @Override
    public void validateQuestionPayload(JsonNode payload) {
        JsonNode options = payload.get("options");
        if (options == null || !options.isArray() || options.size() < 2 || options.size() > 10) {
            throw ApiException.badRequest("INVALID_QUESTION", "RANKING requires 2-10 'options'");
        }
        for (JsonNode opt : options) {
            if (!opt.isTextual() || opt.asText().isBlank() || opt.asText().length() > 200) {
                throw ApiException.badRequest("INVALID_QUESTION", "Each option must be 1-200 chars");
            }
        }
    }

    @Override
    public void validateAnswerPayload(JsonNode questionPayload, JsonNode answerPayload) {
        JsonNode ranking = answerPayload.get("ranking");
        int size = questionPayload.get("options").size();
        if (ranking == null || !ranking.isArray() || ranking.size() != size) {
            throw ApiException.badRequest("INVALID_ANSWER", "ranking must be a permutation of size " + size);
        }
        Set<Integer> seen = new HashSet<>();
        for (JsonNode v : ranking) {
            if (!v.isInt() || v.asInt() < 0 || v.asInt() >= size || !seen.add(v.asInt())) {
                throw ApiException.badRequest("INVALID_ANSWER", "ranking must be a permutation of 0.." + (size - 1));
            }
        }
    }

    @Override
    public Map<String, Object> computeResults(Question question, List<Answer> answers, boolean includeOpenAnswers) {
        JsonNode options = question.getPayload().get("options");
        int n = options.size();
        long[] bordaScore = new long[n];
        long[] positionSum = new long[n];
        int voters = 0;

        for (Answer a : answers) {
            JsonNode ranking = a.getPayload().get("ranking");
            if (ranking == null || ranking.size() != n) continue;
            voters++;
            for (int pos = 0; pos < n; pos++) {
                int optionIndex = ranking.get(pos).asInt();
                bordaScore[optionIndex] += (n - 1 - pos); // 1st place gets n-1, last gets 0
                positionSum[optionIndex] += pos + 1;      // 1-indexed position for avg display
            }
        }

        List<Map<String, Object>> rows = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            rows.add(Map.of(
                    "index", i,
                    "label", options.get(i).asText(),
                    "bordaScore", bordaScore[i],
                    "averagePosition", voters == 0 ? null : (double) positionSum[i] / voters
            ));
        }
        rows.sort(Comparator.comparingLong((Map<String, Object> m) -> -((Number) m.get("bordaScore")).longValue()));
        return Map.of("type", "RANKING", "totalVoters", voters, "options", rows);
    }
}
