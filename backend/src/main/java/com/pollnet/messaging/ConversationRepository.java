package com.pollnet.messaging;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, UUID> {

    @Query("""
           SELECT c FROM Conversation c
           WHERE (c.userA.id = :a AND c.userB.id = :b)
              OR (c.userA.id = :b AND c.userB.id = :a)
           """)
    Optional<Conversation> findByPair(@Param("a") UUID a, @Param("b") UUID b);

    @Query("""
           SELECT c FROM Conversation c
                JOIN FETCH c.userA
                JOIN FETCH c.userB
           WHERE c.userA.id = :userId OR c.userB.id = :userId
           ORDER BY c.lastMessageAt DESC
           """)
    List<Conversation> findAllForUser(@Param("userId") UUID userId);
}
