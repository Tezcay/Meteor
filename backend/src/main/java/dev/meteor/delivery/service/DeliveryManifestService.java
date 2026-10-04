package dev.meteor.delivery.service;

import dev.meteor.delivery.dto.DeliveryManifestResponse;
import dev.meteor.delivery.dto.ManifestItemResponse;
import dev.meteor.delivery.mapper.DeliveryManifestMapper;
import dev.meteor.delivery.mapper.projection.DeliveryManifestProjection;
import dev.meteor.delivery.mapper.projection.ManifestItemProjection;
import dev.meteor.entitlement.mapper.EntitlementMapper;
import dev.meteor.entitlement.mapper.projection.EntitlementGrantProjection;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DeliveryManifestService {

    private final EntitlementMapper entitlementMapper;
    private final DeliveryManifestMapper manifestMapper;

    public DeliveryManifestService(
            EntitlementMapper entitlementMapper,
            DeliveryManifestMapper manifestMapper
    ) {
        this.entitlementMapper = entitlementMapper;
        this.manifestMapper = manifestMapper;
    }

    @Transactional(readOnly = true)
    public DeliveryManifestResponse getAccessibleManifest(long userId, long grantId, long releaseId) {
        EntitlementGrantProjection grant = entitlementMapper
                .findActiveByIdAndUserId(grantId, userId)
                .orElseThrow(() -> new EntitlementGrantNotFoundException(grantId));
        DeliveryManifestProjection manifest = manifestMapper
                .findPublishedByReleaseIdAndPackageId(releaseId, grant.packageId())
                .orElseThrow(() -> new DeliveryManifestNotFoundException(releaseId));
        List<ManifestItemResponse> items = manifestMapper.findItemsByManifestId(manifest.id()).stream()
                .map(DeliveryManifestService::toResponse)
                .toList();
        long totalBytes = items.stream().mapToLong(ManifestItemResponse::sizeBytes).sum();
        return new DeliveryManifestResponse(
                manifest.id(),
                manifest.releaseId(),
                manifest.versionLabel(),
                manifest.createdAt(),
                items.size(),
                totalBytes,
                items
        );
    }

    private static ManifestItemResponse toResponse(ManifestItemProjection item) {
        return new ManifestItemResponse(
                item.id(),
                item.position(),
                item.role(),
                item.displayName(),
                item.filename(),
                item.mediaType(),
                item.sizeBytes(),
                item.sha256()
        );
    }
}
