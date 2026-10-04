package dev.meteor.auth.dto;

import java.util.List;

public record CurrentUserResponse(
        long id,
        String email,
        String displayName,
        List<String> roles
) {
}
