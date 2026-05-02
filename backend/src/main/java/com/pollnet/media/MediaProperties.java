package com.pollnet.media;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "pollnet.media")
public record MediaProperties(
        String storageDir,
        String publicBasePath,
        long maxAvatarBytes,
        long maxAttachmentBytes
) {
    public String storageDirOrDefault() {
        return storageDir == null || storageDir.isBlank() ? "./media" : storageDir;
    }
    public String publicBasePathOrDefault() {
        return publicBasePath == null || publicBasePath.isBlank() ? "/media" : publicBasePath;
    }
    public long maxAvatarBytesOrDefault()     { return maxAvatarBytes     <= 0 ? 2L * 1024 * 1024 : maxAvatarBytes; }
    public long maxAttachmentBytesOrDefault() { return maxAttachmentBytes <= 0 ? 8L * 1024 * 1024 : maxAttachmentBytes; }
}
