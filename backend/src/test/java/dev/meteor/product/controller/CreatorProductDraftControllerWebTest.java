package dev.meteor.product.controller;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import dev.meteor.auth.security.DatabaseUserDetailsService;
import dev.meteor.auth.security.MeteorUserPrincipal;
import dev.meteor.auth.security.SecurityConfig;
import dev.meteor.product.dto.CreateProductDraftRequest;
import dev.meteor.product.dto.ProductDraftResponse;
import dev.meteor.product.service.CreatorShopUnavailableException;
import dev.meteor.product.service.ProductDraftService;
import dev.meteor.web.error.GlobalExceptionHandler;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(CreatorProductDraftController.class)
@ContextConfiguration(classes = {CreatorProductDraftController.class, SecurityConfig.class, GlobalExceptionHandler.class})
class CreatorProductDraftControllerWebTest {
    private static final String PATH = "/api/creator/product-drafts";
    private static final String BODY = "{\"title\":\" 新模板 \",\"summary\":\" 简介 \"}";

    @Autowired private MockMvc mvc;
    @MockitoBean private ProductDraftService service;
    @MockitoBean private DatabaseUserDetailsService users;

    @Test
    void requiresLoginForBothEndpoints() throws Exception {
        mvc.perform(get(PATH)).andExpect(status().isUnauthorized());
        mvc.perform(post(PATH).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(service);
    }

    @Test
    void rejectsBuyerOnBothEndpointsEvenWithCsrf() throws Exception {
        mvc.perform(get(PATH).with(user(principal(2L, "USER"))))
                .andExpect(status().isForbidden());
        mvc.perform(post(PATH).with(user(principal(2L, "USER"))).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isForbidden());
        verifyNoInteractions(service);
    }

    @Test
    void requiresCsrfForCreatorWrites() throws Exception {
        mvc.perform(post(PATH).with(user(principal(1L, "CREATOR")))
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isForbidden());
        verifyNoInteractions(service);
    }

    @Test
    void rejectsEmptyAndOverlongTitles() throws Exception {
        for (String title : List.of(" ", "x".repeat(161))) {
            mvc.perform(post(PATH).with(user(principal(1L, "CREATOR"))).with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"title\":\"" + title + "\",\"summary\":\"简介\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("INVALID_ARGUMENT"));
        }
        verifyNoInteractions(service);
    }

    @Test
    void createsDraftForSessionUserWithNormalizedText() throws Exception {
        when(service.createDraft(1L, new CreateProductDraftRequest("新模板", "简介"))).thenReturn(draft());
        mvc.perform(post(PATH).with(user(principal(1L, "CREATOR"))).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.productId").value(1002))
                .andExpect(jsonPath("$.revisionStatus").value("DRAFT"))
                .andExpect(jsonPath("$.saleStatus").value("OFF_SALE"));
        verify(service).createDraft(1L, new CreateProductDraftRequest("新模板", "简介"));
    }

    @Test
    void returnsOwnDraftsAndEmptyArray() throws Exception {
        when(service.listOwnDrafts(1L)).thenReturn(List.of(draft()));
        when(service.listOwnDrafts(9L)).thenReturn(List.of());
        mvc.perform(get(PATH).with(user(principal(1L, "CREATOR"))))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].title").value("新模板"));
        mvc.perform(get(PATH).with(user(principal(9L, "CREATOR"))))
                .andExpect(status().isOk()).andExpect(content().json("[]"));
    }

    @Test
    void reportsMissingActiveShopAsBusinessConflict() throws Exception {
        when(service.createDraft(1L, new CreateProductDraftRequest("新模板", "简介")))
                .thenThrow(new CreatorShopUnavailableException());
        mvc.perform(post(PATH).with(user(principal(1L, "CREATOR"))).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CREATOR_SHOP_UNAVAILABLE"));
    }

    private MeteorUserPrincipal principal(long id, String role) {
        return new MeteorUserPrincipal(id, "test@meteor.local", "hash", "测试用户", "ACTIVE", List.of(role));
    }

    private ProductDraftResponse draft() {
        return new ProductDraftResponse(1002L, 2L, "新模板", "简介", "DRAFT", "OFF_SALE", Instant.EPOCH);
    }
}
