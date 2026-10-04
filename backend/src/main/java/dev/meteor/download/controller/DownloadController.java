package dev.meteor.download.controller;

import dev.meteor.auth.security.MeteorUserPrincipal;
import dev.meteor.download.dto.DownloadFilePayload;
import dev.meteor.download.service.DownloadService;
import java.nio.charset.StandardCharsets;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/downloads")
public class DownloadController {

    private final DownloadService downloadService;

    public DownloadController(DownloadService downloadService) {
        this.downloadService = downloadService;
    }

    @GetMapping("/{token}")
    public ResponseEntity<Resource> download(
            @AuthenticationPrincipal MeteorUserPrincipal principal,
            @PathVariable String token
    ) {
        DownloadFilePayload file = downloadService.consume(principal.id(), token);
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(safeFilename(file.filename()), StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .contentType(safeMediaType(file.mediaType()))
                .contentLength(file.sizeBytes())
                .cacheControl(CacheControl.noStore())
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .header("X-Content-SHA256", file.sha256())
                .body(file.resource());
    }

    private static String safeFilename(String filename) {
        if (filename == null || filename.isBlank()) {
            return "download";
        }
        StringBuilder sanitized = new StringBuilder(filename.length());
        for (int index = 0; index < filename.length(); index++) {
            char current = filename.charAt(index);
            if (Character.isISOControl(current) || current == '/' || current == '\\') {
                sanitized.append('_');
            } else {
                sanitized.append(current);
            }
        }
        String value = sanitized.toString().trim();
        return value.isEmpty() ? "download" : value;
    }

    private static MediaType safeMediaType(String mediaType) {
        if (mediaType == null || mediaType.isBlank()) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
        try {
            return MediaType.parseMediaType(mediaType);
        } catch (IllegalArgumentException exception) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }
}
