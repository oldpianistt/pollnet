package com.pollnet.comment.dto;

import com.pollnet.comment.Comment;
import com.pollnet.user.dto.UserSummary;

import java.time.OffsetDateTime;
import java.util.UUID;

public record CommentView(
        UUID id,
        UserSummary author,
        String body,
        OffsetDateTime createdAt
) {
    public static CommentView from(Comment c) {
        return new CommentView(c.getId(), UserSummary.from(c.getUser()), c.getBody(), c.getCreatedAt());
    }
}
