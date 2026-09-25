package dev.meteor.product.controller;

import dev.meteor.product.dto.ApiErrorResponse;
import dev.meteor.product.service.ProductNotFoundException;
import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ProductNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiErrorResponse handleProductNotFound(ProductNotFoundException exception) {
        return new ApiErrorResponse("PRODUCT_NOT_FOUND", exception.getMessage(), Instant.now());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiErrorResponse handleIllegalArgument(IllegalArgumentException exception) {
        return new ApiErrorResponse("INVALID_ARGUMENT", exception.getMessage(), Instant.now());
    }
}
