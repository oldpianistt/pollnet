package com.pollnet.user;

import com.pollnet.auth.CurrentUser;
import com.pollnet.follow.FollowService;
import com.pollnet.poll.PollService;
import com.pollnet.poll.dto.PollView;
import com.pollnet.user.dto.MeView;
import com.pollnet.user.dto.PublicProfileView;
import com.pollnet.user.dto.UpdateMeRequest;
import com.pollnet.user.dto.UserSummary;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final FollowService followService;
    private final PollService pollService;

    @GetMapping("/me")
    public MeView me() {
        return userService.me(CurrentUser.requiredId());
    }

    @PatchMapping("/me")
    public MeView updateMe(@Valid @RequestBody UpdateMeRequest req) {
        return userService.updateMe(CurrentUser.requiredId(), req);
    }

    /**
     * Frontend pings this every ~30s to record presence. No body, no rate-limit
     * (light DB write); the row is updated in-place.
     */
    @org.springframework.web.bind.annotation.PostMapping("/me/heartbeat")
    public org.springframework.http.ResponseEntity<Void> heartbeat() {
        userService.touchPresence(CurrentUser.requiredId());
        return org.springframework.http.ResponseEntity.noContent().build();
    }

    @GetMapping("/{username}")
    public PublicProfileView profile(@PathVariable String username) {
        UUID viewerId = CurrentUser.get().map(u -> u.id()).orElse(null);
        return userService.publicProfile(username, viewerId);
    }

    @GetMapping("/{username}/polls")
    public List<PollView> userPolls(@PathVariable String username) {
        UUID viewerId = CurrentUser.get().map(u -> u.id()).orElse(null);
        return pollService.listByAuthor(username, viewerId);
    }

    @PostMapping("/{username}/follow")
    public ResponseEntity<Void> follow(@PathVariable String username) {
        followService.follow(CurrentUser.requiredId(), username);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{username}/follow")
    public ResponseEntity<Void> unfollow(@PathVariable String username) {
        followService.unfollow(CurrentUser.requiredId(), username);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{username}/followers")
    public List<UserSummary> followers(@PathVariable String username) {
        return followService.followers(username);
    }

    @GetMapping("/{username}/following")
    public List<UserSummary> following(@PathVariable String username) {
        return followService.following(username);
    }
}
