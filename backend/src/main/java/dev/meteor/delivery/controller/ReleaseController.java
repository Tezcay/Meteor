package dev.meteor.delivery.controller;

import dev.meteor.auth.security.MeteorUserPrincipal;
import dev.meteor.delivery.dto.ReleaseResponse;
import dev.meteor.delivery.service.ReleaseService;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/library/{grantId}/releases")
public class ReleaseController {

    private final ReleaseService releaseService;

    public ReleaseController(ReleaseService releaseService) {
        this.releaseService = releaseService;
    }

    @GetMapping
    public List<ReleaseResponse> getReleases(
            @AuthenticationPrincipal MeteorUserPrincipal principal,
            @PathVariable long grantId
    ) {
        return releaseService.getAccessibleReleases(principal.id(), grantId);
    }
}
