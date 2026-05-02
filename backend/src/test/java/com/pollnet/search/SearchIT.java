package com.pollnet.search;

import com.pollnet.IntegrationTestBase;
import com.pollnet.TestSeed;
import com.pollnet.auth.AuthService;
import com.pollnet.auth.dto.LoginRequest;
import com.pollnet.user.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SearchIT extends IntegrationTestBase {

    @Autowired TestSeed seed;
    @Autowired AuthService authService;

    @Test
    void search_finds_polls_by_title_substring() throws Exception {
        long n = System.nanoTime();
        User author = seed.createUser("searchau_" + n, "sa_" + n + "@x.io", "hunter22");
        String token = authService.login(new LoginRequest(author.getUsername(), "hunter22")).accessToken();

        String body = """
                {
                  "title": "Hangi yaz tatil yeri en güzel?",
                  "questions": [{"type":"SINGLE","prompt":"x","payload":{"options":["A","B"]}}]
                }
                """;
        MvcResult ok = mvc.perform(post("/api/polls")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn();

        mvc.perform(get("/api/search/polls").param("q", "yaz tatil")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(1)));
    }

    @Test
    void short_query_is_rejected() throws Exception {
        long n = System.nanoTime();
        User u = seed.createUser("u_" + n, "u_" + n + "@x.io", "hunter22");
        String token = authService.login(new LoginRequest(u.getUsername(), "hunter22")).accessToken();
        mvc.perform(get("/api/search/users").param("q", "a")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("QUERY_TOO_SHORT"));
    }
}
