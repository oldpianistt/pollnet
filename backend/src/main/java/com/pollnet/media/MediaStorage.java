package com.pollnet.media;

import com.pollnet.common.error.ApiException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * Local-disk media storage. To swap for S3-compatible storage later, replace
 * the body of {@link #store} with an SDK call and update
 * {@link MediaProperties#publicBasePath()} to a CDN/bucket URL prefix.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MediaStorage {

    private static final Set<String> ALLOWED_IMAGE_EXTENSIONS = Set.of("jpg", "jpeg", "png", "gif", "webp");
    private static final Set<String> ALLOWED_IMAGE_MIME       = Set.of(
            "image/jpeg", "image/png", "image/gif", "image/webp");

    private final MediaProperties props;

    public StoredFile storeImage(MultipartFile file, String subdir, long maxBytes) {
        if (file == null || file.isEmpty()) {
            throw ApiException.badRequest("EMPTY_FILE", "Dosya boş");
        }
        if (file.getSize() > maxBytes) {
            throw ApiException.badRequest("FILE_TOO_LARGE",
                    "Dosya çok büyük (en fazla " + (maxBytes / 1024) + " KB)");
        }
        String contentType = file.getContentType() == null ? "" : file.getContentType().toLowerCase(Locale.ROOT);
        if (!ALLOWED_IMAGE_MIME.contains(contentType)) {
            throw ApiException.badRequest("UNSUPPORTED_TYPE",
                    "Yalnızca JPEG/PNG/GIF/WebP desteklenir");
        }

        String ext = guessExtension(contentType, file.getOriginalFilename());
        String filename = UUID.randomUUID() + "." + ext;
        Path dir = Path.of(props.storageDirOrDefault(), subdir);
        Path target = dir.resolve(filename);

        try {
            Files.createDirectories(dir);
            try (var in = file.getInputStream()) {
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException ex) {
            log.error("Failed to write upload to {}", target, ex);
            throw ApiException.badRequest("STORAGE_FAILURE", "Dosya kaydedilemedi");
        }

        // Public URL: prefix + subdir + filename. nginx (or Spring's resource handler)
        // will serve the file. The controller mounts /media/** to disk.
        String publicUrl = props.publicBasePathOrDefault() + "/" + subdir + "/" + filename;
        return new StoredFile(target, publicUrl);
    }

    private static String guessExtension(String contentType, String originalName) {
        return switch (contentType) {
            case "image/jpeg" -> "jpg";
            case "image/png"  -> "png";
            case "image/gif"  -> "gif";
            case "image/webp" -> "webp";
            default -> {
                if (originalName == null) yield "bin";
                int dot = originalName.lastIndexOf('.');
                String ext = dot < 0 ? "" : originalName.substring(dot + 1).toLowerCase(Locale.ROOT);
                yield ALLOWED_IMAGE_EXTENSIONS.contains(ext) ? ext : "bin";
            }
        };
    }

    public record StoredFile(Path path, String publicUrl) {}
}
