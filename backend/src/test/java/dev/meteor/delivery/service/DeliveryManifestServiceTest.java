package dev.meteor.delivery.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.meteor.delivery.dto.DeliveryManifestResponse;
import dev.meteor.delivery.mapper.DeliveryManifestMapper;
import dev.meteor.delivery.mapper.projection.DeliveryManifestProjection;
import dev.meteor.delivery.mapper.projection.ManifestItemProjection;
import dev.meteor.entitlement.mapper.EntitlementMapper;
import dev.meteor.entitlement.mapper.projection.EntitlementGrantProjection;
import dev.meteor.entitlement.mapper.projection.LibraryItemProjection;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class DeliveryManifestServiceTest {

    private final StubEntitlementMapper entitlementMapper = new StubEntitlementMapper();
    private final StubDeliveryManifestMapper manifestMapper = new StubDeliveryManifestMapper();
    private final DeliveryManifestService manifestService =
            new DeliveryManifestService(entitlementMapper, manifestMapper);

    @Test
    void returnsFrozenManifestItemsAndCalculatesSummary() {
        entitlementMapper.grant = Optional.of(grant());
        manifestMapper.manifest = Optional.of(manifest());
        manifestMapper.items = List.of(
                item(6001L, 1, "PRIMARY", "简历模板", "resume.html", 634L, "a".repeat(64)),
                item(6002L, 2, "GUIDE", "使用说明", "guide.md", 311L, "b".repeat(64))
        );

        DeliveryManifestResponse result = manifestService.getAccessibleManifest(2L, 5001L, 3001L);

        assertThat(entitlementMapper.requestedUserId).isEqualTo(2L);
        assertThat(entitlementMapper.requestedGrantId).isEqualTo(5001L);
        assertThat(manifestMapper.requestedReleaseId).isEqualTo(3001L);
        assertThat(manifestMapper.requestedPackageId).isEqualTo(2001L);
        assertThat(result.itemCount()).isEqualTo(2);
        assertThat(result.totalBytes()).isEqualTo(945L);
        assertThat(result.items()).extracting(item -> item.displayName())
                .containsExactly("简历模板", "使用说明");
        assertThat(result.items()).extracting(item -> item.sha256())
                .containsExactly("a".repeat(64), "b".repeat(64));
    }

    @Test
    void returnsAnEmptyFrozenManifestWithoutInventingFiles() {
        entitlementMapper.grant = Optional.of(grant());
        manifestMapper.manifest = Optional.of(manifest());

        DeliveryManifestResponse result = manifestService.getAccessibleManifest(2L, 5001L, 3001L);

        assertThat(result.itemCount()).isZero();
        assertThat(result.totalBytes()).isZero();
        assertThat(result.items()).isEmpty();
    }

    @Test
    void rejectsMissingRevokedOrForeignGrantBeforeLookingUpARelease() {
        assertThatThrownBy(() -> manifestService.getAccessibleManifest(2L, 5999L, 3001L))
                .isInstanceOf(EntitlementGrantNotFoundException.class);
        assertThat(manifestMapper.headerCalls).isZero();
        assertThat(manifestMapper.itemCalls).isZero();
    }

    @Test
    void hidesAReleaseOutsideTheGrantedPackageBehindNotFound() {
        entitlementMapper.grant = Optional.of(grant());

        assertThatThrownBy(() -> manifestService.getAccessibleManifest(2L, 5001L, 3999L))
                .isInstanceOf(DeliveryManifestNotFoundException.class);
        assertThat(manifestMapper.requestedPackageId).isEqualTo(2001L);
        assertThat(manifestMapper.itemCalls).isZero();
    }

    private static EntitlementGrantProjection grant() {
        return new EntitlementGrantProjection(
                5001L, 2L, 8001L, 1001L, 2001L, "ALL_RELEASES", "ACTIVE",
                Instant.parse("2026-10-03T00:00:00Z"), null
        );
    }

    private static DeliveryManifestProjection manifest() {
        return new DeliveryManifestProjection(
                4001L, 3001L, "1.0.0", Instant.parse("2026-10-03T00:00:00Z")
        );
    }

    private static ManifestItemProjection item(
            long id,
            int position,
            String role,
            String displayName,
            String filename,
            long sizeBytes,
            String sha256
    ) {
        return new ManifestItemProjection(
                id, position, role, displayName, filename, "text/plain", sizeBytes, sha256
        );
    }

    private static final class StubEntitlementMapper implements EntitlementMapper {
        private Optional<EntitlementGrantProjection> grant = Optional.empty();
        private long requestedGrantId;
        private long requestedUserId;

        @Override
        public int ensureGrantForPaidOrder(long orderId, long userId) {
            return 0;
        }

        @Override
        public Optional<EntitlementGrantProjection> findByOrderIdAndUserId(long orderId, long userId) {
            return Optional.empty();
        }

        @Override
        public Optional<EntitlementGrantProjection> findActiveByIdAndUserId(long grantId, long userId) {
            requestedGrantId = grantId;
            requestedUserId = userId;
            return grant;
        }

        @Override
        public List<LibraryItemProjection> findActiveLibraryByUserId(long userId) {
            return List.of();
        }
    }

    private static final class StubDeliveryManifestMapper implements DeliveryManifestMapper {
        private Optional<DeliveryManifestProjection> manifest = Optional.empty();
        private List<ManifestItemProjection> items = List.of();
        private long requestedReleaseId;
        private long requestedPackageId;
        private int headerCalls;
        private int itemCalls;

        @Override
        public Optional<DeliveryManifestProjection> findPublishedByReleaseIdAndPackageId(
                long releaseId,
                long packageId
        ) {
            headerCalls++;
            requestedReleaseId = releaseId;
            requestedPackageId = packageId;
            return manifest;
        }

        @Override
        public List<ManifestItemProjection> findItemsByManifestId(long manifestId) {
            itemCalls++;
            return items;
        }
    }
}
