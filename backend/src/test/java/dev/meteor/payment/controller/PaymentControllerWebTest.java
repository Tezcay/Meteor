package dev.meteor.payment.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.meteor.auth.security.DatabaseUserDetailsService;
import dev.meteor.auth.security.MeteorUserPrincipal;
import dev.meteor.auth.security.SecurityConfig;
import dev.meteor.order.model.OrderStatus;
import dev.meteor.payment.dto.PaymentResponse;
import dev.meteor.payment.dto.TestPaymentEventRequest;
import dev.meteor.payment.model.PaymentEventProcessingStatus;
import dev.meteor.payment.service.PaymentConflictException;
import dev.meteor.payment.service.PaymentOrderNotFoundException;
import dev.meteor.payment.service.PaymentService;
import dev.meteor.web.error.GlobalExceptionHandler;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(PaymentController.class)
@ContextConfiguration(classes = {
        PaymentController.class,
        SecurityConfig.class,
        GlobalExceptionHandler.class
})
@TestPropertySource(properties = "meteor.test-payment.enabled=true")
class PaymentControllerWebTest {

    private static final long USER_ID = 2L;
    private static final long ORDER_ID = 7001L;
    private static final String PAYMENT_PATH = "/api/orders/{orderId}/test-payment";
    private static final String VALID_REQUEST = """
            {
              "providerEventId": " evt-001 ",
              "providerTransactionId": " txn-001 ",
              "amountCents": 1900,
              "currency": " cny "
            }
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PaymentService paymentService;

    @MockitoBean
    private DatabaseUserDetailsService userDetailsService;

    @Test
    void rejectsAnonymousPaymentRequest() throws Exception {
        mockMvc.perform(post(PAYMENT_PATH, ORDER_ID)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_REQUEST))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));

        verifyNoInteractions(paymentService);
    }

    @Test
    void rejectsPaymentRequestWithoutCsrfToken() throws Exception {
        mockMvc.perform(post(PAYMENT_PATH, ORDER_ID)
                        .with(user(principal()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_REQUEST))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));

        verifyNoInteractions(paymentService);
    }

    @Test
    void rejectsInvalidPaymentRequestBeforeCallingService() throws Exception {
        mockMvc.perform(post(PAYMENT_PATH, ORDER_ID)
                        .with(user(principal()))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "providerEventId": " ",
                                  "providerTransactionId": "txn-001",
                                  "amountCents": 0,
                                  "currency": "CN"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_ARGUMENT"));

        verifyNoInteractions(paymentService);
    }

    @Test
    void appliesPaymentForCurrentUserAndReturnsTypedResponse() throws Exception {
        TestPaymentEventRequest normalizedRequest = new TestPaymentEventRequest(
                "evt-001",
                "txn-001",
                1900L,
                "CNY"
        );
        when(paymentService.applyTestPayment(USER_ID, ORDER_ID, normalizedRequest))
                .thenReturn(new PaymentResponse(
                        4001L,
                        PaymentService.PROVIDER,
                        "evt-001",
                        "txn-001",
                        PaymentEventProcessingStatus.APPLIED,
                        ORDER_ID,
                        OrderStatus.PAID,
                        1900L,
                        "CNY",
                        5001L,
                        "ACTIVE"
                ));

        mockMvc.perform(post(PAYMENT_PATH, ORDER_ID)
                        .with(user(principal()))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_REQUEST))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eventId").value(4001))
                .andExpect(jsonPath("$.provider").value("LOCAL_TEST"))
                .andExpect(jsonPath("$.processingStatus").value("APPLIED"))
                .andExpect(jsonPath("$.orderId").value(ORDER_ID))
                .andExpect(jsonPath("$.orderStatus").value("PAID"))
                .andExpect(jsonPath("$.entitlementGrantId").value(5001))
                .andExpect(jsonPath("$.entitlementStatus").value("ACTIVE"));

        verify(paymentService).applyTestPayment(USER_ID, ORDER_ID, normalizedRequest);
    }

    @Test
    void mapsPaymentConflictToConflictResponse() throws Exception {
        TestPaymentEventRequest request = new TestPaymentEventRequest(
                "evt-001",
                "txn-001",
                1900L,
                "CNY"
        );
        when(paymentService.applyTestPayment(USER_ID, ORDER_ID, request))
                .thenThrow(new PaymentConflictException("Payment amount does not match the order"));

        mockMvc.perform(post(PAYMENT_PATH, ORDER_ID)
                        .with(user(principal()))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_REQUEST))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("PAYMENT_CONFLICT"))
                .andExpect(jsonPath("$.message").value("Payment amount does not match the order"));
    }

    @Test
    void hidesMissingOrForeignOrderBehindNotFoundResponse() throws Exception {
        TestPaymentEventRequest request = new TestPaymentEventRequest(
                "evt-001",
                "txn-001",
                1900L,
                "CNY"
        );
        when(paymentService.applyTestPayment(USER_ID, ORDER_ID, request))
                .thenThrow(new PaymentOrderNotFoundException(ORDER_ID));

        mockMvc.perform(post(PAYMENT_PATH, ORDER_ID)
                        .with(user(principal()))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_REQUEST))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ORDER_NOT_FOUND"));
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
