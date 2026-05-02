package com.pollnet.messaging;

import com.pollnet.common.error.ApiException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * AES-GCM at-rest encryption for direct messages.
 *
 * NOTE: This is "server-side encrypted", NOT end-to-end. The server holds the
 * key and can read every message. The threat model this defeats is "attacker
 * with read-only DB access" (e.g. backup leak). It does NOT defend against a
 * compromised application server. Real E2EE requires per-user keypairs and a
 * Signal-protocol-style ratchet, which is intentionally out of scope here.
 */
@Slf4j
@Component
public class MessageCipher {

    private static final int IV_LEN  = 12;        // GCM standard
    private static final int TAG_BIT = 128;
    private static final SecureRandom RNG = new SecureRandom();

    private final SecretKey key;

    public MessageCipher(MessagingProperties props) {
        if (props.secret() == null || props.secret().getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("pollnet.messaging.secret must be >= 32 bytes");
        }
        try {
            // Derive a 256-bit key from the configured secret via SHA-256.
            byte[] hashed = MessageDigest.getInstance("SHA-256")
                    .digest(props.secret().getBytes(StandardCharsets.UTF_8));
            this.key = new SecretKeySpec(hashed, "AES");
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to derive messaging key", ex);
        }
    }

    public String encrypt(String plaintext) {
        try {
            byte[] iv = new byte[IV_LEN];
            RNG.nextBytes(iv);
            Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
            c.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_BIT, iv));
            byte[] ct = c.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            byte[] out = new byte[iv.length + ct.length];
            System.arraycopy(iv, 0, out, 0, iv.length);
            System.arraycopy(ct, 0, out, iv.length, ct.length);
            return Base64.getEncoder().encodeToString(out);
        } catch (Exception ex) {
            log.error("Message encryption failed", ex);
            throw ApiException.badRequest("ENCRYPT_FAILED", "Mesaj şifrelenemedi");
        }
    }

    public String decrypt(String ciphertext) {
        try {
            byte[] all = Base64.getDecoder().decode(ciphertext);
            byte[] iv = new byte[IV_LEN];
            byte[] ct = new byte[all.length - IV_LEN];
            System.arraycopy(all, 0, iv, 0, IV_LEN);
            System.arraycopy(all, IV_LEN, ct, 0, ct.length);
            Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
            c.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_BIT, iv));
            return new String(c.doFinal(ct), StandardCharsets.UTF_8);
        } catch (Exception ex) {
            // We don't surface details; a corrupted message just shows as opaque.
            log.error("Message decryption failed (data may be corrupt or key changed)");
            return "[mesaj çözülemedi]";
        }
    }
}
