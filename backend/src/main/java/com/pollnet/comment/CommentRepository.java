package com.pollnet.comment;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.UUID;

@Repository
public interface CommentRepository extends JpaRepository<Comment, UUID> {

    @Query("""
           SELECT c FROM Comment c
           WHERE c.poll.id = :pollId
             AND (c.createdAt < :cursorTime
                  OR (c.createdAt = :cursorTime AND c.id < :cursorId))
           ORDER BY c.createdAt DESC, c.id DESC
           """)
    Slice<Comment> findByPollWithCursor(@Param("pollId") UUID pollId,
                                        @Param("cursorTime") OffsetDateTime cursorTime,
                                        @Param("cursorId") UUID cursorId,
                                        Pageable pageable);

    long countByPollId(UUID pollId);
}
