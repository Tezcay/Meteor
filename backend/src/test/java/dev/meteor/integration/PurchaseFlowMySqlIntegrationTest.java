package dev.meteor.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.meteor.download.dto.DownloadAuthorizationResponse;
import dev.meteor.download.dto.DownloadFilePayload;
import dev.meteor.download.service.DownloadNotFoundException;
import dev.meteor.download.service.DownloadService;
import dev.meteor.order.dto.CreateOrderRequest;
import dev.meteor.order.dto.OrderResponse;
import dev.meteor.order.model.OrderStatus;
import dev.meteor.order.service.OrderNotFoundException;
import dev.meteor.order.service.OrderService;
import dev.meteor.payment.dto.PaymentResponse;
import dev.meteor.payment.dto.TestPaymentEventRequest;
import dev.meteor.payment.service.PaymentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

@SpringBootTest(properties = {
        "spring.flyway.baseline-on-migrate=false",
        "meteor.demo-accounts.enabled=false",
        "meteor.test-payment.enabled=false"
})
@Testcontainers(disabledWithoutDocker = true)
class PurchaseFlowMySqlIntegrationTest {

    @Container
    @ServiceConnection
    private static final MySQLContainer MYSQL = new MySQLContainer("mysql:8.0.36")
            .withDatabaseName("meteor_test")
            .withUsername("meteor_test")
            .withPassword("meteor_test");

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private OrderService orderService;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private DownloadService downloadService;

    @Test
    void runsAllMigrationsAndCompletesIdempotentPurchaseAndDownload() {
        long buyerId = userId("buyer@meteor.local");
        long creatorId = userId("creator@meteor.local");

        assertThat(appliedMigrationCount()).isEqualTo(12);

        OrderResponse order = orderService.createOrder(
                buyerId,
                new CreateOrderRequest(1001L, 2001L, "it-purchase-success")
        );
        OrderResponse replayedOrder = orderService.createOrder(
                buyerId,
                new CreateOrderRequest(1001L, 2001L, "it-purchase-success")
        );

        assertThat(order.status()).isEqualTo(OrderStatus.PENDING_PAYMENT);
        assertThat(order.totalAmountCents()).isEqualTo(1900L);
        assertThat(replayedOrder.id()).isEqualTo(order.id());
        assertThat(count("orders", "idempotency_key", "it-purchase-success")).isEqualTo(1);

        TestPaymentEventRequest paymentEvent = new TestPaymentEventRequest(
                "it-event-success",
                "it-transaction-success",
                1900L,
                "CNY"
        );
        PaymentResponse payment = paymentService.applyTestPayment(buyerId, order.id(), paymentEvent);
        PaymentResponse replayedPayment = paymentService.applyTestPayment(buyerId, order.id(), paymentEvent);

        assertThat(payment.orderStatus()).isEqualTo(OrderStatus.PAID);
        assertThat(payment.entitlementStatus()).isEqualTo("ACTIVE");
        assertThat(replayedPayment.eventId()).isEqualTo(payment.eventId());
        assertThat(replayedPayment.entitlementGrantId()).isEqualTo(payment.entitlementGrantId());
        assertThat(count("payment_attempts", "order_id", order.id())).isEqualTo(1);
        assertThat(count("payment_events", "payment_attempt_id", paymentAttemptId(order.id()))).isEqualTo(1);
        assertThat(payment.entitlementGrantId()).isNotNull();
        assertThat(count("entitlement_grants", "id", payment.entitlementGrantId())).isEqualTo(1);

        assertThat(orderService.getOrder(buyerId, order.id()).status()).isEqualTo(OrderStatus.PAID);
        assertThatThrownBy(() -> orderService.getOrder(creatorId, order.id()))
                .isInstanceOf(OrderNotFoundException.class);

        long grantId = payment.entitlementGrantId();
        DownloadAuthorizationResponse authorization = downloadService.authorize(
                buyerId,
                grantId,
                3001L,
                6001L
        );
        String token = tokenFrom(authorization);
        DownloadFilePayload file = downloadService.consume(buyerId, token);

        assertThat(file.filename()).isEqualTo("basic-resume-template.html");
        assertThat(file.sizeBytes()).isEqualTo(634L);
        assertThat(file.resource().isReadable()).isTrue();
        assertThat(count("download_tickets", "id", authorization.ticketId())).isEqualTo(1);
        assertThat(count("download_audits", "download_ticket_id", authorization.ticketId())).isEqualTo(1);
        assertThat(ticketStatus(authorization.ticketId())).isEqualTo("CONSUMED");
        assertThatThrownBy(() -> downloadService.consume(buyerId, token))
                .isInstanceOf(DownloadNotFoundException.class);
    }

