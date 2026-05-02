package com.pollnet.auth.passwordreset;

import com.pollnet.common.error.ApiException;
import com.pollnet.config.MailProperties;
import com.pollnet.config.PasswordResetProperties;
import com.pollnet.mail.EmailService;
import com.pollnet.user.User;
import com.pollnet.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private static final SecureRandom RNG = new SecureRandom();
    private static final Base64.Encoder TOKEN = Base64.getUrlEncoder().withoutPadding();

    private final PasswordResetRepository repo;
    private final UserRepository userRepository;
    private final PasswordEncoder encoder;
    private final EmailService emailService;
    private final MailProperties mailProps;
    private final PasswordResetProperties props;

    /**
     * Always returns silently — we do not leak whether the email exists.
     * Caller-facing endpoint always responds 204.
     */
    @Transactional
    public void requestReset(String email) {
        Optional<User> maybe = userRepository.findByEmail(email);
        if (maybe.isEmpty()) return;
        User user = maybe.get();

        String token = randomToken();
        OffsetDateTime expiresAt = OffsetDateTime.now().plusHours(props.tokenTtlHours());
        PasswordReset pr = PasswordReset.builder()
                .user(user)
                .token(token)
                .expiresAt(expiresAt)
                .build();
        repo.save(pr);

        String link = mailProps.publicBaseUrl() + "/reset-password?token=" + token;
        String body = """
                Şifre sıfırlama isteğin alındı. %d saat içinde aşağıdaki bağlantıyı kullan:

                %s

                Eğer bunu sen istemediysen, bu maili yok say. Hesabına dokunulmadı.
                """.formatted(props.tokenTtlHours(), link);
        emailService.send(user.getEmail(), "PollNet — şifre sıfırlama", body);
    }

    @Transactional
    public void confirm(String token, String newPassword) {
        PasswordReset pr = repo.findByToken(token)
                .orElseThrow(() -> ApiException.badRequest("INVALID_TOKEN", "Bağlantı geçersiz"));
        if (pr.isConsumed()) throw ApiException.badRequest("ALREADY_CONSUMED", "Bu bağlantı zaten kullanıldı");
        if (pr.isExpired())  throw ApiException.badRequest("TOKEN_EXPIRED",   "Bağlantının süresi dolmuş");

        User user = pr.getUser();
        user.setPasswordHash(encoder.encode(newPassword));
        pr.setConsumedAt(OffsetDateTime.now());
    }

    private static String randomToken() {
        byte[] b = new byte[32];
        RNG.nextBytes(b);
        return TOKEN.encodeToString(b);
    }
}
