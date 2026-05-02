package com.pollnet.user;

import com.pollnet.auth.CurrentUser;
import com.pollnet.common.error.ApiException;
import com.pollnet.media.MediaProperties;
import com.pollnet.media.MediaStorage;
import com.pollnet.user.dto.MeView;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/users/me/avatar")
@RequiredArgsConstructor
public class UserMediaController {

    private final UserRepository userRepository;
    private final MediaStorage   storage;
    private final MediaProperties mediaProps;

    @PostMapping
    @Transactional
    public MeView upload(@RequestParam("file") MultipartFile file) {
        var meId = CurrentUser.requiredId();
        User u = userRepository.findById(meId)
                .orElseThrow(() -> ApiException.unauthorized("UNAUTHENTICATED", "User not found"));
        var stored = storage.storeImage(file, "avatars", mediaProps.maxAvatarBytesOrDefault());
        u.setAvatarUrl(stored.publicUrl());
        return MeView.from(u);
    }

    @DeleteMapping
    @Transactional
    public ResponseEntity<Void> remove() {
        var meId = CurrentUser.requiredId();
        userRepository.findById(meId).ifPresent(u -> u.setAvatarUrl(null));
        return ResponseEntity.noContent().build();
    }
}
