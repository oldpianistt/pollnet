package com.pollnet.messaging;

import com.pollnet.auth.CurrentUser;
import com.pollnet.common.error.ApiException;
import com.pollnet.common.pagination.CursorPage;
import com.pollnet.media.MediaProperties;
import com.pollnet.media.MediaStorage;
import com.pollnet.messaging.dto.ConversationView;
import com.pollnet.messaging.dto.MessageView;
import com.pollnet.messaging.dto.SendMessageRequest;
import com.pollnet.ratelimit.RateLimiter;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/messages")
@RequiredArgsConstructor
public class MessagingController {

    private final MessagingService service;
    private final RateLimiter rateLimiter;
    private final MediaStorage storage;
    private final MediaProperties mediaProps;

    /* ───────────── Conversations ───────────── */

    @GetMapping("/conversations")
    public List<ConversationView> listConversations() {
        return service.listForUser(CurrentUser.requiredId());
    }

    @PostMapping("/conversations")
    public ResponseEntity<ConversationView> openConversation(@RequestBody Map<String, String> body) {
        String peer = body.get("peerUsername");
        if (peer == null) throw ApiException.badRequest("MISSING_PEER", "peerUsername gerekli");
        ConversationView view = service.openWith(CurrentUser.requiredId(), peer);
        return ResponseEntity.status(HttpStatus.OK).body(view);
    }

    @GetMapping("/unread-count")
    public Map<String, Long> totalUnread() {
        return Map.of("count", service.totalUnread(CurrentUser.requiredId()));
    }

    /* ───────────── Messages ───────────── */

    @GetMapping("/conversations/{id}/messages")
    public CursorPage<MessageView> listMessages(@PathVariable UUID id,
                                                @RequestParam(required = false) String cursor,
                                                @RequestParam(defaultValue = "30") int limit) {
        return service.listMessages(id, CurrentUser.requiredId(), cursor, limit);
    }

    @PostMapping("/conversations/{id}/messages")
    public ResponseEntity<MessageView> send(@PathVariable UUID id,
                                            @Valid @RequestBody SendMessageRequest req) {
        UUID me = CurrentUser.requiredId();
        rateLimiter.check("dm-send", me.toString(), 60, Duration.ofMinutes(1));
        MessageView m = service.send(id, me, req);
        return ResponseEntity.status(HttpStatus.CREATED).body(m);
    }

    @PostMapping("/conversations/{id}/read")
    public Map<String, Integer> markRead(@PathVariable UUID id) {
        int updated = service.markRead(id, CurrentUser.requiredId());
        return Map.of("updated", updated);
    }

    /* ───────────── Attachments ───────────── */

    @PostMapping("/attachments")
    public Map<String, String> uploadAttachment(@RequestParam("file") MultipartFile file) {
        UUID me = CurrentUser.requiredId();
        rateLimiter.check("dm-upload", me.toString(), 30, Duration.ofMinutes(1));
        var stored = storage.storeImage(file, "messages", mediaProps.maxAttachmentBytesOrDefault());
        return Map.of("url", stored.publicUrl());
    }
}
