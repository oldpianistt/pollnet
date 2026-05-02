package com.pollnet.notification;

import com.pollnet.IntegrationTestBase;
import com.pollnet.TestSeed;
import com.pollnet.auth.AuthService;
import com.pollnet.auth.dto.LoginRequest;
import com.pollnet.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class NotificationFlowIT extends IntegrationTestBase {

    @Autowired TestSeed seed;
    @Autowired AuthService authService;

    @Test
    void following_a_user_creates_a_notification_for_the_followee() throws Exception {
        long n = System.nanoTime();
        User followee = seed.createUser("ee_" + n,     "ee_" + n + "@x.io", "hunter22");
        User follower = seed.createUser("er_" + (n+1), "er_" + n + "@x.io", "hunter22");

        String followerToken = authService.login(new LoginRequest(follower.getUsername(), "hunter22")).accessToken();
        String followeeToken = authService.login(new LoginRequest(followee.getUsername(), "hunter22")).accessToken();

        // Follower follows followee
        mvc.perform(post("/api/users/" + followee.getUsername() + "/follow")
                        .header("Authorization", "Bearer " + followerToken))
                .andExpect(status().isNoContent());

        // Followee should see exactly one NEW_FOLLOWER notification
        mvc.perform(get("/api/notifications")
                        .header("Authorization", "Bearer " + followeeToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].type").value("NEW_FOLLOWER"))
                .andExpect(jsonPath("$.items[0].payload.actorUsername").value(follower.getUsername()))
                .andExpect(jsonPath("$.items[0].readAt").doesNotExist());

        // Unread count == 1
        mvc.perform(get("/api/notifications/unread-count")
                        .header("Authorization", "Bearer " + followeeToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(1));
    }
}
