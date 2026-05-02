package com.pollnet.follow;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface FollowRepository extends JpaRepository<Follow, FollowId> {

    boolean existsByIdFollowerIdAndIdFolloweeId(UUID followerId, UUID followeeId);

    long countByIdFollowerId(UUID followerId);   // following count

    long countByIdFolloweeId(UUID followeeId);   // followers count

    @Query("SELECT f.id.followeeId FROM Follow f WHERE f.id.followerId = :followerId")
    List<UUID> findFolloweeIds(@Param("followerId") UUID followerId);

    Slice<Follow> findByIdFollowerIdOrderByCreatedAtDesc(UUID followerId, Pageable pageable);

    Slice<Follow> findByIdFolloweeIdOrderByCreatedAtDesc(UUID followeeId, Pageable pageable);
}
