package com.pollnet.invitation;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InvitationRepository extends JpaRepository<Invitation, UUID> {

    Optional<Invitation> findByToken(String token);

    List<Invitation> findByInviterIdAndUsedByIsNullOrderByCreatedAtDesc(UUID inviterId);

    long countByInviterIdAndUsedByIsNotNull(UUID inviterId);
}
