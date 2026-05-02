package com.pollnet.auth;

import com.pollnet.auth.dto.AuthResponse;
import com.pollnet.auth.dto.LoginRequest;
import com.pollnet.auth.dto.RefreshRequest;
import com.pollnet.auth.dto.RegisterRequest;
import com.pollnet.auth.jwt.JwtService;
import com.pollnet.auth.verification.EmailVerificationService;
import com.pollnet.common.error.ApiException;
import com.pollnet.config.InviteProperties;
import com.pollnet.invitation.InvitationService;
import com.pollnet.user.User;
import com.pollnet.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final String REFRESH_KEY_PREFIX = "refresh:";
    private static final SecureRandom RNG = new SecureRandom();
    private static final Base64.Encoder TOKEN_ENCODER = Base64.getUrlEncoder().withoutPadding();

    private final UserRepository userRepository;
    private final InvitationService invitationService;
    private final EmailVerificationService emailVerificationService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final StringRedisTemplate redis;
    private final InviteProperties inviteProps;

    @Transactional
    public AuthResponse register(RegisterRequest req) {
        if (userRepository.existsByUsername(req.username())) {
            throw ApiException.conflict("USERNAME_TAKEN", "Username already taken");
        }
        if (userRepository.existsByEmail(req.email())) {
            throw ApiException.conflict("EMAIL_TAKEN", "Email already registered");
        }

        // Build user without flushing first; we need it persisted before linking the invite
        // (used_by FK requires the new row to exist).
        User newUser = User.builder()
                .username(req.username())
                .email(req.email())
                .passwordHash(passwordEncoder.encode(req.password()))
                .displayName(req.displayName())
                .inviteQuota(inviteProps.quotaMonthly())
                .inviteQuotaResetAt(OffsetDateTime.now())
                .build();
        newUser = userRepository.save(newUser);

        User inviter = invitationService.consumeForRegistration(req.inviteToken(), newUser);
        newUser.setInvitedBy(inviter);

        emailVerificationService.issueFor(newUser);

        return issueTokens(newUser);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest req) {
        User user = userRepository
                .findByUsernameOrEmail(req.usernameOrEmail(), req.usernameOrEmail())
                .orElseThrow(() -> ApiException.unauthorized("BAD_CREDENTIALS", "Invalid credentials"));

        if (!passwordEncoder.matches(req.password(), user.getPasswordHash())) {
            throw ApiException.unauthorized("BAD_CREDENTIALS", "Invalid credentials");
        }
        return issueTokens(user);
    }

    /**
     * Refresh-token rotation: the presented token is consumed unconditionally, even if
     * the lookup fails — that way a leaked refresh token cannot be replayed.
     */
    @Transactional(readOnly = true)
    public AuthResponse refresh(RefreshRequest req) {
        String key = REFRESH_KEY_PREFIX + req.refreshToken();
        String userIdStr = redis.opsForValue().getAndDelete(key);
        if (userIdStr == null) {
            throw ApiException.unauthorized("INVALID_REFRESH", "Refresh token invalid or expired");
        }
        UUID userId;
        try {
            userId = UUID.fromString(userIdStr);
        } catch (IllegalArgumentException ex) {
            throw ApiException.unauthorized("INVALID_REFRESH", "Refresh token invalid or expired");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> ApiException.unauthorized("INVALID_REFRESH", "Refresh token invalid or expired"));
        return issueTokens(user);
    }

    public void logout(String refreshToken) {
        if (refreshToken != null && !refreshToken.isBlank()) {
            redis.delete(REFRESH_KEY_PREFIX + refreshToken);
        }
    }

    private AuthResponse issueTokens(User user) {
        String access = jwtService.issueAccessToken(user.getId(), user.getUsername());
        String refresh = generateRefreshToken();
        redis.opsForValue().set(REFRESH_KEY_PREFIX + refresh, user.getId().toString(),
                Duration.ofSeconds(jwtService.refreshTtlSeconds()));
        return new AuthResponse(
                access,
                refresh,
                jwtService.accessTtlSeconds(),
                new AuthResponse.UserSummary(user.getId(), user.getUsername(), user.getEmail(), user.getDisplayName(), user.getAvatarUrl())
        );
    }

    private static String generateRefreshToken() {
        byte[] bytes = new byte[48];
        RNG.nextBytes(bytes);
        return TOKEN_ENCODER.encodeToString(bytes);
    }
}
