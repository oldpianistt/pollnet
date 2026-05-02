package com.pollnet.auth;

import com.pollnet.IntegrationTestBase;
import com.pollnet.TestSeed;
import com.pollnet.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.util.Map;

import static org.hamcrest.Matchers.hasLength;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthFlowIT extends IntegrationTestBase {

    @Autowired TestSeed seed;

    User inviter;
    String inviteToken;

    @BeforeEach
    void setupUser() {
        // Usernames must satisfy users_username_format ([a-zA-Z0-9_]{3,32}); avoid hyphens.
        long n = System.nanoTime();
        inviter = seed.createUser("inviter_" + n, "inv_" + n + "@x.io", "hunter22");
        inviteToken = "VALID_TOKEN_" + n;
        seed.issueInvite(inviter, inviteToken);
    }

    @Test
    void register_with_invalid_invite_returns_400() throws Exception {
        String body = json.writeValueAsString(Map.of(
                "inviteToken", "DOES_NOT_EXIST",
                "username", "alice",
                "email", "alice@x.io",
                "password", "hunter22"
        ));
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
           .andExpect(status().isBadRequest())
           .andExpect(jsonPath("$.code").value("INVALID_INVITE"));
    }

    @Test
    void register_with_valid_invite_returns_tokens_and_user() throws Exception {
        String username = "alice_" + System.nanoTime();
        String body = json.writeValueAsString(Map.of(
                "inviteToken", inviteToken,
                "username", username,
                "email", username + "@x.io",
                "password", "hunter22",
                "displayName", "Alice"
        ));
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
           .andExpect(status().isCreated())
           .andExpect(jsonPath("$.accessToken", notNullValue()))
           .andExpect(jsonPath("$.refreshToken", hasLength(64))) // 48 bytes b64url unpadded
           .andExpect(jsonPath("$.user.username").value(username));
    }

    @Test
    void login_with_bad_password_returns_401() throws Exception {
        String body = json.writeValueAsString(Map.of(
                "usernameOrEmail", inviter.getUsername(),
                "password", "wrong-password"
        ));
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(body))
           .andExpect(status().isUnauthorized())
           .andExpect(jsonPath("$.code").value("BAD_CREDENTIALS"));
    }

    @Test
    void login_succeeds_for_seeded_user() throws Exception {
        String body = json.writeValueAsString(Map.of(
                "usernameOrEmail", inviter.getUsername(),
                "password", "hunter22"
        ));
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(body))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.user.username").value(inviter.getUsername()));
    }

    @Test
    void rate_limit_kicks_in_after_5_login_attempts() throws Exception {
        String body = json.writeValueAsString(Map.of(
                "usernameOrEmail", inviter.getUsername(),
                "password", "hunter22"
        ));
        for (int i = 0; i < 5; i++) {
            mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(body))
               .andExpect(status().isOk());
        }
        // 6th request should be rate limited (429)
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(body))
           .andExpect(status().isTooManyRequests())
           .andExpect(jsonPath("$.code").value("RATE_LIMITED"));
    }
}
