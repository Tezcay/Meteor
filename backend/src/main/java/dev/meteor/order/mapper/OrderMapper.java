package dev.meteor.order.mapper;

import dev.meteor.order.mapper.param.OrderInsert;
import dev.meteor.order.mapper.param.OrderItemInsert;
import dev.meteor.order.mapper.projection.OrderDetailProjection;
import dev.meteor.order.mapper.projection.OrderSnapshotProjection;
import java.util.List;
import java.util.Optional;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

public interface OrderMapper {

    @Select("""
            SELECT o.id,
                   o.user_id,
                   o.status,
                   o.total_amount_cents,
                   o.currency,
                   o.idempotency_key,
                   oi.product_id,
                   oi.package_id,
                   oi.product_title,
                   oi.shop_name,
                   oi.package_name,
                   oi.access_policy
            FROM orders o
            JOIN order_items oi ON oi.order_id = o.id
            WHERE o.user_id = #{userId}
              AND o.idempotency_key = #{idempotencyKey}
            """)
    Optional<OrderSnapshotProjection> findByUserIdAndIdempotencyKey(
            @Param("userId") long userId,
            @Param("idempotencyKey") String idempotencyKey
    );

    @Select("""
            SELECT o.id,
                   o.user_id,
                   o.status,
                   o.total_amount_cents,
                   o.currency,
                   o.idempotency_key,
                   oi.product_id,
                   oi.package_id,
                   oi.product_title,
                   oi.shop_name,
                   oi.package_name,
                   oi.access_policy
            FROM orders o
            JOIN order_items oi ON oi.order_id = o.id
            WHERE o.user_id = #{userId}
              AND o.idempotency_key = #{idempotencyKey}
            FOR UPDATE
            """)
    Optional<OrderSnapshotProjection> findByUserIdAndIdempotencyKeyForUpdate(
            @Param("userId") long userId,
            @Param("idempotencyKey") String idempotencyKey
    );

    @Select("""
            SELECT o.id,
                   o.user_id,
                   o.status,
                   o.total_amount_cents,
                   o.currency,
                   o.idempotency_key,
                   oi.product_id,
                   oi.package_id,
                   oi.product_title,
                   oi.shop_name,
                   oi.package_name,
                   oi.access_policy
            FROM orders o
            JOIN order_items oi ON oi.order_id = o.id
            WHERE o.id = #{orderId}
              AND o.user_id = #{userId}
            """)
    Optional<OrderSnapshotProjection> findByIdAndUserId(
            @Param("orderId") long orderId,
            @Param("userId") long userId
    );

    @Select("""
            SELECT o.id,
                   o.user_id,
                   o.status,
                   o.total_amount_cents,
                   o.currency,
                   o.idempotency_key,
                   oi.product_id,
                   oi.package_id,
                   oi.product_title,
                   oi.shop_name,
                   oi.package_name,
                   oi.access_policy
            FROM orders o
            JOIN order_items oi ON oi.order_id = o.id
            WHERE o.id = #{orderId}
              AND o.user_id = #{userId}
            FOR UPDATE
            """)
    Optional<OrderSnapshotProjection> findByIdAndUserIdForUpdate(
            @Param("orderId") long orderId,
            @Param("userId") long userId
    );

    @Select("""
            SELECT o.id,
                   o.status,
                   o.total_amount_cents,
                   o.currency,
                   o.created_at,
                   oi.product_id,
                   oi.package_id,
                   oi.product_title,
                   oi.shop_name,
                   oi.package_name,
                   oi.access_policy,
                   eg.id AS entitlement_grant_id,
                   eg.status AS entitlement_status,
                   eg.granted_at
            FROM orders o
            JOIN order_items oi ON oi.order_id = o.id
            LEFT JOIN entitlement_grants eg ON eg.order_item_id = oi.id
            WHERE o.user_id = #{userId}
            ORDER BY o.created_at DESC, o.id DESC
            """)
    List<OrderDetailProjection> findAllByUserId(@Param("userId") long userId);

    @Select("""
            SELECT o.id,
                   o.status,
                   o.total_amount_cents,
                   o.currency,
                   o.created_at,
                   oi.product_id,
                   oi.package_id,
                   oi.product_title,
                   oi.shop_name,
                   oi.package_name,
                   oi.access_policy,
                   eg.id AS entitlement_grant_id,
                   eg.status AS entitlement_status,
                   eg.granted_at
            FROM orders o
            JOIN order_items oi ON oi.order_id = o.id
            LEFT JOIN entitlement_grants eg ON eg.order_item_id = oi.id
            WHERE o.id = #{orderId}
              AND o.user_id = #{userId}
            """)
    Optional<OrderDetailProjection> findDetailByIdAndUserId(
            @Param("orderId") long orderId,
            @Param("userId") long userId
    );

    @Insert("""
            INSERT INTO orders (user_id, status, total_amount_cents, currency, idempotency_key)
            VALUES (#{userId}, #{status}, #{totalAmountCents}, #{currency}, #{idempotencyKey})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int insertOrder(OrderInsert order);

    @Insert("""
            INSERT INTO order_items (
                order_id,
                product_id,
                package_id,
                product_title,
                shop_name,
                package_name,
                access_policy,
                unit_price_cents,
                currency,
                quantity
            ) VALUES (
                #{orderId},
                #{productId},
                #{packageId},
                #{productTitle},
                #{shopName},
                #{packageName},
                #{accessPolicy},
                #{unitPriceCents},
                #{currency},
                #{quantity}
            )
            """)
    int insertOrderItem(OrderItemInsert item);

    @Update("""
            UPDATE orders
            SET status = 'PAID'
            WHERE id = #{orderId}
              AND user_id = #{userId}
              AND status = 'PENDING_PAYMENT'
            """)
    int markPaid(@Param("orderId") long orderId, @Param("userId") long userId);
}
