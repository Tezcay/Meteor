package dev.meteor.delivery.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.meteor.delivery.dto.ReleaseResponse;
import dev.meteor.delivery.mapper.ReleaseMapper;
import dev.meteor.delivery.mapper.projection.ReleaseProjection;
import dev.meteor.entitlement.mapper.EntitlementMapper;
import dev.meteor.entitlement.mapper.projection.EntitlementGrantProjection;
import dev.meteor.entitlement.mapper.projection.LibraryItemProjection;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ReleaseServiceTest {

    private final StubEntitlementMapper entitlementMapper = new StubEntitlementMapper();
    private final StubReleaseMapper releaseMapper = new StubReleaseMapper();
    private final ReleaseService releaseService = new ReleaseService(entitlementMapper, releaseMapper);

    @Test
    void listsPublishedPackageReleasesNewestFirstAndMarksOnlyTheFirstAsLatest() {
        entitlementMapper.grant = Optional.of(grant());
        releaseMapper.releases = List.of(
                release(3002L, 2, "1.1.0", "修正文档结构"),
                release(3001L, 1, "1.0.0", "首个正式版本")
        );

        List<ReleaseResponse> result = releaseService.getAccessibleReleases(2L, 5001L);

        assertThat(entitlementMapper.requestedUserId).isEqualTo(2L);
        assertThat(entitlementMapper.requestedGrantId).isEqualTo(5001L);
        assertThat(releaseMapper.requestedPackageId).isEqualTo(2001L);
        assertThat(result).extracting(ReleaseResponse::versionLabel)
                .containsExactly("1.1.0", "1.0.0");
        assertThat(result).extracting(ReleaseResponse::latest)
                .containsExactly(true, false);
    }

    @Test
    void returnsEmptyListWhenAnActiveGrantPackageHasNoPublishedRelease() {
        entitlementMapper.grant = Optional.of(grant());

        assertThat(releaseService.getAccessibleReleases(2L, 5001L)).isEmpty();
    }

    @Test
    void hidesMissingRevokedOrForeignGrantBehindNotFound() {
        assertThatThrownBy(() -> releaseService.getAccessibleReleases(2L, 5999L))
                .isInstanceOf(EntitlementGrantNotFoundException.class);
        assertThat(releaseMapper.calls).isZero();
    }

    private static EntitlementGrantProjection grant() {
        return new EntitlementGrantProjection(
                5001L, 2L, 8001L, 1001L, 2001L, "ALL_RELEASES", "ACTIVE",
                Instant.parse("2026-10-03T00:00:00Z"), null
        );
    }

    private static ReleaseProjection release(long id, int number, String label, String summary) {
        return new ReleaseProjection(
                id, 2001L, number, label, summary, Instant.parse("2026-10-03T00:00:00Z")
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

    private static final class StubReleaseMapper implements ReleaseMapper {
        private List<ReleaseProjection> releases = List.of();
        private long requestedPackageId;
        private int calls;

        @Override
        public List<ReleaseProjection> findPublishedByPackageId(long packageId) {
            calls++;
            requestedPackageId = packageId;
            return releases;
        }
    }
}
