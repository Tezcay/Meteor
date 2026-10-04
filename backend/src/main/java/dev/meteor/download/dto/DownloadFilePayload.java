package dev.meteor.download.dto;

import org.springframework.core.io.Resource;

public record DownloadFilePayload(
        String filename,
        String mediaType,
        String sha256,
        long sizeBytes,
        Resource resource
) {
}
