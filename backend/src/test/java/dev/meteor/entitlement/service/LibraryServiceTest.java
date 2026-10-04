package dev.meteor.entitlement.service;

import static org.assertj.core.api.Assertions.assertThat;

import dev.meteor.entitlement.dto.LibraryItemResponse;
import dev.meteor.entitlement.mapper.EntitlementMapper;
import dev.meteor.entitlement.mapper.projection.EntitlementGrantProjection;
import dev.meteor.entitlement.mapper.projection.LibraryItemProjection;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class LibraryServiceTest {

    @Test
    void mapsActiveGrantFactsWithoutReconstructingThemFromCurrentCatalog() {
        StubEntitlementMapper mapper = new StubEntitlementMapper();
        mapper.items = List.of(new LibraryItemProjection(
                5001L,
                7001L,
                8001L,
                1001L,
                2001L,
                "求职材料模板包",
                "Northstar Design",
                "Basic",
                "ALL_RELEASES",
                "ACTIVE",
                Instant.parse("2026-10-03T00:00:00Z")
        ));

        List<LibraryItemResponse> result = new LibraryService(mapper).getLibrary(2L);

        assertThat(mapper.requestedUserId).isEqualTo(2L);
        assertThat(result).singleElement().satisfies(item -> {
            assertThat(item.grantId()).isEqualTo(5001L);
            assertThat(item.orderId()).isEqualTo(7001L);
            assertThat(item.productTitle()).isEqualTo("求职材料模板包");
            assertThat(item.shopName()).isEqualTo("Northstar Design");
            assertThat(item.accessPolicy()).isEqualTo("ALL_RELEASES");
            assertThat(item.grantStatus()).isEqualTo("ACTIVE");
        });
    }

    @Test
    void returnsAnEmptyLibraryWhenTheUserHasNoActiveGrant() {
        StubEntitlementMapper mapper = new StubEntitlementMapper();

        assertThat(new LibraryService(mapper).getLibrary(2L)).isEmpty();
    }

    private static final class StubEntitlementMapper implements EntitlementMapper {
        private List<LibraryItemProjection> items = List.of();
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
            return Optional.empty();
        }

        @Override
        public List<LibraryItemProjection> findActiveLibraryByUserId(long userId) {
            requestedUserId = userId;
            return items;
        }
    }
}
