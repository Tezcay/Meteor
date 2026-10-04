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

    @ExceptionHandler(ProductNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiErrorResponse handleProductNotFound(ProductNotFoundException exception) {
        return new ApiErrorResponse("PRODUCT_NOT_FOUND", exception.getMessage(), Instant.now());
    }

    @ExceptionHandler(OrderPackageNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiErrorResponse handleOrderPackageNotFound(OrderPackageNotFoundException exception) {
        return new ApiErrorResponse("ORDER_PACKAGE_NOT_FOUND", exception.getMessage(), Instant.now());
    }

    @ExceptionHandler(OrderNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiErrorResponse handleOrderNotFound(OrderNotFoundException exception) {
        return new ApiErrorResponse("ORDER_NOT_FOUND", exception.getMessage(), Instant.now());
    }

    @ExceptionHandler(EntitlementGrantNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiErrorResponse handleEntitlementGrantNotFound(EntitlementGrantNotFoundException exception) {
        return new ApiErrorResponse("ENTITLEMENT_NOT_FOUND", exception.getMessage(), Instant.now());
    }

    @ExceptionHandler(DeliveryManifestNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiErrorResponse handleDeliveryManifestNotFound(DeliveryManifestNotFoundException exception) {
        return new ApiErrorResponse("DELIVERY_MANIFEST_NOT_FOUND", exception.getMessage(), Instant.now());
    }

    @ExceptionHandler(DownloadNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiErrorResponse handleDownloadNotFound(DownloadNotFoundException exception) {
        return new ApiErrorResponse("DOWNLOAD_NOT_FOUND", exception.getMessage(), Instant.now());
    }

    @ExceptionHandler(DownloadIntegrityException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiErrorResponse handleDownloadIntegrityFailure(DownloadIntegrityException exception) {
        return new ApiErrorResponse("DOWNLOAD_INTEGRITY_FAILURE", exception.getMessage(), Instant.now());
    }

    @ExceptionHandler(DownloadStorageException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiErrorResponse handleDownloadStorageFailure(DownloadStorageException exception) {
        return new ApiErrorResponse("DOWNLOAD_STORAGE_FAILURE", exception.getMessage(), Instant.now());
    }

    @ExceptionHandler(OrderIdempotencyConflictException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiErrorResponse handleOrderIdempotencyConflict(OrderIdempotencyConflictException exception) {
        return new ApiErrorResponse("IDEMPOTENCY_CONFLICT", exception.getMessage(), Instant.now());
    }

    @ExceptionHandler(PaymentOrderNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiErrorResponse handlePaymentOrderNotFound(PaymentOrderNotFoundException exception) {
        return new ApiErrorResponse("ORDER_NOT_FOUND", exception.getMessage(), Instant.now());
    }

    @ExceptionHandler(PaymentConflictException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiErrorResponse handlePaymentConflict(PaymentConflictException exception) {
        return new ApiErrorResponse("PAYMENT_CONFLICT", exception.getMessage(), Instant.now());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiErrorResponse handleIllegalArgument(IllegalArgumentException exception) {
        return new ApiErrorResponse("INVALID_ARGUMENT", exception.getMessage(), Instant.now());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiErrorResponse handleUnreadableRequestBody(HttpMessageNotReadableException exception) {
        return new ApiErrorResponse("INVALID_ARGUMENT", "Request body is malformed", Instant.now());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiErrorResponse handleArgumentTypeMismatch(MethodArgumentTypeMismatchException exception) {
        return new ApiErrorResponse(
                "INVALID_ARGUMENT",
                "Request parameter or path variable has an invalid type",
                Instant.now()
        );
    }

    @ExceptionHandler(AuthenticationException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ApiErrorResponse handleAuthenticationFailure(AuthenticationException exception) {
        return new ApiErrorResponse("AUTHENTICATION_FAILED", "Email or password is incorrect", Instant.now());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiErrorResponse handleValidationFailure(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getField() + " " + error.getDefaultMessage())
                .orElse("Request validation failed");
        return new ApiErrorResponse("INVALID_ARGUMENT", message, Instant.now());
    }
}
