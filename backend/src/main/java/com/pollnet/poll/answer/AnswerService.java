package com.pollnet.poll.answer;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.pollnet.common.error.ApiException;
import com.pollnet.notification.NotificationService;
import com.pollnet.notification.NotificationType;
import com.pollnet.poll.OpenAnswersVisibility;
import com.pollnet.poll.Poll;
import com.pollnet.poll.PollRepository;
import com.pollnet.poll.ResultsVisibility;
import com.pollnet.poll.dto.AnswerEntry;
import com.pollnet.poll.dto.AnswersSubmitRequest;
import com.pollnet.poll.dto.ResultsView;
import com.pollnet.poll.question.Question;
import com.pollnet.poll.question.QuestionType;
import com.pollnet.poll.question.handler.QuestionTypeHandlers;
import com.pollnet.user.User;
import com.pollnet.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AnswerService {

    private final PollRepository pollRepository;
    private final AnswerRepository answerRepository;
    private final UserRepository userRepository;
    private final QuestionTypeHandlers handlers;
    private final NotificationService notifications;

    /**
     * Submit answers to all questions of a poll, atomically. The request must contain
     * exactly one entry per question. Re-voting is rejected (the unique (question,user)
     * constraint already enforces it; we surface a clean 409 first).
     */
    @Transactional
    public void submit(UUID pollId, UUID userId, AnswersSubmitRequest req) {
        Poll poll = pollRepository.findById(pollId)
                .orElseThrow(() -> ApiException.notFound("POLL_NOT_FOUND", "Poll not found"));

        if (answerRepository.existsByQuestionPollIdAndUserId(pollId, userId)) {
            throw ApiException.conflict("ALREADY_ANSWERED", "You have already answered this poll");
        }

        Map<UUID, Question> qById = new HashMap<>();
        for (Question q : poll.getQuestions()) qById.put(q.getId(), q);

        if (req.answers().size() != qById.size()) {
            throw ApiException.badRequest("ANSWER_COUNT_MISMATCH",
                    "Expected " + qById.size() + " answers, got " + req.answers().size());
        }

        Set<UUID> seen = new HashSet<>();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> ApiException.unauthorized("UNAUTHENTICATED", "User not found"));

        for (AnswerEntry entry : req.answers()) {
            Question q = qById.get(entry.questionId());
            if (q == null) {
                throw ApiException.badRequest("UNKNOWN_QUESTION",
                        "Question " + entry.questionId() + " does not belong to this poll");
            }
            if (!seen.add(entry.questionId())) {
                throw ApiException.badRequest("DUPLICATE_ANSWER",
                        "Question " + entry.questionId() + " answered twice");
            }
            handlers.forType(q.getType())
                    .validateAnswerPayload(q.getPayload(), entry.payload());

            Answer answer = Answer.builder()
                    .question(q)
                    .user(user)
                    .payload(entry.payload())
                    .build();
            answerRepository.save(answer);
        }

        // Notify the poll's author once per submission, not per question.
        ObjectNode payload = notifications.payload();
        payload.put("actorUsername", user.getUsername());
        if (user.getDisplayName() != null) payload.put("actorDisplayName", user.getDisplayName());
        payload.put("pollId", poll.getId().toString());
        payload.put("pollTitle", poll.getTitle());
        notifications.notify(poll.getAuthor().getId(), NotificationType.POLL_ANSWERED, payload, userId);
    }

    @Transactional(readOnly = true)
    public ResultsView resultsFor(UUID pollId, UUID viewerId) {
        Poll poll = pollRepository.findById(pollId)
                .orElseThrow(() -> ApiException.notFound("POLL_NOT_FOUND", "Poll not found"));

        boolean isAuthor = viewerId != null && poll.getAuthor().getId().equals(viewerId);
        ResultsVisibility vis = poll.getResultsVisibility();

        if (vis == ResultsVisibility.AUTHOR_ONLY && !isAuthor) {
            throw ApiException.forbidden("RESULTS_HIDDEN", "Only the poll author can view results");
        }
        if (vis == ResultsVisibility.AFTER_VOTE && !isAuthor) {
            if (viewerId == null
                    || !answerRepository.existsByQuestionPollIdAndUserId(pollId, viewerId)) {
                throw ApiException.forbidden("VOTE_REQUIRED", "Vote first to view results");
            }
        }
        // ALWAYS → no extra check.

        boolean includeOpen = isAuthor
                || poll.getOpenAnswersVisibility() == OpenAnswersVisibility.PUBLIC;

        List<ResultsView.QuestionResults> rows = poll.getQuestions().stream()
                .map(q -> {
                    List<Answer> answers = answerRepository.findByQuestionId(q.getId());
                    Map<String, Object> data = handlers.forType(q.getType())
                            .computeResults(q, answers, includeOpen && q.getType() == QuestionType.OPEN);
                    return new ResultsView.QuestionResults(q.getId(), q.getType(), q.getPrompt(), data);
                })
                .toList();

        return new ResultsView(pollId, rows);
    }
}
