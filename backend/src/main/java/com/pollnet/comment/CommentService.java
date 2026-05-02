package com.pollnet.comment;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.pollnet.comment.dto.CommentCreateRequest;
import com.pollnet.comment.dto.CommentView;
import com.pollnet.common.error.ApiException;
import com.pollnet.common.pagination.Cursor;
import com.pollnet.common.pagination.CursorPage;
import com.pollnet.notification.NotificationService;
import com.pollnet.notification.NotificationType;
import com.pollnet.poll.Poll;
import com.pollnet.poll.PollRepository;
import com.pollnet.user.User;
import com.pollnet.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;
    private final PollRepository pollRepository;
    private final UserRepository userRepository;
    private final NotificationService notifications;

    @Transactional
    public CommentView create(UUID pollId, UUID userId, CommentCreateRequest req) {
        Poll poll = pollRepository.findById(pollId)
                .orElseThrow(() -> ApiException.notFound("POLL_NOT_FOUND", "Poll not found"));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> ApiException.unauthorized("UNAUTHENTICATED", "User not found"));

        Comment c = Comment.builder()
                .poll(poll)
                .user(user)
                .body(req.body())
                .build();
        commentRepository.save(c);

        ObjectNode payload = notifications.payload();
        payload.put("actorUsername", user.getUsername());
        if (user.getDisplayName() != null) payload.put("actorDisplayName", user.getDisplayName());
        payload.put("pollId", poll.getId().toString());
        payload.put("pollTitle", poll.getTitle());
        payload.put("commentId", c.getId().toString());
        notifications.notify(poll.getAuthor().getId(), NotificationType.POLL_COMMENTED, payload, userId);

        return CommentView.from(c);
    }

    @Transactional
    public void delete(UUID commentId, UUID requesterId) {
        Comment c = commentRepository.findById(commentId)
                .orElseThrow(() -> ApiException.notFound("COMMENT_NOT_FOUND", "Comment not found"));
        if (!c.getUser().getId().equals(requesterId)) {
            throw ApiException.forbidden("NOT_COMMENT_OWNER", "Only the author can delete this comment");
        }
        commentRepository.delete(c);
    }

    @Transactional(readOnly = true)
    public CursorPage<CommentView> list(UUID pollId, String cursorParam, int limit) {
        Cursor cursor = Cursor.decode(cursorParam);
        Slice<Comment> slice = commentRepository.findByPollWithCursor(
                pollId, cursor.time(), cursor.id(), PageRequest.of(0, Math.min(Math.max(limit, 1), 100)));

        List<CommentView> items = slice.getContent().stream().map(CommentView::from).toList();
        String next = null;
        if (slice.hasNext() && !slice.getContent().isEmpty()) {
            Comment last = slice.getContent().get(slice.getContent().size() - 1);
            next = new Cursor(last.getCreatedAt(), last.getId()).encode();
        }
        return CursorPage.of(items, next);
    }
}
