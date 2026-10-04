package dev.meteor.payment.mapper;

import dev.meteor.payment.mapper.param.PaymentAttemptInsert;
import dev.meteor.payment.mapper.param.PaymentEventInsert;
import dev.meteor.payment.mapper.projection.PaymentAttemptProjection;
import dev.meteor.payment.mapper.projection.PaymentEventProjection;
import java.util.Optional;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

public interface PaymentMapper {

    @Select("""
            SELECT id,
                   order_id,
                   provider,
                   provider_transaction_id,
                   status,
                   amount_cents,
                   currency
            FROM payment_attempts
            WHERE order_id = #{orderId}
            """)
    Optional<PaymentAttemptProjection> findAttemptByOrderId(@Param("orderId") long orderId);

    @Select("""
            SELECT id,
                   order_id,
                   provider,
                   provider_transaction_id,
                   status,
                   amount_cents,
                   currency
            FROM payment_attempts
            WHERE provider = #{provider}
              AND provider_transaction_id = #{providerTransactionId}
            """)
    Optional<PaymentAttemptProjection> findAttemptByProviderTransactionId(
            @Param("provider") String provider,
            @Param("providerTransactionId") String providerTransactionId
    );

    @Select("""
            SELECT id,
                   payment_attempt_id,
                   provider,
                   provider_event_id,
                   provider_transaction_id,
                   event_type,
                   processing_status,
                   amount_cents,
                   currency
            FROM payment_events
            WHERE provider = #{provider}
              AND provider_event_id = #{providerEventId}
            """)
    Optional<PaymentEventProjection> findEventByProviderEventId(
            @Param("provider") String provider,
            @Param("providerEventId") String providerEventId
    );

    @Select("""
            SELECT id,
                   payment_attempt_id,
                   provider,
                   provider_event_id,
                   provider_transaction_id,
                   event_type,
                   processing_status,
                   amount_cents,
                   currency
            FROM payment_events
            WHERE provider = #{provider}
              AND provider_transaction_id = #{providerTransactionId}
            """)
    Optional<PaymentEventProjection> findEventByProviderTransactionId(
            @Param("provider") String provider,
            @Param("providerTransactionId") String providerTransactionId
    );

    @Insert("""
            INSERT INTO payment_attempts (
                order_id,
                provider,
                provider_transaction_id,
                status,
                amount_cents,
                currency
            ) VALUES (
                #{orderId},
                #{provider},
                #{providerTransactionId},
                #{status},
                #{amountCents},
                #{currency}
            )
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int insertAttempt(PaymentAttemptInsert attempt);

    @Insert("""
            INSERT INTO payment_events (
                payment_attempt_id,
                provider,
                provider_event_id,
                provider_transaction_id,
                event_type,
                amount_cents,
                currency,
                processing_status,
                processed_at
            ) VALUES (
                #{paymentAttemptId},
                #{provider},
                #{providerEventId},
                #{providerTransactionId},
                #{eventType},
                #{amountCents},
                #{currency},
                #{processingStatus},
                CURRENT_TIMESTAMP(6)
            )
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int insertEvent(PaymentEventInsert event);

    @Update("""
            UPDATE payment_attempts
            SET status = 'SUCCEEDED'
            WHERE id = #{attemptId}
              AND status = 'PENDING'
            """)
    int markAttemptSucceeded(@Param("attemptId") long attemptId);
}
