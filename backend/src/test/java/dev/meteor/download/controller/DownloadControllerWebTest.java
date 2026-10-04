package dev.meteor.download.controller;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.meteor.auth.security.DatabaseUserDetailsService;
import dev.meteor.auth.security.MeteorUserPrincipal;
import dev.meteor.auth.security.SecurityConfig;
import dev.meteor.download.dto.DownloadAuthorizationResponse;
import dev.meteor.download.dto.DownloadFilePayload;
import dev.meteor.download.service.DownloadService;
import dev.meteor.web.error.GlobalExceptionHandler;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest({DownloadAuthorizationController.class, DownloadController.class})
@ContextConfiguration(classes = {
        DownloadAuthorizationController.class,
        DownloadController.class,
        SecurityConfig.class,
        GlobalExceptionHandler.class
})
class DownloadControllerWebTest {

    private static final long USER_ID = 2L;
    private static final String TOKEN = "one-time-token";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DownloadService downloadService;

    @MockitoBean
    private DatabaseUserDetailsService userDetailsService;

    @Test
    void rejectsAnonymousAuthorizationRequest() throws Exception {
        mockMvc.perform(post("/api/library/7/releases/8/manifest/items/9/download-authorizations")
                        .with(csrf()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void rejectsAuthorizationRequestWithoutCsrfToken() throws Exception {
        mockMvc.perform(post("/api/library/7/releases/8/manifest/items/9/download-authorizations")
                        .with(user(principal())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void authorizesOwnedManifestItemForAuthenticatedUser() throws Exception {
        Instant expiresAt = Instant.parse("2026-10-03T08:05:00Z");
        when(downloadService.authorize(USER_ID, 7L, 8L, 9L))
                .thenReturn(new DownloadAuthorizationResponse(
                        21L,
                        "/api/downloads/" + TOKEN,
                        expiresAt,
                        "starter-kit.zip"
                ));

        mockMvc.perform(post("/api/library/7/releases/8/manifest/items/9/download-authorizations")
                        .with(user(principal()))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ticketId").value(21))
                .andExpect(jsonPath("$.downloadUrl").value("/api/downloads/" + TOKEN))
                .andExpect(jsonPath("$.expiresAt").value("2026-10-03T08:05:00Z"))
                .andExpect(jsonPath("$.filename").value("starter-kit.zip"));

        verify(downloadService).authorize(USER_ID, 7L, 8L, 9L);
    }

    @Test
    void returnsPreparedFileWithDownloadHeaders() throws Exception {
        byte[] body = "verified-download".getBytes(StandardCharsets.UTF_8);
        when(downloadService.consume(USER_ID, TOKEN)).thenReturn(new DownloadFilePayload(
                "starter-kit.zip",
                "application/zip",
                "abc123",
                body.length,
                new ByteArrayResource(body)
        ));

        mockMvc.perform(get("/api/downloads/{token}", TOKEN)
                        .with(user(principal())))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/zip"))
                .andExpect(content().bytes(body))
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store"))
                .andExpect(header().string("X-Content-SHA256", "abc123"))
                .andExpect(header().string(HttpHeaders.CONTENT_LENGTH, String.valueOf(body.length)))
                .andExpect(header().string(
                        HttpHeaders.CONTENT_DISPOSITION,
                        containsString("attachment; filename=\"starter-kit.zip\"")
                ));

        verify(downloadService).consume(USER_ID, TOKEN);
    }

    @Test
    void rejectsAnonymousDownloadRequest() throws Exception {
        mockMvc.perform(get("/api/downloads/{token}", TOKEN))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
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
