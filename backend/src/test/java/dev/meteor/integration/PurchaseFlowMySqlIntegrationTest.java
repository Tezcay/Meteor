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
import dev.meteor.product.dto.CreateProductDraftRequest;
import dev.meteor.product.dto.ProductDraftResponse;
import dev.meteor.product.service.ProductDraftService;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
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
            // Test-only: the application user needs to create failure-injection triggers.
            // Production does not enable this MySQL option or create triggers at runtime.
            .withCommand("--log-bin-trust-function-creators=1")
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

    @Autowired
    private ProductDraftService productDraftService;

    // 隔离的临时 MySQL 中验证真实自增主键、记录归属及第二张表失败后的事务回滚。
    private void assertCreatorDraftStorageAndRollback(long creatorId, long buyerId) {
        ProductDraftResponse draft = productDraftService.createDraft(
                creatorId, new CreateProductDraftRequest(" 容器测试草稿 ", " 尚未发布 "));
        assertThat(draft.productId()).isPositive();
        assertThat(draft.revisionId()).isPositive();
        assertThat(draft.title()).isEqualTo("容器测试草稿");
        assertThat(draft.revisionStatus()).isEqualTo("DRAFT");
        assertThat(draft.saleStatus()).isEqualTo("OFF_SALE");
        assertThat(productDraftService.listOwnDrafts(creatorId)).contains(draft);
        assertThat(productDraftService.listOwnDrafts(buyerId)).isEmpty();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT current_published_revision_id FROM products WHERE id = ?", Long.class, draft.productId()))
                .isNull();

        int before = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM products", Integer.class);
        jdbcTemplate.execute("""
                CREATE TRIGGER it_fail_draft_revision BEFORE INSERT ON product_revisions
                FOR EACH ROW SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'draft rollback test'
                """);
        try {
            assertThatThrownBy(() -> productDraftService.createDraft(
                    creatorId, new CreateProductDraftRequest("失败草稿", "用于验证回滚")))
                    .isInstanceOf(DataAccessException.class);
            assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM products", Integer.class))
                    .isEqualTo(before);
        } finally {
            jdbcTemplate.execute("DROP TRIGGER IF EXISTS it_fail_draft_revision");
        }
    }

    @Test
    void runsAllMigrationsAndCompletesIdempotentPurchaseAndDownload() throws Exception {
        long buyerId = userId("buyer@meteor.local");
        long creatorId = userId("creator@meteor.local");

        assertThat(appliedMigrationCount()).isEqualTo(13);

        assertCreatorDraftStorageAndRollback(creatorId, buyerId);

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

        assertConcurrentOrderCreationConverges(buyerId);

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


    @Autowired
    private dev.meteor.product.service.ProductPublicationService publicationService;

    @Autowired
    private dev.meteor.product.service.ProductQueryService productQueryService;

    @Test
    void publishesOwnDraftAndCompletesPurchaseOfTheNewProduct() throws Exception {
        long creatorId = userId("creator@meteor.local");
        long buyerId = userId("buyer@meteor.local");
        ProductDraftResponse draft = productDraftService.createDraft(
                creatorId, new CreateProductDraftRequest("真实新商品", "通过发布接口生成的交付"));
        assertThatThrownBy(() -> productQueryService.getPublishedProduct(draft.productId()))
                .isInstanceOf(dev.meteor.product.service.ProductNotFoundException.class);
        assertThatThrownBy(() -> publicationService.publish(buyerId, draft.productId(),
                new dev.meteor.product.dto.PublishProductRequest("Basic", 2300, 5001)))
                .isInstanceOf(dev.meteor.product.service.ProductDraftNotFoundException.class);
        assertThat(publicationService.listReusableAssets(buyerId)).isEmpty();

        // 两个调用同时发布同一草稿，最终必须共享一个套餐、版本和清单，而非重复交付对象。
        var request = new dev.meteor.product.dto.PublishProductRequest("Basic", 2300, 5001);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        dev.meteor.product.dto.ProductPublicationResponse published;
        try {
            java.util.concurrent.Callable<dev.meteor.product.dto.ProductPublicationResponse> task = () -> {
                ready.countDown();
                if (!start.await(5, TimeUnit.SECONDS)) { throw new IllegalStateException("Publication start timed out"); }
                return publicationService.publish(creatorId, draft.productId(), request);
            };
            var first = executor.submit(task);
            var second = executor.submit(task);
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            published = first.get(15, TimeUnit.SECONDS);
            assertThat(second.get(15, TimeUnit.SECONDS)).isEqualTo(published);
        } finally {
            start.countDown();
            executor.shutdownNow();
            executor.awaitTermination(5, TimeUnit.SECONDS);
        }
        assertThat(count("packages", "product_id", draft.productId())).isEqualTo(1);
        assertThat(count("releases", "package_id", published.packageId())).isEqualTo(1);
        assertThat(count("delivery_manifests", "release_id", published.releaseId())).isEqualTo(1);
        assertThat(count("manifest_items", "manifest_id", published.manifestId())).isEqualTo(1);
        assertThat(productQueryService.getPublishedProduct(draft.productId()).title()).isEqualTo("真实新商品");
        assertThatThrownBy(() -> publicationService.publish(creatorId, draft.productId(),
                new dev.meteor.product.dto.PublishProductRequest("Basic", 2400, 5001)))
                .isInstanceOf(dev.meteor.product.service.ProductPublicationConflictException.class);

        OrderResponse order = orderService.createOrder(buyerId,
                new CreateOrderRequest(draft.productId(), published.packageId(), "it-published-product"));
        assertThat(order.totalAmountCents()).isEqualTo(2300);
        PaymentResponse payment = paymentService.applyTestPayment(buyerId, order.id(),
                new TestPaymentEventRequest("it-published-event", "it-published-transaction", 2300, "CNY"));
        DownloadAuthorizationResponse authorization = downloadService.authorize(
                buyerId, payment.entitlementGrantId(), published.releaseId(), published.manifestItemId());
        assertThat(downloadService.consume(buyerId, tokenFrom(authorization)).sizeBytes()).isEqualTo(634);
        assertThatThrownBy(() -> downloadService.consume(buyerId, tokenFrom(authorization)))
                .isInstanceOf(DownloadNotFoundException.class);
    }

    @Test
    void rollsBackPublicationWhenManifestItemInsertionFails() {
        long creatorId = userId("creator@meteor.local");
        ProductDraftResponse draft = productDraftService.createDraft(
                creatorId, new CreateProductDraftRequest("发布回滚草稿", "交付失败时不能上架"));
        var request = new dev.meteor.product.dto.PublishProductRequest("Basic", 1700, 5001);
        jdbcTemplate.execute("""
                CREATE TRIGGER it_reject_publication_item BEFORE INSERT ON manifest_items
                FOR EACH ROW SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'publication rollback test'
                """);
        try {
            assertThatThrownBy(() -> publicationService.publish(creatorId, draft.productId(), request))
                    .isInstanceOf(DataAccessException.class);
            assertThat(count("packages", "product_id", draft.productId())).isZero();
            assertThat(jdbcTemplate.queryForObject(
                    "SELECT status FROM product_revisions WHERE id = ?", String.class, draft.revisionId()))
                    .isEqualTo("DRAFT");
            assertThat(jdbcTemplate.queryForObject(
                    "SELECT sale_status FROM products WHERE id = ?", String.class, draft.productId()))
                    .isEqualTo("OFF_SALE");
            assertThat(jdbcTemplate.queryForObject(
                    "SELECT current_published_revision_id FROM products WHERE id = ?", Long.class, draft.productId()))
                    .isNull();
            assertThatThrownBy(() -> productQueryService.getPublishedProduct(draft.productId()))
                    .isInstanceOf(dev.meteor.product.service.ProductNotFoundException.class);
        } finally {
            jdbcTemplate.execute("DROP TRIGGER IF EXISTS it_reject_publication_item");
        }
        assertThat(publicationService.publish(creatorId, draft.productId(), request).productId())
                .isEqualTo(draft.productId());
    }

    @Test
    void purchasesAllImportedShowcasePacksAndVerifiesTheirRealImages() throws Exception {
        long buyerId = userId("buyer@meteor.local");
        long creatorId = userId("creator@meteor.local");
        String[][] imageNames = {
                {"courtyard-1280.webp", "exhibition-1942.webp", "creator-studio-1280.webp"},
                {"winter-forest.webp", "tahoe-overlook.webp", "winter-cabin.webp"},
                {"still-places.webp"}, {"sunset-field.webp", "wild-places.webp"}
        };
        for (int index = 0; index < imageNames.length; index++) {
            long id = 9101L + index;
            String category = index == 0 ? "photography" : "illustration";
            var product = productQueryService.getPublishedProduct(id);
            assertThat(productQueryService.listPublishedProducts("", category))
                    .anyMatch(item -> item.id() == id);
            assertThat(publicationService.listReusableAssets(creatorId)).anyMatch(item -> item.id() == id);
            assertThat(publicationService.listReusableAssets(buyerId)).isEmpty();
            long amount = product.packages().getFirst().priceCents();
            var order = orderService.createOrder(buyerId,
                    new CreateOrderRequest(id, id, "it-showcase-" + id));
            assertThat(order.totalAmountCents()).isEqualTo(amount);
            var payment = paymentService.applyTestPayment(buyerId, order.id(),
                    new TestPaymentEventRequest("it-showcase-event-" + id, "it-showcase-tx-" + id, amount, "CNY"));
            assertThatThrownBy(() -> downloadService.authorize(creatorId, payment.entitlementGrantId(), id, id))
                    .isInstanceOf(DownloadNotFoundException.class);
            var authorization = downloadService.authorize(buyerId, payment.entitlementGrantId(), id, id);
            var payload = downloadService.consume(buyerId, tokenFrom(authorization));
            assertThat(payload.mediaType()).isEqualTo("application/zip");
            // Verify receipt bytes, not just a PREPARED audit or a download button.
            byte[] receipt;
            try (var input = payload.resource().getInputStream()) { receipt = input.readAllBytes(); }
            assertThat(receipt.length).isEqualTo(payload.sizeBytes());
            assertThat(java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(receipt)))
                    .isEqualTo(payload.sha256());
            java.util.Map<String, byte[]> entries = new java.util.HashMap<>();
            try (var zip = new java.util.zip.ZipInputStream(new java.io.ByteArrayInputStream(receipt))) {
                java.util.zip.ZipEntry entry;
                while ((entry = zip.getNextEntry()) != null) { entries.put(entry.getName(), zip.readAllBytes()); }
            }
            assertThat(entries.keySet()).containsExactlyInAnyOrderElementsOf(
                    java.util.stream.Stream.concat(java.util.stream.Stream.of("README.txt"), java.util.Arrays.stream(imageNames[index])).toList());
            for (String image : imageNames[index]) {
                assertThat(new String(entries.get(image), 0, 4, java.nio.charset.StandardCharsets.US_ASCII)).isEqualTo("RIFF");
                assertThat(new String(entries.get(image), 8, 4, java.nio.charset.StandardCharsets.US_ASCII)).isEqualTo("WEBP");
            }
            assertThatThrownBy(() -> downloadService.consume(buyerId, tokenFrom(authorization)))
                    .isInstanceOf(DownloadNotFoundException.class);
        }
    }

    private void assertConcurrentOrderCreationConverges(long buyerId) throws Exception {
        String idempotencyKey = "it-order-concurrent";
        CreateOrderRequest request = new CreateOrderRequest(1001L, 2001L, idempotencyKey);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        List<OrderResponse> responses = List.of();

        jdbcTemplate.execute("""
                CREATE TRIGGER delay_integration_test_order
                BEFORE INSERT ON orders
                FOR EACH ROW
                SET @integration_test_delay = SLEEP(1)
                """);
        try {
            Future<OrderResponse> first = executor.submit(
                    () -> createOrderAfterStartSignal(buyerId, request, ready, start)
            );
            Future<OrderResponse> second = executor.submit(
                    () -> createOrderAfterStartSignal(buyerId, request, ready, start)
            );

            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            responses = List.of(
                    first.get(15, TimeUnit.SECONDS),
                    second.get(15, TimeUnit.SECONDS)
            );
        } finally {
            start.countDown();
            executor.shutdownNow();
            try {
                executor.awaitTermination(5, TimeUnit.SECONDS);
            } finally {
                jdbcTemplate.execute("DROP TRIGGER IF EXISTS delay_integration_test_order");
            }
        }

        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).id()).isEqualTo(responses.get(1).id());
        assertThat(count("orders", "idempotency_key", idempotencyKey)).isEqualTo(1);
        assertThat(count("order_items", "order_id", responses.get(0).id())).isEqualTo(1);
    }

    private OrderResponse createOrderAfterStartSignal(
            long buyerId,
            CreateOrderRequest request,
            CountDownLatch ready,
            CountDownLatch start
    ) throws Exception {
        ready.countDown();
        if (!start.await(5, TimeUnit.SECONDS)) {
            throw new IllegalStateException("Timed out waiting to start concurrent order creation");
        }
        return orderService.createOrder(buyerId, request);
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
