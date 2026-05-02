package com.pollnet.alice;

import com.pollnet.auth.CurrentUser;
import com.pollnet.ratelimit.RateLimiter;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.UUID;

@RestController
@RequestMapping("/api/alice")
@RequiredArgsConstructor
public class AliceController {

    private final AliceService  alice;
    private final RateLimiter   rateLimiter;
    private final AnthropicProperties props;

    @GetMapping("/status")
    public StatusView status() {
        return new StatusView(props.enabled());
    }

    @PostMapping("/suggest-poll")
    public AlicePollDraft suggest(@Valid @RequestBody SuggestRequest req) {
        UUID userId = CurrentUser.requiredId();
        // Strict ceiling — Claude calls cost real money. 5/min/user, 30/hour/user.
        rateLimiter.check("alice-suggest-min",  userId.toString(),  5, Duration.ofMinutes(1));
        rateLimiter.check("alice-suggest-hour", userId.toString(), 30, Duration.ofHours(1));
        return alice.suggest(req.prompt());
    }

    public record SuggestRequest(@NotBlank @Size(max = 2000) String prompt) {}

    public record StatusView(boolean enabled) {}
}
