package dev.meteor.auth.dto;

public record CsrfTokenResponse(
        String headerName,
        String parameterName,
        String token
) {
}
