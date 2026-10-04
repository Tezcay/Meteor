package dev.meteor.delivery.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.meteor.auth.security.DatabaseUserDetailsService;
import dev.meteor.auth.security.MeteorUserPrincipal;
import dev.meteor.auth.security.SecurityConfig;
import dev.meteor.delivery.dto.DeliveryManifestResponse;
import dev.meteor.delivery.dto.ManifestItemResponse;
import dev.meteor.delivery.dto.ReleaseResponse;
import dev.meteor.delivery.service.DeliveryManifestNotFoundException;
import dev.meteor.delivery.service.DeliveryManifestService;
import dev.meteor.delivery.service.EntitlementGrantNotFoundException;
import dev.meteor.delivery.service.ReleaseService;
import dev.meteor.entitlement.controller.LibraryController;
import dev.meteor.entitlement.dto.LibraryItemResponse;
import dev.meteor.entitlement.service.LibraryService;
import dev.meteor.web.error.GlobalExceptionHandler;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest({LibraryController.class, ReleaseController.class, DeliveryManifestController.class})
@ContextConfiguration(classes = {
        LibraryController.class,
        ReleaseController.class,
        DeliveryManifestController.class,
        SecurityConfig.class,
        GlobalExceptionHandler.class
})
class LibraryDeliveryControllerWebTest {

    private static final long USER_ID = 2L;
    private static final long GRANT_ID = 5001L;
    private static final long RELEASE_ID = 6001L;
    private static final String LIBRARY_PATH = "/api/library";
    private static final String RELEASES_PATH = "/api/library/{grantId}/releases";
    private static final String MANIFEST_PATH = "/api/library/{grantId}/releases/{releaseId}/manifest";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private LibraryService libraryService;

    @MockitoBean
    private ReleaseService releaseService;

    @MockitoBean
    private DeliveryManifestService manifestService;

    @MockitoBean
    private DatabaseUserDetailsService userDetailsService;

    @Test
    void rejectsAnonymousLibraryRequest() throws Exception {
        mockMvc.perform(get(LIBRARY_PATH))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));

        verifyNoInteractions(libraryService);
    }

    @Test
    void listsOnlyLibraryItemsResolvedForCurrentUser() throws Exception {
        Instant grantedAt = Instant.parse("2026-10-04T05:00:00Z");
        when(libraryService.getLibrary(USER_ID)).thenReturn(List.of(new LibraryItemResponse(
                GRANT_ID,
                7001L,
                7101L,
                101L,
                201L,
                "Spring Boot 实战素材包",
                "Meteor Studio",
                "标准版",
                "ALL_RELEASES",
                "ACTIVE",
                grantedAt
        )));

        mockMvc.perform(get(LIBRARY_PATH).with(user(principal())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].grantId").value(GRANT_ID))
                .andExpect(jsonPath("$[0].packageId").value(201))
                .andExpect(jsonPath("$[0].grantStatus").value("ACTIVE"))
                .andExpect(jsonPath("$[0].grantedAt").value("2026-10-04T05:00:00Z"));

        verify(libraryService).getLibrary(USER_ID);
    }

    @Test
    void rejectsAnonymousReleaseRequest() throws Exception {
        mockMvc.perform(get(RELEASES_PATH, GRANT_ID))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));

        verifyNoInteractions(releaseService);
    }

    @Test
    void listsReleasesForCurrentUsersGrant() throws Exception {
        when(releaseService.getAccessibleReleases(USER_ID, GRANT_ID)).thenReturn(List.of(
                new ReleaseResponse(
                        RELEASE_ID,
                        201L,
                        2,
                        "2.0.0",
                        "增加面试题与示例代码",
                        Instant.parse("2026-10-04T06:00:00Z"),
                        true
                ),
                new ReleaseResponse(
                        6000L,
                        201L,
                        1,
                        "1.0.0",
                        "首个版本",
                        Instant.parse("2026-10-03T06:00:00Z"),
                        false
                )
        ));

        mockMvc.perform(get(RELEASES_PATH, GRANT_ID).with(user(principal())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(RELEASE_ID))
                .andExpect(jsonPath("$[0].versionLabel").value("2.0.0"))
                .andExpect(jsonPath("$[0].latest").value(true))
                .andExpect(jsonPath("$[1].latest").value(false));

        verify(releaseService).getAccessibleReleases(USER_ID, GRANT_ID);
    }

    @Test
    void hidesMissingOrForeignGrantFromReleaseRequest() throws Exception {
        when(releaseService.getAccessibleReleases(USER_ID, GRANT_ID))
                .thenThrow(new EntitlementGrantNotFoundException(GRANT_ID));

        mockMvc.perform(get(RELEASES_PATH, GRANT_ID).with(user(principal())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ENTITLEMENT_NOT_FOUND"));
    }

    @Test
    void rejectsAnonymousManifestRequest() throws Exception {
        mockMvc.perform(get(MANIFEST_PATH, GRANT_ID, RELEASE_ID))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));

        verifyNoInteractions(manifestService);
    }

    @Test
    void returnsOwnedManifestWithoutInternalStorageLocation() throws Exception {
        when(manifestService.getAccessibleManifest(USER_ID, GRANT_ID, RELEASE_ID))
                .thenReturn(new DeliveryManifestResponse(
                        8001L,
                        RELEASE_ID,
                        "2.0.0",
                        Instant.parse("2026-10-04T06:05:00Z"),
                        1,
                        2048L,
                        List.of(new ManifestItemResponse(
                                9001L,
                                1,
                                "MAIN_ARCHIVE",
                                "完整素材包",
                                "spring-boot-kit.zip",
                                "application/zip",
                                2048L,
                                "abc123"
                        ))
                ));

        mockMvc.perform(get(MANIFEST_PATH, GRANT_ID, RELEASE_ID).with(user(principal())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(8001))
                .andExpect(jsonPath("$.releaseId").value(RELEASE_ID))
                .andExpect(jsonPath("$.itemCount").value(1))
                .andExpect(jsonPath("$.totalBytes").value(2048))
                .andExpect(jsonPath("$.items[0].filename").value("spring-boot-kit.zip"))
                .andExpect(jsonPath("$.items[0].sha256").value("abc123"))
                .andExpect(jsonPath("$.items[0].storageKey").doesNotExist())
                .andExpect(jsonPath("$.items[0].storagePath").doesNotExist());

        verify(manifestService).getAccessibleManifest(USER_ID, GRANT_ID, RELEASE_ID);
    }

    @Test
    void hidesMissingOrForeignGrantFromManifestRequest() throws Exception {
        when(manifestService.getAccessibleManifest(USER_ID, GRANT_ID, RELEASE_ID))
                .thenThrow(new EntitlementGrantNotFoundException(GRANT_ID));

        mockMvc.perform(get(MANIFEST_PATH, GRANT_ID, RELEASE_ID).with(user(principal())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ENTITLEMENT_NOT_FOUND"));
    }

    @Test
    void hidesReleaseOutsideGrantedPackageFromManifestRequest() throws Exception {
        when(manifestService.getAccessibleManifest(USER_ID, GRANT_ID, RELEASE_ID))
                .thenThrow(new DeliveryManifestNotFoundException(RELEASE_ID));

        mockMvc.perform(get(MANIFEST_PATH, GRANT_ID, RELEASE_ID).with(user(principal())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("DELIVERY_MANIFEST_NOT_FOUND"));
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
