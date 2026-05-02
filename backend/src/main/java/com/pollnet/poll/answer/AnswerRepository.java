package com.pollnet.poll.answer;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
public interface AnswerRepository extends JpaRepository<Answer, UUID> {

    List<Answer> findByQuestionId(UUID questionId);

    List<Answer> findByQuestionIdIn(Collection<UUID> questionIds);

    List<Answer> findByUserIdAndQuestionIdIn(UUID userId, Collection<UUID> questionIds);

    boolean existsByQuestionIdAndUserId(UUID questionId, UUID userId);

    long countByQuestionId(UUID questionId);

    /** True if the user has at least one answer for any question in the given poll. */
    boolean existsByQuestionPollIdAndUserId(UUID pollId, UUID userId);
}
