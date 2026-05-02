package com.pollnet.notification;

import com.pollnet.auth.CurrentUser;
import com.pollnet.common.pagination.CursorPage;
import com.pollnet.notification.dto.NotificationView;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService service;

    @GetMapping
    public CursorPage<NotificationView> list(@RequestParam(required = false) String cursor,
                                             @RequestParam(defaultValue = "20") int limit) {
        return service.listForUser(CurrentUser.requiredId(), cursor, limit);
    }

    @GetMapping("/unread-count")
    public Map<String, Long> unreadCount() {
        return Map.of("count", service.unreadCount(CurrentUser.requiredId()));
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<Void> markRead(@PathVariable UUID id) {
        service.markRead(id, CurrentUser.requiredId());
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/read-all")
    public Map<String, Integer> markAllRead() {
        int updated = service.markAllRead(CurrentUser.requiredId());
        return Map.of("updated", updated);
    }
}
