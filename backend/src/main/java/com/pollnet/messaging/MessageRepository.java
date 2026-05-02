package com.pollnet.messaging;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.UUID;

@Repository
public interface MessageRepository extends JpaRepository<Message, UUID> {

    @Query("""
           SELECT m FROM Message m
                JOIN FETCH m.sender
           WHERE m.conversation.id = :conversationId
             AND (m.createdAt < :cursorTime
                  OR (m.createdAt = :cursorTime AND m.id < :cursorId))
           ORDER BY m.createdAt DESC, m.id DESC
           """)
    Slice<Message> findForConversation(@Param("conversationId") UUID conversationId,
                                       @Param("cursorTime") OffsetDateTime cursorTime,
                                       @Param("cursorId") UUID cursorId,
                                       Pageable pageable);

    /** Mark every unread message NOT sent by the reader as read. */
    @Modifying
    @Query("""
           UPDATE Message m SET m.readAt = CURRENT_TIMESTAMP
           WHERE m.conversation.id = :conversationId
             AND m.sender.id <> :readerId
             AND m.readAt IS NULL
           """)
    int markRead(@Param("conversationId") UUID conversationId, @Param("readerId") UUID readerId);

    /** Total unread across all conversations for a user — for the navbar badge. */
    @Query("""
           SELECT COUNT(m) FROM Message m
           WHERE m.readAt IS NULL
             AND m.sender.id <> :userId
             AND (m.conversation.userA.id = :userId OR m.conversation.userB.id = :userId)
           """)
    long countTotalUnreadFor(@Param("userId") UUID userId);

    /** Per-conversation unread count, for list view. */
    long countByConversationIdAndSenderIdNotAndReadAtIsNull(UUID conversationId, UUID readerId);
}
