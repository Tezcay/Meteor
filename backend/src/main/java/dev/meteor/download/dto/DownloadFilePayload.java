package dev.meteor.download.dto;

public record DownloadFilePayload(
        String filename,
        String mediaType,
        String sha256,
        byte[] content
) {
}
