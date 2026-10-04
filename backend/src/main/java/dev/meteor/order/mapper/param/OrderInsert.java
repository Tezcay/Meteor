package dev.meteor.order.mapper.param;

import dev.meteor.order.model.OrderStatus;

public final class OrderInsert {

    private long id;
    private final long userId;
    private final OrderStatus status;
    private final long totalAmountCents;
    private final String currency;
    private final String idempotencyKey;

    public OrderInsert(
            long userId,
            OrderStatus status,
            long totalAmountCents,
            String currency,
            String idempotencyKey
    ) {
        this.userId = userId;
        this.status = status;
        this.totalAmountCents = totalAmountCents;
        this.currency = currency;
        this.idempotencyKey = idempotencyKey;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getUserId() {
        return userId;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public long getTotalAmountCents() {
        return totalAmountCents;
    }

    public String getCurrency() {
        return currency;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }
}
