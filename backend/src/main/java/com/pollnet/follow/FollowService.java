package com.pollnet.follow;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.pollnet.common.error.ApiException;
import com.pollnet.notification.NotificationService;
import com.pollnet.notification.NotificationType;
import com.pollnet.user.User;
import com.pollnet.user.UserRepository;
import com.pollnet.user.dto.UserSummary;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FollowService {

    private final FollowRepository followRepository;
    private final UserRepository userRepository;
    private final NotificationService notifications;

    @Transactional
    public void follow(UUID followerId, String followeeUsername) {
        User followee = userRepository.findByUsername(followeeUsername)
                .orElseThrow(() -> ApiException.notFound("USER_NOT_FOUND", "User not found"));
        if (followee.getId().equals(followerId)) {
            throw ApiException.badRequest("SELF_FOLLOW", "Cannot follow yourself");
        }
        if (followRepository.existsByIdFollowerIdAndIdFolloweeId(followerId, followee.getId())) {
            return; // idempotent
        }
        User follower = userRepository.findById(followerId)
                .orElseThrow(() -> ApiException.unauthorized("UNAUTHENTICATED", "User not found"));
        Follow f = Follow.builder()
                .id(new FollowId(followerId, followee.getId()))
                .follower(follower)
                .followee(followee)
                .build();
        followRepository.save(f);

        ObjectNode payload = notifications.payload();
        payload.put("actorUsername", follower.getUsername());
        if (follower.getDisplayName() != null) payload.put("actorDisplayName", follower.getDisplayName());
        notifications.notify(followee.getId(), NotificationType.NEW_FOLLOWER, payload, followerId);
    }

    @Transactional
    public void unfollow(UUID followerId, String followeeUsername) {
        User followee = userRepository.findByUsername(followeeUsername)
                .orElseThrow(() -> ApiException.notFound("USER_NOT_FOUND", "User not found"));
        followRepository.deleteById(new FollowId(followerId, followee.getId()));
    }

    @Transactional(readOnly = true)
    public List<UserSummary> followers(String username) {
        User u = userRepository.findByUsername(username)
                .orElseThrow(() -> ApiException.notFound("USER_NOT_FOUND", "User not found"));
        return followRepository
                .findByIdFolloweeIdOrderByCreatedAtDesc(u.getId(), PageRequest.of(0, 100))
                .stream()
                .map(f -> UserSummary.from(f.getFollower()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<UserSummary> following(String username) {
        User u = userRepository.findByUsername(username)
                .orElseThrow(() -> ApiException.notFound("USER_NOT_FOUND", "User not found"));
        return followRepository
                .findByIdFollowerIdOrderByCreatedAtDesc(u.getId(), PageRequest.of(0, 100))
                .stream()
                .map(f -> UserSummary.from(f.getFollowee()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<UUID> followeeIdsOf(UUID userId) {
        return followRepository.findFolloweeIds(userId);
    }
}
