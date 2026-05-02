package com.pollnet.poll;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pollnet.IntegrationTestBase;
import com.pollnet.TestSeed;
import com.pollnet.auth.AuthService;
import com.pollnet.auth.dto.AuthResponse;
import com.pollnet.auth.dto.LoginRequest;
import com.pollnet.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PollVoteResultsIT extends IntegrationTestBase {

    @Autowired TestSeed seed;
    @Autowired AuthService authService;

    User author;
    User voter;
    String authorToken;
    String voterToken;

    @BeforeEach
    void setup() {
        long n = System.nanoTime();
        author = seed.createUser("author_" + n,     "author_" + n + "@x.io", "hunter22");
        voter  = seed.createUser("voter_"  + (n+1), "voter_"  + n + "@x.io", "hunter22");
        authorToken = authService.login(new LoginRequest(author.getUsername(), "hunter22")).accessToken();
        voterToken  = authService.login(new LoginRequest(voter.getUsername(),  "hunter22")).accessToken();
    }

    @Test
    void create_poll_then_vote_then_see_results() throws Exception {
        // 1. Author creates a SINGLE poll with AFTER_VOTE results
        String createBody = """
                {
                  "title": "Test poll",
                  "resultsVisibility": "AFTER_VOTE",
                  "openAnswersVisibility": "PUBLIC",
                  "questions": [
                    {"type":"SINGLE","prompt":"A or B?","payload":{"options":["A","B"]}}
                  ]
                }
                """;
        MvcResult created = mvc.perform(post("/api/polls")
                        .header("Authorization", "Bearer " + authorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.questions[0].type").value("SINGLE"))
                .andReturn();
        var node = new ObjectMapper().readTree(created.getResponse().getContentAsString());
        String pollId = node.get("id").asText();
        String questionId = node.get("questions").get(0).get("id").asText();

        // 2. Non-voter (other voter) tries to read results → 403 VOTE_REQUIRED
        mvc.perform(get("/api/polls/" + pollId + "/results")
                        .header("Authorization", "Bearer " + voterToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("VOTE_REQUIRED"));

        // 3. Voter submits answer
        String answersBody = json.writeValueAsString(Map.of(
                "answers", new Object[] {
                        Map.of("questionId", questionId, "payload", Map.of("selectedIndex", 0))
                }
        ));
        mvc.perform(post("/api/polls/" + pollId + "/answers")
                        .header("Authorization", "Bearer " + voterToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(answersBody))
                .andExpect(status().isCreated());

        // 4. Voter can now see results
        mvc.perform(get("/api/polls/" + pollId + "/results")
                        .header("Authorization", "Bearer " + voterToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results[0].data.totalVotes").value(1))
                .andExpect(jsonPath("$.results[0].data.options[0].count").value(1));

        // 5. Re-voting on same poll → 409 ALREADY_ANSWERED
        mvc.perform(post("/api/polls/" + pollId + "/answers")
                        .header("Authorization", "Bearer " + voterToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(answersBody))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ALREADY_ANSWERED"));
    }

    @Test
    void invalid_question_payload_is_rejected_at_create() throws Exception {
        String body = """
                {
                  "title": "Bad",
                  "questions": [
                    {"type":"LIKERT","prompt":"x","payload":{"scale":1}}
                  ]
                }
                """;
        mvc.perform(post("/api/polls")
                        .header("Authorization", "Bearer " + authorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_QUESTION"));
    }
}
