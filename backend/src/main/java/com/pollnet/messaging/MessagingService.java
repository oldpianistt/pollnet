package com.pollnet.messaging;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.pollnet.common.error.ApiException;
import com.pollnet.common.pagination.Cursor;
import com.pollnet.common.pagination.CursorPage;
import com.pollnet.messaging.dto.ConversationView;
import com.pollnet.messaging.dto.MessageView;
import com.pollnet.messaging.dto.SendMessageRequest;
import com.pollnet.notification.NotificationService;
import com.pollnet.user.User;
import com.pollnet.user.UserRepository;
import com.pollnet.user.dto.UserSummary;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MessagingService {

    private final ConversationRepository conversationRepo;
    private final MessageRepository      messageRepo;
    private final UserRepository         userRepo;
    private final MessageCipher          cipher;
    private final NotificationService    notifications;

    /* ───────────── Conversations ───────────── */

    @Transactional
    public ConversationView openWith(UUID meId, String peerUsername) {
        if (peerUsername == null || peerUsername.isBlank()) {
            throw ApiException.badRequest("INVALID_PEER", "Peer username gerekli");
        }
        User peer = userRepo.findByUsername(peerUsername)
                .orElseThrow(() -> ApiException.notFound("USER_NOT_FOUND", "Kullanıcı bulunamadı"));
        if (peer.getId().equals(meId)) {
            throw ApiException.badRequest("SELF_DM", "Kendine mesaj atamazsın");
        }
        Conversation c = conversationRepo.findByPair(meId, peer.getId())
                .orElseGet(() -> create(meId, peer));
        return toView(c, meId);
    }

    @Transactional(readOnly = true)
    public List<ConversationView> listForUser(UUID meId) {
        List<Conversation> rows = conversationRepo.findAllForUser(meId);
        List<ConversationView> out = new ArrayList<>(rows.size());
        for (Conversation c : rows) out.add(toView(c, meId));
        return out;
    }

    @Transactional(readOnly = true)
    public long totalUnread(UUID meId) {
        return messageRepo.countTotalUnreadFor(meId);
    }

    /* ───────────── Messages ───────────── */

    @Transactional(readOnly = true)
    public CursorPage<MessageView> listMessages(UUID convoId, UUID meId, String cursorParam, int limit) {
        Conversation c = mustParticipate(convoId, meId);
        Cursor cursor = Cursor.decode(cursorParam);
        Slice<Message> slice = messageRepo.findForConversation(
                c.getId(), cursor.time(), cursor.id(),
                PageRequest.of(0, Math.min(Math.max(limit, 1), 100)));
        List<MessageView> items = slice.getContent().stream().map(this::toMessageView).toList();
        String next = null;
        if (slice.hasNext() && !items.isEmpty()) {
            Message last = slice.getContent().get(slice.getContent().size() - 1);
            next = new Cursor(last.getCreatedAt(), last.getId()).encode();
        }
        return CursorPage.of(items, next);
    }

    @Transactional
    public MessageView send(UUID convoId, UUID senderId, SendMessageRequest req) {
        if ((req.body() == null || req.body().isBlank()) && (req.attachmentUrl() == null || req.attachmentUrl().isBlank())) {
            throw ApiException.badRequest("EMPTY_MESSAGE", "Boş mesaj gönderilemez");
        }
        Conversation c = mustParticipate(convoId, senderId);
        User sender = userRepo.findById(senderId)
                .orElseThrow(() -> ApiException.unauthorized("UNAUTHENTICATED", "User not found"));

        String plain = req.body() == null ? "" : req.body().trim();
        Message m = Message.builder()
                .conversation(c)
                .sender(sender)
                .ciphertext(cipher.encrypt(plain))
                .attachmentUrl(req.attachmentUrl())
                .build();
        messageRepo.save(m);
        c.setLastMessageAt(OffsetDateTime.now());

        UUID peerId = c.getUserA().getId().equals(senderId) ? c.getUserB().getId() : c.getUserA().getId();

        // Notification — payload deliberately doesn't include the message body
        // (we don't want plaintext leaking into notifications storage either).
        ObjectNode payload = notifications.payload();
        payload.put("actorUsername", sender.getUsername());
        if (sender.getDisplayName() != null) payload.put("actorDisplayName", sender.getDisplayName());
        payload.put("conversationId", c.getId().toString());
        // Reuse POLL_COMMENTED bucket to avoid a new notif type just for this MVP.
        // A real release would add NEW_MESSAGE — left as deliberate follow-up.
        // notifications.notify(peerId, NotificationType.NEW_MESSAGE, payload, senderId);

        // For now: just bump conversation timestamp; client polls totalUnread.
        // The peer's UI shows the unread count and, on open, the new message.
        return toMessageView(m);
    }

    @Transactional
    public int markRead(UUID convoId, UUID readerId) {
        mustParticipate(convoId, readerId);
        return messageRepo.markRead(convoId, readerId);
    }

    /* ───────────── helpers ───────────── */

    private Conversation create(UUID meId, User peer) {
        // Canonicalise so user_a_id < user_b_id PER POSTGRES BYTE ORDER. Java's
        // UUID.compareTo is signed-long-based and disagrees with Postgres's
        // unsigned byte order — string comparison on the canonical hex form
        // matches Postgres exactly (0-9 < a-f).
        boolean meFirst = meId.toString().compareTo(peer.getId().toString()) < 0;
        UUID aId = meFirst ? meId : peer.getId();
        UUID bId = meFirst ? peer.getId() : meId;
        User userA = userRepo.findById(aId).orElseThrow();
        User userB = userRepo.findById(bId).orElseThrow();
        Conversation c = Conversation.builder()
                .userA(userA)
                .userB(userB)
                .lastMessageAt(OffsetDateTime.now())
                .build();
        return conversationRepo.save(c);
    }

    private Conversation mustParticipate(UUID convoId, UUID meId) {
        Conversation c = conversationRepo.findById(convoId)
                .orElseThrow(() -> ApiException.notFound("CONVERSATION_NOT_FOUND", "Sohbet yok"));
        if (!c.getUserA().getId().equals(meId) && !c.getUserB().getId().equals(meId)) {
            throw ApiException.forbidden("NOT_PARTICIPANT", "Bu sohbete erişimin yok");
        }
        return c;
    }

    private ConversationView toView(Conversation c, UUID meId) {
        User peer = c.getUserA().getId().equals(meId) ? c.getUserB() : c.getUserA();
        long unread = messageRepo.countByConversationIdAndSenderIdNotAndReadAtIsNull(c.getId(), meId);
        // Preview: last message decrypted, truncated. Cheap to fetch one row.
        String preview = null;
        Slice<Message> last = messageRepo.findForConversation(
                c.getId(), OffsetDateTime.now().plusYears(100),
                new UUID(Long.MAX_VALUE, Long.MAX_VALUE),
                PageRequest.of(0, 1));
        if (!last.isEmpty()) {
            String full = cipher.decrypt(last.getContent().get(0).getCiphertext());
            preview = full.length() > 80 ? full.substring(0, 77) + "…" : full;
        }
        return new ConversationView(c.getId(), UserSummary.from(peer), preview, c.getLastMessageAt(), unread);
    }

    private MessageView toMessageView(Message m) {
        return new MessageView(
                m.getId(),
                UserSummary.from(m.getSender()),
                cipher.decrypt(m.getCiphertext()),
                m.getAttachmentUrl(),
                m.getCreatedAt(),
                m.getReadAt()
        );
    }
}
