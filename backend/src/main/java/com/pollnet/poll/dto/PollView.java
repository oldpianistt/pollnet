package com.pollnet.poll.dto;

import com.pollnet.poll.OpenAnswersVisibility;
import com.pollnet.poll.Poll;
import com.pollnet.poll.ResultsVisibility;
import com.pollnet.user.dto.UserSummary;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record PollView(
        UUID id,
        UserSummary author,
        String title,
        String description,
        ResultsVisibility resultsVisibility,
        OpenAnswersVisibility openAnswersVisibility,
        List<QuestionView> questions,
        OffsetDateTime createdAt,
        boolean viewerHasAnswered
) {
    public static PollView from(Poll poll, boolean viewerHasAnswered) {
        return new PollView(
                poll.getId(),
                UserSummary.from(poll.getAuthor()),
                poll.getTitle(),
                poll.getDescription(),
                poll.getResultsVisibility(),
                poll.getOpenAnswersVisibility(),
                poll.getQuestions().stream().map(QuestionView::from).toList(),
                poll.getCreatedAt(),
                viewerHasAnswered
        );
    }
}
