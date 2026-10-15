package dev.meteor.web.error;

import dev.meteor.delivery.service.DeliveryManifestNotFoundException;
import dev.meteor.delivery.service.EntitlementGrantNotFoundException;
import dev.meteor.download.service.DownloadIntegrityException;
import dev.meteor.download.service.DownloadNotFoundException;
import dev.meteor.download.service.DownloadStorageException;
import dev.meteor.order.service.OrderIdempotencyConflictException;
import dev.meteor.order.service.OrderNotFoundException;
import dev.meteor.order.service.OrderPackageNotFoundException;
import dev.meteor.payment.service.PaymentConflictException;
import dev.meteor.payment.service.PaymentOrderNotFoundException;
import dev.meteor.product.service.ProductNotFoundException;
import dev.meteor.product.service.CreatorShopUnavailableException;
import dev.meteor.product.service.ProductDraftNotFoundException;
import dev.meteor.product.service.ProductPublicationConflictException;
import dev.meteor.web.request.RequestCorrelationFilter;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ProductDraftNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiErrorResponse handleProductDraftNotFound(
            ProductDraftNotFoundException exception, HttpServletRequest request) {
        return error(request, "PRODUCT_DRAFT_NOT_FOUND", exception.getMessage());
    }

    @ExceptionHandler(ProductPublicationConflictException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiErrorResponse handleProductPublicationConflict(
            ProductPublicationConflictException exception, HttpServletRequest request) {
        return error(request, "PRODUCT_PUBLICATION_CONFLICT", exception.getMessage());
    }

    @ExceptionHandler(CreatorShopUnavailableException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiErrorResponse handleCreatorShopUnavailable(
            CreatorShopUnavailableException exception,
            HttpServletRequest request
    ) {
        return error(request, "CREATOR_SHOP_UNAVAILABLE", exception.getMessage());
    }

    @ExceptionHandler(ProductNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiErrorResponse handleProductNotFound(
            ProductNotFoundException exception,
            HttpServletRequest request
    ) {
        return error(request, "PRODUCT_NOT_FOUND", exception.getMessage());
    }

    @ExceptionHandler(OrderPackageNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiErrorResponse handleOrderPackageNotFound(
            OrderPackageNotFoundException exception,
            HttpServletRequest request
    ) {
        return error(request, "ORDER_PACKAGE_NOT_FOUND", exception.getMessage());
    }

    @ExceptionHandler(OrderNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiErrorResponse handleOrderNotFound(
            OrderNotFoundException exception,
            HttpServletRequest request
    ) {
        return error(request, "ORDER_NOT_FOUND", exception.getMessage());
    }

    @ExceptionHandler(EntitlementGrantNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiErrorResponse handleEntitlementGrantNotFound(
            EntitlementGrantNotFoundException exception,
            HttpServletRequest request
    ) {
        return error(request, "ENTITLEMENT_NOT_FOUND", exception.getMessage());
    }

    @ExceptionHandler(DeliveryManifestNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiErrorResponse handleDeliveryManifestNotFound(
            DeliveryManifestNotFoundException exception,
            HttpServletRequest request
    ) {
        return error(request, "DELIVERY_MANIFEST_NOT_FOUND", exception.getMessage());
    }

    @ExceptionHandler(DownloadNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiErrorResponse handleDownloadNotFound(
            DownloadNotFoundException exception,
            HttpServletRequest request
    ) {
        return error(request, "DOWNLOAD_NOT_FOUND", exception.getMessage());
    }

    @ExceptionHandler(DownloadIntegrityException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiErrorResponse handleDownloadIntegrityFailure(
            DownloadIntegrityException exception,
            HttpServletRequest request
    ) {
        return error(request, "DOWNLOAD_INTEGRITY_FAILURE", exception.getMessage());
    }

    @ExceptionHandler(DownloadStorageException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiErrorResponse handleDownloadStorageFailure(
            DownloadStorageException exception,
            HttpServletRequest request
    ) {
        return error(request, "DOWNLOAD_STORAGE_FAILURE", exception.getMessage());
    }

    @ExceptionHandler(OrderIdempotencyConflictException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiErrorResponse handleOrderIdempotencyConflict(
            OrderIdempotencyConflictException exception,
            HttpServletRequest request
    ) {
        return error(request, "IDEMPOTENCY_CONFLICT", exception.getMessage());
    }

    @ExceptionHandler(PaymentOrderNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiErrorResponse handlePaymentOrderNotFound(
            PaymentOrderNotFoundException exception,
            HttpServletRequest request
    ) {
        return error(request, "ORDER_NOT_FOUND", exception.getMessage());
    }

    @ExceptionHandler(PaymentConflictException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiErrorResponse handlePaymentConflict(
            PaymentConflictException exception,
            HttpServletRequest request
    ) {
        return error(request, "PAYMENT_CONFLICT", exception.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiErrorResponse handleIllegalArgument(
            IllegalArgumentException exception,
            HttpServletRequest request
    ) {
        return error(request, "INVALID_ARGUMENT", exception.getMessage());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiErrorResponse handleUnreadableRequestBody(
            HttpMessageNotReadableException exception,
            HttpServletRequest request
    ) {
        return error(request, "INVALID_ARGUMENT", "Request body is malformed");
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiErrorResponse handleArgumentTypeMismatch(
            MethodArgumentTypeMismatchException exception,
            HttpServletRequest request
    ) {
        return error(request, "INVALID_ARGUMENT", "Request parameter or path variable has an invalid type");
    }

    @ExceptionHandler(AuthenticationException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ApiErrorResponse handleAuthenticationFailure(
            AuthenticationException exception,
            HttpServletRequest request
    ) {
        return error(request, "AUTHENTICATION_FAILED", "Email or password is incorrect");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiErrorResponse handleValidationFailure(
            MethodArgumentNotValidException exception,
            HttpServletRequest request
    ) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(fieldError -> fieldError.getField() + " " + fieldError.getDefaultMessage())
                .orElse("Request validation failed");
        return error(request, "INVALID_ARGUMENT", message);
    }

    private static ApiErrorResponse error(HttpServletRequest request, String code, String message) {
        return new ApiErrorResponse(code, message, RequestCorrelationFilter.requestId(request), Instant.now());
    }
}
