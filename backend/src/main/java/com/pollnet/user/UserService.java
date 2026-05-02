package com.pollnet.user;

import com.pollnet.common.error.ApiException;
import com.pollnet.follow.FollowRepository;
import com.pollnet.user.dto.MeView;
import com.pollnet.user.dto.PublicProfileView;
import com.pollnet.user.dto.UpdateMeRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final FollowRepository followRepository;

    @Transactional(readOnly = true)
    public MeView me(UUID userId) {
        return MeView.from(loadOrThrow(userId));
    }

    @Transactional
    public MeView updateMe(UUID userId, UpdateMeRequest req) {
        User u = loadOrThrow(userId);
        if (req.displayName() != null) u.setDisplayName(req.displayName());
        if (req.bio() != null)         u.setBio(req.bio());
        return MeView.from(u);
    }

    @Transactional(readOnly = true)
    public PublicProfileView publicProfile(String username, UUID viewerId) {
        User u = userRepository.findByUsername(username)
                .orElseThrow(() -> ApiException.notFound("USER_NOT_FOUND", "User not found"));
        long followers = followRepository.countByIdFolloweeId(u.getId());
        long following = followRepository.countByIdFollowerId(u.getId());
        boolean viewerFollows = viewerId != null
                && !viewerId.equals(u.getId())
                && followRepository.existsByIdFollowerIdAndIdFolloweeId(viewerId, u.getId());
        return PublicProfileView.from(u, followers, following, viewerFollows);
    }

    /** Heartbeat — refreshes last_seen_at so the UI can render "online ~now". */
    @Transactional
    public void touchPresence(UUID userId) {
        User u = userRepository.findById(userId).orElse(null);
        if (u != null) u.setLastSeenAt(OffsetDateTime.now());
    }

    private User loadOrThrow(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> ApiException.unauthorized("UNAUTHENTICATED", "User not found"));
    }
}
