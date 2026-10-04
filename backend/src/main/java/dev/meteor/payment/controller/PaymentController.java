package dev.meteor.payment.controller;

import dev.meteor.auth.security.MeteorUserPrincipal;
import dev.meteor.payment.dto.PaymentResponse;
import dev.meteor.payment.dto.TestPaymentEventRequest;
import dev.meteor.payment.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders")
@ConditionalOnProperty(name = "meteor.test-payment.enabled", havingValue = "true")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/{orderId}/test-payment")
    public PaymentResponse applyTestPayment(
            @AuthenticationPrincipal MeteorUserPrincipal principal,
            @PathVariable long orderId,
            @Valid @RequestBody TestPaymentEventRequest request
    ) {
        return paymentService.applyTestPayment(principal.id(), orderId, request);
    }
}
