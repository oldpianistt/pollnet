package com.pollnet.auth;

import com.pollnet.auth.dto.AuthResponse;
import com.pollnet.auth.dto.ForgotPasswordRequest;
import com.pollnet.auth.dto.LoginRequest;
import com.pollnet.auth.dto.RefreshRequest;
import com.pollnet.auth.dto.RegisterRequest;
import com.pollnet.auth.dto.ResetPasswordRequest;
import com.pollnet.auth.dto.VerifyEmailRequest;
import com.pollnet.auth.passwordreset.PasswordResetService;
import com.pollnet.auth.verification.EmailVerificationService;
import com.pollnet.ratelimit.RateLimiter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final EmailVerificationService verificationService;
    private final PasswordResetService passwordResetService;
    private final RateLimiter rateLimiter;

    private static final Duration AUTH_WINDOW = Duration.ofMinutes(1);
    private static final int AUTH_LIMIT = 5;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest req,
                                                 HttpServletRequest http) {
        rateLimiter.check("auth", RateLimiter.clientIp(http), AUTH_LIMIT, AUTH_WINDOW);
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(req));
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest req, HttpServletRequest http) {
        rateLimiter.check("auth", RateLimiter.clientIp(http), AUTH_LIMIT, AUTH_WINDOW);
        return authService.login(req);
    }

    @PostMapping("/refresh")
    public AuthResponse refresh(@Valid @RequestBody RefreshRequest req) {
        return authService.refresh(req);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestBody(required = false) RefreshRequest req) {
        authService.logout(req == null ? null : req.refreshToken());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/verify-email")
    public ResponseEntity<Void> verifyEmail(@Valid @RequestBody VerifyEmailRequest req) {
        verificationService.verify(req.token());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest req,
                                               HttpServletRequest http) {
        // Rate-limited per IP so the endpoint can't be used as an email-bomb relay.
        rateLimiter.check("auth", RateLimiter.clientIp(http), AUTH_LIMIT, AUTH_WINDOW);
        passwordResetService.requestReset(req.email());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest req) {
        passwordResetService.confirm(req.token(), req.newPassword());
        return ResponseEntity.noContent().build();
    }
}
