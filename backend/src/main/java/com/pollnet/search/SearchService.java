package com.pollnet.search;

import com.pollnet.common.error.ApiException;
import com.pollnet.common.pagination.Cursor;
import com.pollnet.common.pagination.CursorPage;
import com.pollnet.poll.Poll;
import com.pollnet.poll.PollRepository;
import com.pollnet.poll.answer.AnswerRepository;
import com.pollnet.poll.dto.PollView;
import com.pollnet.user.UserRepository;
import com.pollnet.user.dto.UserSummary;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SearchService {

    static final int MIN_QUERY_LEN = 2;
    static final int MAX_QUERY_LEN = 80;

    private final PollRepository pollRepository;
    private final UserRepository userRepository;
    private final AnswerRepository answerRepository;

    @Transactional(readOnly = true)
    public CursorPage<PollView> searchPolls(String q, int limit, UUID viewerId) {
        String query = sanitize(q);
        Slice<Poll> slice = pollRepository.searchByText(query,
                PageRequest.of(0, Math.min(Math.max(limit, 1), 50)));

        List<PollView> items = slice.getContent().stream()
                .map(p -> PollView.from(p, viewerId != null
                        && answerRepository.existsByQuestionPollIdAndUserId(p.getId(), viewerId)))
                .toList();
        String next = null;
        if (slice.hasNext() && !items.isEmpty()) {
            Poll last = slice.getContent().get(slice.getContent().size() - 1);
            next = new Cursor(last.getCreatedAt(), last.getId()).encode();
        }
        return CursorPage.of(items, next);
    }

    @Transactional(readOnly = true)
    public List<UserSummary> searchUsers(String q, int limit) {
        String query = sanitize(q);
        return userRepository.searchByText(query,
                PageRequest.of(0, Math.min(Math.max(limit, 1), 50)))
                .stream()
                .map(UserSummary::from)
                .toList();
    }

    static String sanitize(String q) {
        String trimmed = q == null ? "" : q.trim();
        if (trimmed.length() < MIN_QUERY_LEN) {
            throw ApiException.badRequest("QUERY_TOO_SHORT", "En az " + MIN_QUERY_LEN + " karakter girin");
        }
        if (trimmed.length() > MAX_QUERY_LEN) {
            throw ApiException.badRequest("QUERY_TOO_LONG", "En fazla " + MAX_QUERY_LEN + " karakter");
        }
        return trimmed;
    }
}
