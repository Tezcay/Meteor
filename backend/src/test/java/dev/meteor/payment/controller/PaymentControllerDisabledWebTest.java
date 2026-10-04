package dev.meteor.payment.controller;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.meteor.auth.security.DatabaseUserDetailsService;
import dev.meteor.auth.security.MeteorUserPrincipal;
import dev.meteor.auth.security.SecurityConfig;
import dev.meteor.payment.service.PaymentService;
import dev.meteor.web.error.GlobalExceptionHandler;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(PaymentController.class)
@ContextConfiguration(classes = {
        PaymentController.class,
        SecurityConfig.class,
        GlobalExceptionHandler.class
})
class PaymentControllerDisabledWebTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PaymentService paymentService;

    @MockitoBean
    private DatabaseUserDetailsService userDetailsService;

    @Test
    void doesNotExposeTestPaymentEndpointWhenFeatureIsDisabled() throws Exception {
        mockMvc.perform(post("/api/orders/7001/test-payment")
                        .with(user(principal()))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "providerEventId": "evt-001",
                                  "providerTransactionId": "txn-001",
                                  "amountCents": 1900,
                                  "currency": "CNY"
                                }
                                """))
                .andExpect(status().isNotFound());

        verifyNoInteractions(paymentService);
    }

    private MeteorUserPrincipal principal() {
        return new MeteorUserPrincipal(
                2L,
                "buyer@meteor.local",
                "unused-test-password-hash",
                "Meteor Buyer",
                "ACTIVE",
                List.of("USER")
        );
    }
}
