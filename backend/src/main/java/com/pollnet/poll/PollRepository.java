package com.pollnet.poll;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.UUID;

@Repository
public interface PollRepository extends JpaRepository<Poll, UUID> {

    Slice<Poll> findByAuthorIdOrderByCreatedAtDesc(UUID authorId, Pageable pageable);

    /**
     * Cursor-paginated discover feed (newest first). Pass {@code Long.MAX_VALUE} epoch
     * for the first page. Tie-breaker on id keeps order stable when timestamps collide.
     */
    @Query("""
           SELECT p FROM Poll p
           WHERE p.createdAt < :cursorTime
              OR (p.createdAt = :cursorTime AND p.id < :cursorId)
           ORDER BY p.createdAt DESC, p.id DESC
           """)
    Slice<Poll> findDiscoverFeed(@Param("cursorTime") OffsetDateTime cursorTime,
                                 @Param("cursorId") UUID cursorId,
                                 Pageable pageable);

    @Query("""
           SELECT p FROM Poll p
           WHERE p.author.id IN :authorIds
             AND (p.createdAt < :cursorTime
                  OR (p.createdAt = :cursorTime AND p.id < :cursorId))
           ORDER BY p.createdAt DESC, p.id DESC
           """)
    Slice<Poll> findFollowingFeed(@Param("authorIds") Collection<UUID> authorIds,
                                  @Param("cursorTime") OffsetDateTime cursorTime,
                                  @Param("cursorId") UUID cursorId,
                                  Pageable pageable);

    /**
     * Case-insensitive substring on title/description, newest first.
     * JOIN FETCH author so {@link com.pollnet.user.dto.UserSummary} can be built
     * outside the persistence context (the search controller is not @Transactional).
     */
    @Query("""
           SELECT p FROM Poll p JOIN FETCH p.author
           WHERE LOWER(p.title) LIKE LOWER(CONCAT('%', :q, '%'))
              OR (p.description IS NOT NULL AND LOWER(p.description) LIKE LOWER(CONCAT('%', :q, '%')))
           ORDER BY p.createdAt DESC, p.id DESC
           """)
    Slice<Poll> searchByText(@Param("q") String q, Pageable pageable);
}
