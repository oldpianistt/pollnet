package com.pollnet.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    Optional<User> findByUsernameOrEmail(String username, String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    @Modifying
    @Query("UPDATE User u SET u.inviteQuota = :quota, u.inviteQuotaResetAt = CURRENT_TIMESTAMP")
    int resetAllInviteQuotas(int quota);

    @Query("""
           SELECT u FROM User u
           WHERE LOWER(u.username) LIKE LOWER(CONCAT('%', :q, '%'))
              OR (u.displayName IS NOT NULL AND LOWER(u.displayName) LIKE LOWER(CONCAT('%', :q, '%')))
           ORDER BY u.username ASC
           """)
    java.util.List<User> searchByText(String q, org.springframework.data.domain.Pageable pageable);
}
