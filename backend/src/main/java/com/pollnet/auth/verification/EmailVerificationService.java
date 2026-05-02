package com.pollnet.auth.verification;

import com.pollnet.common.error.ApiException;
import com.pollnet.config.EmailVerificationProperties;
import com.pollnet.config.MailProperties;
import com.pollnet.mail.EmailService;
import com.pollnet.user.User;
import com.pollnet.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.Base64;

@Service
@RequiredArgsConstructor
public class EmailVerificationService {

    private static final SecureRandom RNG = new SecureRandom();
    private static final Base64.Encoder TOKEN = Base64.getUrlEncoder().withoutPadding();

    private final EmailVerificationRepository repo;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final MailProperties mailProps;
    private final EmailVerificationProperties props;

    @Transactional
    public void issueFor(User user) {
        String token = randomToken();
        OffsetDateTime expiresAt = OffsetDateTime.now().plusHours(props.tokenTtlHours());
        EmailVerification ev = EmailVerification.builder()
                .user(user)
                .token(token)
                .expiresAt(expiresAt)
                .build();
        repo.save(ev);

        String link = mailProps.publicBaseUrl() + "/verify-email?token=" + token;
        String body = """
                Merhaba %s,

                PollNet hesabını doğrulamak için aşağıdaki bağlantıyı kullan:
                %s

                Bağlantı %d saat sonra geçersiz olur.
                """.formatted(displayNameOrUsername(user), link, props.tokenTtlHours());
        emailService.send(user.getEmail(), "PollNet — e-posta doğrulama", body);
    }

    @Transactional
    public void verify(String token) {
        EmailVerification ev = repo.findByToken(token)
                .orElseThrow(() -> ApiException.badRequest("INVALID_TOKEN", "Doğrulama bağlantısı geçersiz"));
        if (ev.isConsumed()) throw ApiException.badRequest("ALREADY_CONSUMED", "Bu bağlantı zaten kullanıldı");
        if (ev.isExpired())  throw ApiException.badRequest("TOKEN_EXPIRED",   "Bağlantının süresi dolmuş");

        User user = ev.getUser();
        if (user.getEmailVerifiedAt() == null) {
            user.setEmailVerifiedAt(OffsetDateTime.now());
            userRepository.save(user);
        }
        ev.setConsumedAt(OffsetDateTime.now());
    }

    private static String randomToken() {
        byte[] b = new byte[32];
        RNG.nextBytes(b);
        return TOKEN.encodeToString(b);
    }

    private static String displayNameOrUsername(User u) {
        return u.getDisplayName() != null && !u.getDisplayName().isBlank() ? u.getDisplayName() : u.getUsername();
    }
}
