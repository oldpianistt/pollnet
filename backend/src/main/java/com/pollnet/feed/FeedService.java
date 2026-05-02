package com.pollnet.feed;

import com.pollnet.common.error.ApiException;
import com.pollnet.common.pagination.Cursor;
import com.pollnet.common.pagination.CursorPage;
import com.pollnet.follow.FollowService;
import com.pollnet.poll.Poll;
import com.pollnet.poll.PollRepository;
import com.pollnet.poll.answer.AnswerRepository;
import com.pollnet.poll.dto.PollView;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FeedService {

    private final PollRepository pollRepository;
    private final AnswerRepository answerRepository;
    private final FollowService followService;

    @Transactional(readOnly = true)
    public CursorPage<PollView> discover(String cursorParam, int limit, UUID viewerId) {
        Cursor cursor = Cursor.decode(cursorParam);
        Slice<Poll> slice = pollRepository.findDiscoverFeed(
                cursor.time(), cursor.id(), PageRequest.of(0, normalizeLimit(limit)));
        return mapSlice(slice, viewerId);
    }

    @Transactional(readOnly = true)
    public CursorPage<PollView> following(String cursorParam, int limit, UUID viewerId) {
        if (viewerId == null) {
            throw ApiException.unauthorized("UNAUTHENTICATED", "Authentication required for following feed");
        }
        List<UUID> followeeIds = followService.followeeIdsOf(viewerId);
        if (followeeIds.isEmpty()) {
            return CursorPage.empty();
        }
        Cursor cursor = Cursor.decode(cursorParam);
        Slice<Poll> slice = pollRepository.findFollowingFeed(
                followeeIds, cursor.time(), cursor.id(), PageRequest.of(0, normalizeLimit(limit)));
        return mapSlice(slice, viewerId);
    }

    private CursorPage<PollView> mapSlice(Slice<Poll> slice, UUID viewerId) {
        List<PollView> items = slice.getContent().stream()
                .map(p -> PollView.from(p, viewerId != null
                        && answerRepository.existsByQuestionPollIdAndUserId(p.getId(), viewerId)))
                .toList();
        String next = null;
        if (slice.hasNext() && !slice.getContent().isEmpty()) {
            Poll last = slice.getContent().get(slice.getContent().size() - 1);
            next = new Cursor(last.getCreatedAt(), last.getId()).encode();
        }
        return CursorPage.of(items, next);
    }

    private static int normalizeLimit(int limit) {
        return Math.min(Math.max(limit, 1), 50);
    }
}
