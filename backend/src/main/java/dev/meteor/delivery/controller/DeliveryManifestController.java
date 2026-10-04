package dev.meteor.delivery.controller;

import dev.meteor.auth.security.MeteorUserPrincipal;
import dev.meteor.delivery.dto.DeliveryManifestResponse;
import dev.meteor.delivery.service.DeliveryManifestService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/library/{grantId}/releases/{releaseId}/manifest")
public class DeliveryManifestController {

    private final DeliveryManifestService manifestService;

    public DeliveryManifestController(DeliveryManifestService manifestService) {
        this.manifestService = manifestService;
    }

    @GetMapping
    public DeliveryManifestResponse getManifest(
            @AuthenticationPrincipal MeteorUserPrincipal principal,
            @PathVariable long grantId,
            @PathVariable long releaseId
    ) {
        return manifestService.getAccessibleManifest(principal.id(), grantId, releaseId);
    }
}
