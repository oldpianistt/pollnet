package com.pollnet.notification;

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
public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    @Query("""
           SELECT n FROM Notification n
           WHERE n.user.id = :userId
             AND (n.createdAt < :cursorTime
                  OR (n.createdAt = :cursorTime AND n.id < :cursorId))
           ORDER BY n.createdAt DESC, n.id DESC
           """)
    Slice<Notification> findForUser(@Param("userId") UUID userId,
                                    @Param("cursorTime") OffsetDateTime cursorTime,
                                    @Param("cursorId") UUID cursorId,
                                    Pageable pageable);

    long countByUserIdAndReadAtIsNull(UUID userId);

    @Modifying
    @Query("UPDATE Notification n SET n.readAt = CURRENT_TIMESTAMP WHERE n.user.id = :userId AND n.readAt IS NULL")
    int markAllRead(@Param("userId") UUID userId);
}
