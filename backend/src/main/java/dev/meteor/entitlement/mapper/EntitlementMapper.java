package dev.meteor.entitlement.mapper;

import dev.meteor.entitlement.mapper.projection.EntitlementGrantProjection;
import dev.meteor.entitlement.mapper.projection.LibraryItemProjection;
import java.util.List;
import java.util.Optional;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface EntitlementMapper {

    // 订单项唯一约束防止重复 Grant；重复时只原样保留 id，不重置 REVOKED 状态。
    @Insert("""
            INSERT INTO entitlement_grants (
                user_id,
                order_item_id,
                product_id,
                package_id,
                access_policy,
                status
            )
            SELECT o.user_id,
                   oi.id,
                   oi.product_id,
                   oi.package_id,
                   oi.access_policy,
                   'ACTIVE'
            FROM orders o
            JOIN order_items oi ON oi.order_id = o.id
            WHERE o.id = #{orderId}
              AND o.user_id = #{userId}
              AND o.status = 'PAID'
            ON DUPLICATE KEY UPDATE id = entitlement_grants.id
            """)
    int ensureGrantForPaidOrder(
            @Param("orderId") long orderId,
            @Param("userId") long userId
    );

    @Select("""
            SELECT eg.id,
                   eg.user_id,
                   eg.order_item_id,
                   eg.product_id,
                   eg.package_id,
                   eg.access_policy,
                   eg.status,
                   eg.granted_at,
                   eg.revoked_at
            FROM entitlement_grants eg
            JOIN order_items oi ON oi.id = eg.order_item_id
            WHERE oi.order_id = #{orderId}
              AND eg.user_id = #{userId}
            """)
    Optional<EntitlementGrantProjection> findByOrderIdAndUserId(
            @Param("orderId") long orderId,
            @Param("userId") long userId
    );

    @Select("""
            SELECT id,
                   user_id,
                   order_item_id,
                   product_id,
                   package_id,
                   access_policy,
                   status,
                   granted_at,
                   revoked_at
            FROM entitlement_grants
            WHERE id = #{grantId}
              AND user_id = #{userId}
              AND status = 'ACTIVE'
            """)
    Optional<EntitlementGrantProjection> findActiveByIdAndUserId(
            @Param("grantId") long grantId,
            @Param("userId") long userId
    );

    @Select("""
            SELECT eg.id AS grant_id,
                   o.id AS order_id,
                   oi.id AS order_item_id,
                   eg.product_id,
                   eg.package_id,
                   oi.product_title,
                   oi.shop_name,
                   oi.package_name,
                   eg.access_policy,
                   eg.status AS grant_status,
                   eg.granted_at
            FROM entitlement_grants eg
            JOIN order_items oi ON oi.id = eg.order_item_id
            JOIN orders o ON o.id = oi.order_id
            WHERE eg.user_id = #{userId}
              AND eg.status = 'ACTIVE'
            ORDER BY eg.granted_at DESC, eg.id DESC
            """)
    List<LibraryItemProjection> findActiveLibraryByUserId(@Param("userId") long userId);
}
