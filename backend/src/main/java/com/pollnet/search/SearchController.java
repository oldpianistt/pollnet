package com.pollnet.search;

import com.pollnet.auth.CurrentUser;
import com.pollnet.common.pagination.CursorPage;
import com.pollnet.poll.dto.PollView;
import com.pollnet.user.dto.UserSummary;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
public class SearchController {

    private final SearchService service;

    @GetMapping("/polls")
    public CursorPage<PollView> searchPolls(@RequestParam("q") String q,
                                            @RequestParam(defaultValue = "20") int limit) {
        UUID viewerId = CurrentUser.get().map(u -> u.id()).orElse(null);
        return service.searchPolls(q, limit, viewerId);
    }

    @GetMapping("/users")
    public List<UserSummary> searchUsers(@RequestParam("q") String q,
                                         @RequestParam(defaultValue = "20") int limit) {
        return service.searchUsers(q, limit);
    }
}
