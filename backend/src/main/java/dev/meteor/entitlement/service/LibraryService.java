package dev.meteor.entitlement.service;

import dev.meteor.entitlement.dto.LibraryItemResponse;
import dev.meteor.entitlement.mapper.EntitlementMapper;
import dev.meteor.entitlement.mapper.projection.LibraryItemProjection;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LibraryService {

    private final EntitlementMapper entitlementMapper;

    public LibraryService(EntitlementMapper entitlementMapper) {
        this.entitlementMapper = entitlementMapper;
    }

    @Transactional(readOnly = true)
    public List<LibraryItemResponse> getLibrary(long userId) {
        return entitlementMapper.findActiveLibraryByUserId(userId).stream()
                .map(LibraryService::toResponse)
                .toList();
    }

    private static LibraryItemResponse toResponse(LibraryItemProjection item) {
        return new LibraryItemResponse(
                item.grantId(),
                item.orderId(),
                item.orderItemId(),
                item.productId(),
                item.packageId(),
                item.productTitle(),
                item.shopName(),
                item.packageName(),
                item.accessPolicy(),
                item.grantStatus(),
                item.grantedAt()
        );
    }
}
