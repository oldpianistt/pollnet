package com.pollnet.poll;

import com.pollnet.common.error.ApiException;
import com.pollnet.poll.answer.AnswerRepository;
import com.pollnet.poll.dto.PollCreateRequest;
import com.pollnet.poll.dto.PollView;
import com.pollnet.poll.dto.QuestionCreateRequest;
import com.pollnet.poll.question.Question;
import com.pollnet.poll.question.handler.QuestionTypeHandlers;
import com.pollnet.user.User;
import com.pollnet.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PollService {

    private final PollRepository pollRepository;
    private final UserRepository userRepository;
    private final AnswerRepository answerRepository;
    private final QuestionTypeHandlers handlers;

    @Transactional
    public PollView create(UUID authorId, PollCreateRequest req) {
        User author = userRepository.findById(authorId)
                .orElseThrow(() -> ApiException.unauthorized("UNAUTHENTICATED", "User not found"));

        Poll poll = Poll.builder()
                .author(author)
                .title(req.title().trim())
                .description(req.description())
                .resultsVisibility(req.resultsVisibilityOrDefault())
                .openAnswersVisibility(req.openAnswersVisibilityOrDefault())
                .build();

        int pos = 0;
        for (QuestionCreateRequest qr : req.questions()) {
            handlers.forType(qr.type()).validateQuestionPayload(qr.payload());
            Question q = Question.builder()
                    .type(qr.type())
                    .prompt(qr.prompt().trim())
                    .payload(qr.payload())
                    .position(pos++)
                    .build();
            poll.addQuestion(q);
        }

        Poll saved = pollRepository.save(poll);
        return PollView.from(saved, false);
    }

    @Transactional(readOnly = true)
    public PollView get(UUID pollId, UUID viewerId) {
        Poll poll = pollRepository.findById(pollId)
                .orElseThrow(() -> ApiException.notFound("POLL_NOT_FOUND", "Poll not found"));
        boolean answered = viewerId != null
                && answerRepository.existsByQuestionPollIdAndUserId(pollId, viewerId);
        return PollView.from(poll, answered);
    }

    @Transactional
    public void delete(UUID pollId, UUID requesterId) {
        Poll poll = pollRepository.findById(pollId)
                .orElseThrow(() -> ApiException.notFound("POLL_NOT_FOUND", "Poll not found"));
        if (!poll.getAuthor().getId().equals(requesterId)) {
            throw ApiException.forbidden("NOT_POLL_OWNER", "Only the author can delete a poll");
        }
        pollRepository.delete(poll);
    }

    @Transactional(readOnly = true)
    public List<PollView> listByAuthor(String username, UUID viewerId) {
        User author = userRepository.findByUsername(username)
                .orElseThrow(() -> ApiException.notFound("USER_NOT_FOUND", "User not found"));
        return pollRepository
                .findByAuthorIdOrderByCreatedAtDesc(author.getId(), org.springframework.data.domain.PageRequest.of(0, 50))
                .stream()
                .map(p -> PollView.from(p, viewerId != null
                        && answerRepository.existsByQuestionPollIdAndUserId(p.getId(), viewerId)))
                .toList();
    }
}
