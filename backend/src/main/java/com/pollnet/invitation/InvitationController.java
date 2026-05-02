package com.pollnet.invitation;

import com.pollnet.auth.CurrentUser;
import com.pollnet.invitation.dto.InvitationView;
import com.pollnet.invitation.dto.QuotaView;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/invitations")
@RequiredArgsConstructor
public class InvitationController {

    private final InvitationService invitationService;

    @GetMapping
    public List<InvitationView> myInvitations() {
        return invitationService.listMyUnused(CurrentUser.requiredId());
    }

    @PostMapping
    public ResponseEntity<InvitationView> create() {
        InvitationView view = invitationService.createForUser(CurrentUser.requiredId());
        return ResponseEntity.status(HttpStatus.CREATED).body(view);
    }

    @GetMapping("/quota")
    public QuotaView quota() {
        return invitationService.quotaFor(CurrentUser.requiredId());
    }
}
