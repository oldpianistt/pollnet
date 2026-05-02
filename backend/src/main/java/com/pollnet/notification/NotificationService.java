package com.pollnet.notification;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.pollnet.common.error.ApiException;
import com.pollnet.common.pagination.Cursor;
import com.pollnet.common.pagination.CursorPage;
import com.pollnet.notification.dto.NotificationView;
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
public class NotificationService {

    private final NotificationRepository repo;
    private final UserRepository userRepository;
    private final ObjectMapper mapper;

    /** Don't notify yourself (e.g. commenting on your own poll). */
    @Transactional
    public void notify(UUID recipientId, NotificationType type, ObjectNode payload, UUID actorId) {
        if (recipientId.equals(actorId)) return;
        User recipient = userRepository.findById(recipientId).orElse(null);
        if (recipient == null) return;
        Notification n = Notification.builder()
                .user(recipient)
                .type(type)
                .payload(payload)
                .build();
        repo.save(n);
    }

    public ObjectNode payload() {
        return mapper.createObjectNode();
    }

    @Transactional(readOnly = true)
    public CursorPage<NotificationView> listForUser(UUID userId, String cursorParam, int limit) {
        Cursor cursor = Cursor.decode(cursorParam);
        Slice<Notification> slice = repo.findForUser(userId, cursor.time(), cursor.id(),
                PageRequest.of(0, Math.min(Math.max(limit, 1), 50)));
        List<NotificationView> items = slice.getContent().stream().map(NotificationView::from).toList();
        String next = null;
        if (slice.hasNext() && !items.isEmpty()) {
            Notification last = slice.getContent().get(slice.getContent().size() - 1);
            next = new Cursor(last.getCreatedAt(), last.getId()).encode();
        }
        return CursorPage.of(items, next);
    }

    @Transactional(readOnly = true)
    public long unreadCount(UUID userId) {
        return repo.countByUserIdAndReadAtIsNull(userId);
    }

    @Transactional
    public void markRead(UUID notificationId, UUID userId) {
        Notification n = repo.findById(notificationId)
                .orElseThrow(() -> ApiException.notFound("NOTIFICATION_NOT_FOUND", "Bildirim yok"));
        if (!n.getUser().getId().equals(userId)) {
            throw ApiException.forbidden("NOT_NOTIFICATION_OWNER", "Sadece kendi bildirimini okudu yapabilirsin");
        }
        if (n.getReadAt() == null) {
            n.setReadAt(java.time.OffsetDateTime.now());
        }
    }

    @Transactional
    public int markAllRead(UUID userId) {
        return repo.markAllRead(userId);
    }
}
