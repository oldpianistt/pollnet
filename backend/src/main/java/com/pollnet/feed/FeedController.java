package com.pollnet.feed;

import com.pollnet.auth.CurrentUser;
import com.pollnet.common.error.ApiException;
import com.pollnet.common.pagination.CursorPage;
import com.pollnet.poll.dto.PollView;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/feed")
@RequiredArgsConstructor
public class FeedController {

    private final FeedService feedService;

    @GetMapping
    public CursorPage<PollView> feed(@RequestParam String type,
                                     @RequestParam(required = false) String cursor,
                                     @RequestParam(defaultValue = "20") int limit) {
        UUID viewerId = CurrentUser.get().map(u -> u.id()).orElse(null);
        return switch (type) {
            case "discover" -> feedService.discover(cursor, limit, viewerId);
            case "following" -> feedService.following(cursor, limit, viewerId);
            default -> throw ApiException.badRequest("INVALID_FEED_TYPE", "type must be 'discover' or 'following'");
        };
    }
}
