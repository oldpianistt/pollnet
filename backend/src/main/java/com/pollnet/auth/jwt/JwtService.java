package com.pollnet.auth.jwt;

import com.pollnet.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Slf4j
@Service
public class JwtService {

    private final JwtProperties props;
    private final SecretKey signingKey;

    public JwtService(JwtProperties props) {
        this.props = props;
        byte[] keyBytes = props.secret().getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            throw new IllegalStateException("pollnet.jwt.secret must be >= 32 bytes for HS256");
        }
        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
    }

    public String issueAccessToken(UUID userId, String username) {
        Instant now = Instant.now();
        Instant exp = now.plus(Duration.ofMinutes(props.accessTtlMinutes()));
        return Jwts.builder()
                .issuer(props.issuer())
                .subject(userId.toString())
                .claim("username", username)
                .issuedAt(Date.from(now))
                .expiration(Date.from(exp))
                .signWith(signingKey, Jwts.SIG.HS256)
                .compact();
    }

    public ParsedToken parse(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(signingKey)
                    .requireIssuer(props.issuer())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return new ParsedToken(
                    UUID.fromString(claims.getSubject()),
                    claims.get("username", String.class),
                    claims.getExpiration().toInstant()
            );
        } catch (JwtException | IllegalArgumentException ex) {
            log.debug("JWT parse failure: {}", ex.getMessage());
            throw new InvalidJwtException("Token is invalid or expired", ex);
        }
    }

    public long accessTtlSeconds() {
        return Duration.ofMinutes(props.accessTtlMinutes()).toSeconds();
    }

    public long refreshTtlSeconds() {
        return Duration.ofDays(props.refreshTtlDays()).toSeconds();
    }

    public record ParsedToken(UUID userId, String username, Instant expiresAt) {}
}
