package com.pollnet.poll.question.handler;

import com.fasterxml.jackson.databind.JsonNode;
import com.pollnet.poll.answer.Answer;
import com.pollnet.poll.question.Question;
import com.pollnet.poll.question.QuestionType;

import java.util.List;
import java.util.Map;

/**
 * Per-type strategy: validates payloads when a question is created, validates an
 * answer payload against the question, and aggregates a list of answers into a
 * type-specific results view.
 */
public interface QuestionTypeHandler {

    QuestionType type();

    /** Throws ApiException(400, ...) on invalid payload. */
    void validateQuestionPayload(JsonNode payload);

    /** Throws ApiException(400, ...) on invalid answer. */
    void validateAnswerPayload(JsonNode questionPayload, JsonNode answerPayload);

    /**
     * Aggregate the given answers into a result map. {@code includeOpenAnswers}
     * is consulted only by the OPEN type; ignored by everyone else.
     */
    Map<String, Object> computeResults(Question question, List<Answer> answers, boolean includeOpenAnswers);
}
