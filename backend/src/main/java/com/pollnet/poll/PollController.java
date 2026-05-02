package com.pollnet.poll;

import com.pollnet.auth.CurrentUser;
import com.pollnet.poll.answer.AnswerService;
import com.pollnet.poll.dto.AnswersSubmitRequest;
import com.pollnet.poll.dto.PollCreateRequest;
import com.pollnet.poll.dto.PollView;
import com.pollnet.poll.dto.ResultsView;
import com.pollnet.ratelimit.RateLimiter;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.Duration;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/polls")
@RequiredArgsConstructor
public class PollController {

    private final PollService pollService;
    private final AnswerService answerService;
    private final RateLimiter rateLimiter;

    @PostMapping
    public ResponseEntity<PollView> create(@Valid @RequestBody PollCreateRequest req) {
        UUID userId = CurrentUser.requiredId();
        rateLimiter.check("poll-create", userId.toString(), 10, Duration.ofHours(1));
        PollView created = pollService.create(userId, req);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/{id}")
    public PollView get(@PathVariable UUID id) {
        UUID viewerId = CurrentUser.get().map(u -> u.id()).orElse(null);
        return pollService.get(id, viewerId);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        pollService.delete(id, CurrentUser.requiredId());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/answers")
    public ResponseEntity<Void> submitAnswers(@PathVariable UUID id,
                                              @Valid @RequestBody AnswersSubmitRequest req) {
        UUID userId = CurrentUser.requiredId();
        rateLimiter.check("vote", userId.toString(), 60, Duration.ofMinutes(1));
        answerService.submit(id, userId, req);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping("/{id}/results")
    public ResultsView results(@PathVariable UUID id) {
        UUID viewerId = CurrentUser.get().map(u -> u.id()).orElse(null);
        return answerService.resultsFor(id, viewerId);
    }
}
