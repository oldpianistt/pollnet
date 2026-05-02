package com.pollnet.poll.question.handler;

import com.fasterxml.jackson.databind.JsonNode;
import com.pollnet.common.error.ApiException;
import com.pollnet.poll.answer.Answer;
import com.pollnet.poll.question.Question;
import com.pollnet.poll.question.QuestionType;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class LikertHandler implements QuestionTypeHandler {

    @Override public QuestionType type() { return QuestionType.LIKERT; }

    @Override
    public void validateQuestionPayload(JsonNode payload) {
        int scale = payload.path("scale").asInt(0);
        if (scale < 3 || scale > 11) {
            throw ApiException.badRequest("INVALID_QUESTION", "LIKERT scale must be 3-11");
        }
    }

    @Override
    public void validateAnswerPayload(JsonNode questionPayload, JsonNode answerPayload) {
        int scale = questionPayload.get("scale").asInt();
        JsonNode v = answerPayload.get("value");
        if (v == null || !v.isInt() || v.asInt() < 1 || v.asInt() > scale) {
            throw ApiException.badRequest("INVALID_ANSWER", "value must be 1.." + scale);
        }
    }

    @Override
    public Map<String, Object> computeResults(Question question, List<Answer> answers, boolean includeOpenAnswers) {
        int scale = question.getPayload().get("scale").asInt();
        int[] dist = new int[scale + 1]; // 1-indexed; index 0 unused
        long sum = 0;
        for (Answer a : answers) {
            int v = a.getPayload().get("value").asInt();
            if (v >= 1 && v <= scale) {
                dist[v]++;
                sum += v;
            }
        }
        int n = answers.size();
        double mean = n == 0 ? 0.0 : (double) sum / n;
        double variance = 0.0;
        if (n > 1) {
            double v = 0.0;
            for (int i = 1; i <= scale; i++) {
                double diff = i - mean;
                v += diff * diff * dist[i];
            }
            variance = v / (n - 1);
        }
        Map<String, Integer> distMap = new HashMap<>();
        for (int i = 1; i <= scale; i++) distMap.put(String.valueOf(i), dist[i]);

        Map<String, Object> out = new HashMap<>();
        out.put("type", "LIKERT");
        out.put("scale", scale);
        out.put("count", n);
        out.put("mean", mean);
        out.put("stddev", Math.sqrt(variance));
        out.put("distribution", distMap);
        return out;
    }
}
