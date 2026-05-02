package com.pollnet;

import com.pollnet.invitation.Invitation;
import com.pollnet.invitation.InvitationRepository;
import com.pollnet.user.User;
import com.pollnet.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;

/**
 * Helper for tests: create a real User (with encoded password) and an open
 * invitation pointing at them, so tests can drive the public register endpoint.
 */
@Component
@RequiredArgsConstructor
public class TestSeed {

    private final UserRepository userRepository;
    private final InvitationRepository invitationRepository;
    private final PasswordEncoder passwordEncoder;

    public User createUser(String username, String email, String rawPassword) {
        User u = User.builder()
                .username(username)
                .email(email)
                .passwordHash(passwordEncoder.encode(rawPassword))
                .inviteQuota(10)
                .inviteQuotaResetAt(OffsetDateTime.now())
                .build();
        return userRepository.save(u);
    }

    public Invitation issueInvite(User inviter, String token) {
        Invitation inv = Invitation.builder()
                .token(token)
                .inviter(inviter)
                .expiresAt(OffsetDateTime.now().plusDays(7))
                .build();
        return invitationRepository.save(inv);
    }
}
