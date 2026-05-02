package com.pollnet.alice;

import java.util.List;

/**
 * Shape returned by Alice for the frontend to seed the poll-create form.
 * Mirrors PollCreateRequest closely but stays a separate type so we can
 * loosen validation and pre-fill: e.g. ranking options, likert scale defaults.
 */
public record AlicePollDraft(
        String title,
        String description,
        List<QuestionDraft> questions
) {
    public record QuestionDraft(
            String type,         // SINGLE | MULTIPLE | LIKERT | RANKING | OPEN
            String prompt,
            PayloadDraft payload
    ) {}

    public record PayloadDraft(
            List<String> options,
            Integer minSelect,
            Integer maxSelect,
            Integer scale,
            String  leftLabel,
            String  rightLabel,
            Integer maxLength
    ) {}
}
