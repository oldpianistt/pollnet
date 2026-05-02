package com.pollnet.poll.question.handler;

import com.pollnet.common.error.ApiException;
import com.pollnet.poll.question.QuestionType;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Registry: lookup by {@link QuestionType}. Spring injects every handler bean.
 * A startup check ensures we have exactly one handler per type — catches mistakes
 * like forgetting to declare a new type's handler as @Component.
 */
@Component
@RequiredArgsConstructor
public class QuestionTypeHandlers {

    private final List<QuestionTypeHandler> handlers;
    private final Map<QuestionType, QuestionTypeHandler> byType = new EnumMap<>(QuestionType.class);

    @PostConstruct
    void index() {
        for (QuestionTypeHandler h : handlers) {
            QuestionTypeHandler prev = byType.put(h.type(), h);
            if (prev != null) {
                throw new IllegalStateException("Duplicate handler for " + h.type());
            }
        }
        for (QuestionType t : QuestionType.values()) {
            if (!byType.containsKey(t)) {
                throw new IllegalStateException("Missing handler for question type " + t);
            }
        }
    }

    public QuestionTypeHandler forType(QuestionType type) {
        QuestionTypeHandler h = byType.get(type);
        if (h == null) throw ApiException.badRequest("UNKNOWN_QUESTION_TYPE", "Unknown question type: " + type);
        return h;
    }
}
