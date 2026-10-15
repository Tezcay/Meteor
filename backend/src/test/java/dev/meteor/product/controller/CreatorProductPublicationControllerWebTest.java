package dev.meteor.product.controller;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import dev.meteor.auth.security.DatabaseUserDetailsService;
import dev.meteor.auth.security.MeteorUserPrincipal;
import dev.meteor.auth.security.SecurityConfig;
import dev.meteor.product.dto.ProductPublicationResponse;
import dev.meteor.product.dto.PublishProductRequest;
import dev.meteor.product.service.ProductDraftNotFoundException;
import dev.meteor.product.service.ProductPublicationConflictException;
import dev.meteor.product.service.ProductPublicationService;
import dev.meteor.web.error.GlobalExceptionHandler;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(CreatorProductPublicationController.class)
@ContextConfiguration(classes = {CreatorProductPublicationController.class, SecurityConfig.class, GlobalExceptionHandler.class})
class CreatorProductPublicationControllerWebTest {
    private static final String PATH = "/api/creator/product-drafts/1002/publish";
    private static final String BODY = "{\"packageName\":\" Basic \",\"priceCents\":1900,\"fileObjectId\":5001}";
    @Autowired private MockMvc mvc;
    @MockitoBean private ProductPublicationService service;
    @MockitoBean private DatabaseUserDetailsService users;

    @Test
    void requiresLoginForReadsAndPublish() throws Exception {
        mvc.perform(get("/api/creator/products")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/creator/delivery-assets")).andExpect(status().isUnauthorized());
        mvc.perform(post(PATH).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(service);
    }

    @Test
    void rejectsBuyerRoleEvenWithCsrf() throws Exception {
        mvc.perform(get("/api/creator/products").with(user(principal(2, "USER"))))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/creator/delivery-assets").with(user(principal(2, "USER"))))
                .andExpect(status().isForbidden());
        mvc.perform(post(PATH).with(user(principal(2, "USER"))).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(BODY)).andExpect(status().isForbidden());
        verifyNoInteractions(service);
    }

    @Test
    void requiresCsrfForCreatorPublication() throws Exception {
        mvc.perform(post(PATH).with(user(principal(1, "CREATOR")))
                .contentType(MediaType.APPLICATION_JSON).content(BODY)).andExpect(status().isForbidden());
        verifyNoInteractions(service);
    }

    @Test
    void usesSessionUserAndNormalizesPackageName() throws Exception {
        when(service.listOwnProducts(7)).thenReturn(List.of());
        when(service.listReusableAssets(7)).thenReturn(List.of());
        when(service.publish(eq(7L), eq(1002L), any())).thenReturn(
                new ProductPublicationResponse(1002, 2, 2003, 3003, 4003, 6007, "Basic", 1900, "CNY", 5001));
        mvc.perform(get("/api/creator/products").with(user(principal(7, "CREATOR"))))
                .andExpect(status().isOk()).andExpect(content().json("[]"));
        mvc.perform(get("/api/creator/delivery-assets").with(user(principal(7, "CREATOR"))))
                .andExpect(status().isOk()).andExpect(content().json("[]"));
        mvc.perform(post(PATH).with(user(principal(7, "CREATOR"))).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isOk()).andExpect(jsonPath("$.productId").value(1002))
                .andExpect(jsonPath("$.packageId").value(2003));
        verify(service).publish(7, 1002, new PublishProductRequest("Basic", 1900, 5001));
    }

    @Test
    void rejectsInvalidPriceAndAssetId() throws Exception {
        mvc.perform(post(PATH).with(user(principal(1, "CREATOR"))).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"packageName\":\"Basic\",\"priceCents\":0,\"fileObjectId\":0}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_ARGUMENT"));
        verifyNoInteractions(service);
    }

    @Test
    void returnsStableNotFoundAndConflictErrors() throws Exception {
        when(service.publish(anyLong(), anyLong(), any()))
                .thenThrow(new ProductDraftNotFoundException())
                .thenThrow(new ProductPublicationConflictException("Already published"));
        mvc.perform(post(PATH).with(user(principal(1, "CREATOR"))).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("PRODUCT_DRAFT_NOT_FOUND"));
        mvc.perform(post(PATH).with(user(principal(1, "CREATOR"))).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("PRODUCT_PUBLICATION_CONFLICT"));
    }

    private static MeteorUserPrincipal principal(long id, String role) {
        return new MeteorUserPrincipal(id, "test@meteor.local", "unused", "Test", "ACTIVE", List.of(role));
    }
}
