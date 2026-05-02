package com.pollnet.invitation;

import com.pollnet.common.error.ApiException;
import com.pollnet.config.InviteProperties;
import com.pollnet.invitation.dto.InvitationView;
import com.pollnet.invitation.dto.QuotaView;
import com.pollnet.user.User;
import com.pollnet.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class InvitationService {

    private static final String REDIS_KEY_PREFIX = "invite:";
    private static final SecureRandom RNG = new SecureRandom();
    private static final Base64.Encoder TOKEN_ENCODER = Base64.getUrlEncoder().withoutPadding();

    private final InvitationRepository invitationRepository;
    private final UserRepository userRepository;
    private final StringRedisTemplate redis;
    private final InviteProperties props;

    @Transactional
    public InvitationView createForUser(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> ApiException.unauthorized("UNAUTHENTICATED", "User not found"));

        if (user.getInviteQuota() <= 0) {
            throw ApiException.forbidden("QUOTA_EXHAUSTED", "Monthly invite quota exhausted");
        }

        String token = generateToken();
        OffsetDateTime expiresAt = OffsetDateTime.now().plusDays(props.tokenTtlDays());

        Invitation inv = Invitation.builder()
                .token(token)
                .inviter(user)
                .expiresAt(expiresAt)
                .build();
        invitationRepository.save(inv);

        user.setInviteQuota(user.getInviteQuota() - 1);

        redis.opsForValue().set(REDIS_KEY_PREFIX + token, user.getId().toString(),
                Duration.ofDays(props.tokenTtlDays()));

        return InvitationView.from(inv);
    }

    @Transactional(readOnly = true)
    public List<InvitationView> listMyUnused(UUID userId) {
        return invitationRepository.findByInviterIdAndUsedByIsNullOrderByCreatedAtDesc(userId).stream()
                .map(InvitationView::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public QuotaView quotaFor(UUID userId) {
        User u = userRepository.findById(userId)
                .orElseThrow(() -> ApiException.unauthorized("UNAUTHENTICATED", "User not found"));
        return new QuotaView(u.getInviteQuota(), props.quotaMonthly(), u.getInviteQuotaResetAt());
    }

    /**
     * Validates the token and marks the invitation as used by {@code newUser}.
     * Caller is responsible for being inside a transaction (the register flow).
     * Returns the inviter so the caller can wire {@code invited_by}.
     */
    public User consumeForRegistration(String token, User newUser) {
        Invitation inv = invitationRepository.findByToken(token)
                .orElseThrow(() -> ApiException.badRequest("INVALID_INVITE", "Invite token is invalid"));

        if (inv.isUsed())    throw ApiException.badRequest("INVITE_ALREADY_USED", "Invite has already been used");
        if (inv.isExpired()) throw ApiException.badRequest("INVITE_EXPIRED",      "Invite has expired");

        inv.setUsedBy(newUser);
        inv.setUsedAt(OffsetDateTime.now());

        redis.delete(REDIS_KEY_PREFIX + token);
        return inv.getInviter();
    }

    private static String generateToken() {
        byte[] bytes = new byte[32];
        RNG.nextBytes(bytes);
        return TOKEN_ENCODER.encodeToString(bytes);
    }
}
