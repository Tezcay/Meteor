package dev.meteor.order.mapper;

import dev.meteor.order.mapper.param.OrderInsert;
import dev.meteor.order.mapper.param.OrderItemInsert;
import dev.meteor.order.mapper.projection.OrderDetailProjection;
import dev.meteor.order.mapper.projection.OrderSnapshotProjection;
import java.util.List;
import java.util.Optional;
import org.apache.ibatis.annotations.Param;

public interface OrderMapper {

    Optional<OrderSnapshotProjection> findByUserIdAndIdempotencyKey(
            @Param("userId") long userId,
            @Param("idempotencyKey") String idempotencyKey
    );

    Optional<OrderSnapshotProjection> findByUserIdAndIdempotencyKeyForUpdate(
            @Param("userId") long userId,
            @Param("idempotencyKey") String idempotencyKey
    );

    Optional<OrderSnapshotProjection> findByIdAndUserId(
            @Param("orderId") long orderId,
            @Param("userId") long userId
    );

    Optional<OrderSnapshotProjection> findByIdAndUserIdForUpdate(
            @Param("orderId") long orderId,
            @Param("userId") long userId
    );

    List<OrderDetailProjection> findAllByUserId(@Param("userId") long userId);

    Optional<OrderDetailProjection> findDetailByIdAndUserId(
            @Param("orderId") long orderId,
            @Param("userId") long userId
    );

    int insertOrder(OrderInsert order);

    int insertOrderItem(OrderItemInsert item);

    int markPaid(@Param("orderId") long orderId, @Param("userId") long userId);
}
