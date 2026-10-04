package dev.meteor.order.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.meteor.auth.security.DatabaseUserDetailsService;
import dev.meteor.auth.security.MeteorUserPrincipal;
import dev.meteor.auth.security.SecurityConfig;
import dev.meteor.order.dto.CreateOrderRequest;
import dev.meteor.order.dto.OrderDetailResponse;
import dev.meteor.order.dto.OrderResponse;
import dev.meteor.order.model.OrderStatus;
import dev.meteor.order.service.OrderIdempotencyConflictException;
import dev.meteor.order.service.OrderNotFoundException;
import dev.meteor.order.service.OrderPackageNotFoundException;
import dev.meteor.order.service.OrderService;
import dev.meteor.web.error.GlobalExceptionHandler;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(OrderController.class)
@ContextConfiguration(classes = {
        OrderController.class,
        SecurityConfig.class,
        GlobalExceptionHandler.class
})
class OrderControllerWebTest {

    private static final long USER_ID = 2L;
    private static final long PRODUCT_ID = 101L;
    private static final long PACKAGE_ID = 201L;
    private static final long ORDER_ID = 7001L;
    private static final String ORDERS_PATH = "/api/orders";
    private static final String VALID_REQUEST = """
            {
              "productId": 101,
              "packageId": 201,
              "idempotencyKey": " checkout-001 "
            }
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OrderService orderService;

    @MockitoBean
    private DatabaseUserDetailsService userDetailsService;

    @Test
    void rejectsAnonymousOrderCreation() throws Exception {
        mockMvc.perform(post(ORDERS_PATH)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_REQUEST))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));

        verifyNoInteractions(orderService);
    }

    @Test
    void rejectsOrderCreationWithoutCsrfToken() throws Exception {
        mockMvc.perform(post(ORDERS_PATH)
                        .with(user(principal()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_REQUEST))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));

        verifyNoInteractions(orderService);
    }

    @Test
    void rejectsInvalidOrderBeforeCallingService() throws Exception {
        mockMvc.perform(post(ORDERS_PATH)
                        .with(user(principal()))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "productId": 0,
                                  "packageId": -1,
                                  "idempotencyKey": " "
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_ARGUMENT"));

        verifyNoInteractions(orderService);
    }

    @Test
    void createsOrderForCurrentUserAndReturnsSnapshot() throws Exception {
        CreateOrderRequest normalizedRequest = new CreateOrderRequest(PRODUCT_ID, PACKAGE_ID, "checkout-001");
        when(orderService.createOrder(USER_ID, normalizedRequest)).thenReturn(new OrderResponse(
                ORDER_ID,
                OrderStatus.PENDING_PAYMENT,
                1900L,
                "CNY",
                "checkout-001",
                PRODUCT_ID,
                PACKAGE_ID,
                "Spring Boot 实战素材包",
                "Meteor Studio",
                "标准版",
                "ALL_RELEASES"
        ));

        mockMvc.perform(post(ORDERS_PATH)
                        .with(user(principal()))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_REQUEST))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(ORDER_ID))
                .andExpect(jsonPath("$.status").value("PENDING_PAYMENT"))
                .andExpect(jsonPath("$.totalAmountCents").value(1900))
                .andExpect(jsonPath("$.currency").value("CNY"))
                .andExpect(jsonPath("$.idempotencyKey").value("checkout-001"))
                .andExpect(jsonPath("$.productTitle").value("Spring Boot 实战素材包"))
                .andExpect(jsonPath("$.accessPolicy").value("ALL_RELEASES"));

        verify(orderService).createOrder(USER_ID, normalizedRequest);
    }

    @Test
    void mapsIdempotencyReuseForDifferentPackageToConflict() throws Exception {
        CreateOrderRequest request = new CreateOrderRequest(PRODUCT_ID, PACKAGE_ID, "checkout-001");
        when(orderService.createOrder(USER_ID, request))
                .thenThrow(new OrderIdempotencyConflictException("checkout-001"));

        mockMvc.perform(post(ORDERS_PATH)
                        .with(user(principal()))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_REQUEST))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("IDEMPOTENCY_CONFLICT"));
    }

    @Test
    void mapsUnavailablePackageToNotFound() throws Exception {
        CreateOrderRequest request = new CreateOrderRequest(PRODUCT_ID, PACKAGE_ID, "checkout-001");
        when(orderService.createOrder(USER_ID, request))
                .thenThrow(new OrderPackageNotFoundException(PRODUCT_ID, PACKAGE_ID));

        mockMvc.perform(post(ORDERS_PATH)
                        .with(user(principal()))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_REQUEST))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ORDER_PACKAGE_NOT_FOUND"));
    }

    @Test
    void listsOnlyOrdersResolvedForCurrentUser() throws Exception {
        Instant createdAt = Instant.parse("2026-10-04T05:00:00Z");
        when(orderService.getOrders(USER_ID)).thenReturn(List.of(new OrderDetailResponse(
                ORDER_ID,
                OrderStatus.PAID,
                1900L,
                "CNY",
                createdAt,
                PRODUCT_ID,
                PACKAGE_ID,
                "Spring Boot 实战素材包",
                "Meteor Studio",
                "标准版",
                "ALL_RELEASES",
                5001L,
                "ACTIVE",
                createdAt.plusSeconds(30)
        )));

        mockMvc.perform(get(ORDERS_PATH).with(user(principal())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(ORDER_ID))
                .andExpect(jsonPath("$[0].status").value("PAID"))
                .andExpect(jsonPath("$[0].entitlementGrantId").value(5001))
                .andExpect(jsonPath("$[0].entitlementStatus").value("ACTIVE"));

        verify(orderService).getOrders(USER_ID);
    }

    @Test
    void hidesMissingOrForeignOrderBehindNotFoundResponse() throws Exception {
        when(orderService.getOrder(USER_ID, ORDER_ID)).thenThrow(new OrderNotFoundException(ORDER_ID));

        mockMvc.perform(get(ORDERS_PATH + "/{orderId}", ORDER_ID).with(user(principal())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ORDER_NOT_FOUND"));

        verify(orderService).getOrder(USER_ID, ORDER_ID);
    }

    private MeteorUserPrincipal principal() {
        return new MeteorUserPrincipal(
                USER_ID,
                "buyer@meteor.local",
                "unused-test-password-hash",
                "Meteor Buyer",
                "ACTIVE",
                List.of("USER")
        );
    }
}
