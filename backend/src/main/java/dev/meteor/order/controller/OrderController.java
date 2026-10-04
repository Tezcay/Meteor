package dev.meteor.order.controller;

import dev.meteor.auth.security.MeteorUserPrincipal;
import dev.meteor.order.dto.CreateOrderRequest;
import dev.meteor.order.dto.OrderDetailResponse;
import dev.meteor.order.dto.OrderResponse;
import dev.meteor.order.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse createOrder(
            @AuthenticationPrincipal MeteorUserPrincipal principal,
            @Valid @RequestBody CreateOrderRequest request
    ) {
        return orderService.createOrder(principal.id(), request);
    }

    @GetMapping
    public List<OrderDetailResponse> getOrders(
            @AuthenticationPrincipal MeteorUserPrincipal principal
    ) {
        return orderService.getOrders(principal.id());
    }

    @GetMapping("/{orderId}")
    public OrderDetailResponse getOrder(
            @AuthenticationPrincipal MeteorUserPrincipal principal,
            @PathVariable long orderId
    ) {
        return orderService.getOrder(principal.id(), orderId);
    }
}
