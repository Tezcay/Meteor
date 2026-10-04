package dev.meteor.download.controller;

import dev.meteor.auth.security.MeteorUserPrincipal;
import dev.meteor.download.dto.DownloadAuthorizationResponse;
import dev.meteor.download.service.DownloadService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/library/{grantId}/releases/{releaseId}/manifest/items/{manifestItemId}")
public class DownloadAuthorizationController {

    private final DownloadService downloadService;

    public DownloadAuthorizationController(DownloadService downloadService) {
        this.downloadService = downloadService;
    }

    @PostMapping("/download-authorizations")
    public DownloadAuthorizationResponse authorize(
            @AuthenticationPrincipal MeteorUserPrincipal principal,
            @PathVariable long grantId,
            @PathVariable long releaseId,
            @PathVariable long manifestItemId
    ) {
        return downloadService.authorize(principal.id(), grantId, releaseId, manifestItemId);
    }
}
