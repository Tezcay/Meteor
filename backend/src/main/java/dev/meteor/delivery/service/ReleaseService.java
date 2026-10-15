package dev.meteor.delivery.service;

import dev.meteor.delivery.dto.ReleaseResponse;
import dev.meteor.delivery.mapper.ReleaseMapper;
import dev.meteor.delivery.mapper.projection.ReleaseProjection;
import dev.meteor.entitlement.mapper.EntitlementMapper;
import dev.meteor.entitlement.mapper.projection.EntitlementGrantProjection;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 先验证当前用户的有效权益，再用权益中的可信 packageId 限定正式版本。 */
@Service
public class ReleaseService {

    private final EntitlementMapper entitlementMapper;
    private final ReleaseMapper releaseMapper;

    public ReleaseService(EntitlementMapper entitlementMapper, ReleaseMapper releaseMapper) {
        this.entitlementMapper = entitlementMapper;
        this.releaseMapper = releaseMapper;
    }

    @Transactional(readOnly = true)
    public List<ReleaseResponse> getAccessibleReleases(long userId, long grantId) {
        EntitlementGrantProjection grant = entitlementMapper
                .findActiveByIdAndUserId(grantId, userId)
                .orElseThrow(() -> new EntitlementGrantNotFoundException(grantId));
        // 不接收客户端 packageId，防止拿 Basic 权益读取另一个套餐的版本。
        List<ReleaseProjection> releases = releaseMapper.findPublishedByPackageId(grant.packageId());
        List<ReleaseResponse> result = new ArrayList<>(releases.size());
        for (int index = 0; index < releases.size(); index++) {
            ReleaseProjection release = releases.get(index);
            result.add(new ReleaseResponse(
                    release.id(),
                    release.packageId(),
                    release.releaseNumber(),
                    release.versionLabel(),
                    release.summary(),
                    release.publishedAt(),
                    index == 0
            ));
        }
        return List.copyOf(result);
    }
}
