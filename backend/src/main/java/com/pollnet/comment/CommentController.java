package com.pollnet.comment;

import com.pollnet.auth.CurrentUser;
import com.pollnet.comment.dto.CommentCreateRequest;
import com.pollnet.comment.dto.CommentView;
import com.pollnet.common.pagination.CursorPage;
import com.pollnet.ratelimit.RateLimiter;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.Duration;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;
    private final RateLimiter rateLimiter;

    @GetMapping("/api/polls/{pollId}/comments")
    public CursorPage<CommentView> list(@PathVariable UUID pollId,
                                        @RequestParam(required = false) String cursor,
                                        @RequestParam(defaultValue = "20") int limit) {
        return commentService.list(pollId, cursor, limit);
    }

    @PostMapping("/api/polls/{pollId}/comments")
    public ResponseEntity<CommentView> create(@PathVariable UUID pollId,
                                              @Valid @RequestBody CommentCreateRequest req) {
        UUID userId = CurrentUser.requiredId();
        rateLimiter.check("comment", userId.toString(), 20, Duration.ofMinutes(1));
        CommentView created = commentService.create(pollId, userId, req);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @DeleteMapping("/api/comments/{commentId}")
    public ResponseEntity<Void> delete(@PathVariable UUID commentId) {
        commentService.delete(commentId, CurrentUser.requiredId());
        return ResponseEntity.noContent().build();
    }
}