    @Test
    void rollsBackBusinessTransactionsWhenDownstreamInsertFails() {
        long buyerId = userId("buyer@meteor.local");
        OrderResponse order = orderService.createOrder(
                buyerId,
                new CreateOrderRequest(1001L, 2002L, "it-purchase-rollback")
        );

        jdbcTemplate.execute("""
                CREATE TRIGGER reject_integration_test_grant
                BEFORE INSERT ON entitlement_grants
                FOR EACH ROW
                SIGNAL SQLSTATE '45000'
                SET MESSAGE_TEXT = 'forced entitlement failure for transaction test'
                """);
        try {
            assertThatThrownBy(() -> paymentService.applyTestPayment(
                    buyerId,
                    order.id(),
                    new TestPaymentEventRequest(
                            "it-event-rollback",
                            "it-transaction-rollback",
                            3900L,
                            "CNY"
                    )
            )).isInstanceOf(DataAccessException.class);
        } finally {
            jdbcTemplate.execute("DROP TRIGGER IF EXISTS reject_integration_test_grant");
        }

        assertThat(orderService.getOrder(buyerId, order.id()).status())
                .isEqualTo(OrderStatus.PENDING_PAYMENT);
        assertThat(count("payment_attempts", "order_id", order.id())).isZero();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM payment_events WHERE provider_event_id = ?",
                Integer.class,
                "it-event-rollback"
        )).isZero();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM entitlement_grants eg "
                        + "JOIN order_items oi ON oi.id = eg.order_item_id WHERE oi.order_id = ?",
                Integer.class,
                order.id()
        )).isZero();

        OrderResponse downloadOrder = orderService.createOrder(
                buyerId,
                new CreateOrderRequest(1001L, 2001L, "it-download-rollback")
        );
        PaymentResponse downloadPayment = paymentService.applyTestPayment(
                buyerId,
                downloadOrder.id(),
                new TestPaymentEventRequest(
                        "it-event-download-rollback",
                        "it-transaction-download-rollback",
                        1900L,
                        "CNY"
                )
        );
        assertThat(downloadPayment.entitlementGrantId()).isNotNull();
        DownloadAuthorizationResponse authorization = downloadService.authorize(
                buyerId,
                downloadPayment.entitlementGrantId(),
                3001L,
                6001L
        );
        String token = tokenFrom(authorization);

        jdbcTemplate.execute("""
                CREATE TRIGGER reject_integration_test_download_audit
                BEFORE INSERT ON download_audits
                FOR EACH ROW
                SIGNAL SQLSTATE '45000'
                SET MESSAGE_TEXT = 'forced download audit failure for transaction test'
                """);
        try {
            assertThatThrownBy(() -> downloadService.consume(buyerId, token))
                    .isInstanceOf(DataAccessException.class);
        } finally {
            jdbcTemplate.execute("DROP TRIGGER IF EXISTS reject_integration_test_download_audit");
        }

        assertThat(ticketStatus(authorization.ticketId())).isEqualTo("ISSUED");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM download_tickets "
                        + "WHERE id = ? AND consumed_at IS NULL",
                Integer.class,
                authorization.ticketId()
        )).isEqualTo(1);
        assertThat(count("download_audits", "download_ticket_id", authorization.ticketId())).isZero();

        DownloadFilePayload retriedFile = downloadService.consume(buyerId, token);
        assertThat(retriedFile.sizeBytes()).isEqualTo(634L);
        assertThat(ticketStatus(authorization.ticketId())).isEqualTo("CONSUMED");
        assertThat(count("download_audits", "download_ticket_id", authorization.ticketId())).isEqualTo(1);
    }

    private long userId(String email) {
        Long id = jdbcTemplate.queryForObject(
                "SELECT id FROM users WHERE email = ?",
                Long.class,
                email
        );
        assertThat(id).isNotNull();
        return id;
    }

    private int appliedMigrationCount() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM flyway_schema_history WHERE success = 1",
                Integer.class
        );
        return count == null ? 0 : count;
    }

    private long paymentAttemptId(long orderId) {
        Long id = jdbcTemplate.queryForObject(
                "SELECT id FROM payment_attempts WHERE order_id = ?",
                Long.class,
                orderId
        );
        assertThat(id).isNotNull();
        return id;
    }

    private String ticketStatus(long ticketId) {
        String status = jdbcTemplate.queryForObject(
                "SELECT status FROM download_tickets WHERE id = ?",
                String.class,
                ticketId
        );
        assertThat(status).isNotNull();
        return status;
    }

    private static String tokenFrom(DownloadAuthorizationResponse authorization) {
        String prefix = "/api/downloads/";
        assertThat(authorization.downloadUrl()).startsWith(prefix);
        return authorization.downloadUrl().substring(prefix.length());
    }

    private int count(String table, String column, Object value) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM " + table + " WHERE " + column + " = ?",
                Integer.class,
                value
        );
        return count == null ? 0 : count;
    }
}
