package dev.meteor.auth.mapper.projection;

public record AccountCredentialsProjection(
        long id,
        String email,
        String passwordHash,
        String displayName,
        String status
) {
}
