package dev.meteor.product.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.meteor.auth.security.DatabaseUserDetailsService;
import dev.meteor.auth.security.SecurityConfig;
import dev.meteor.product.dto.ProductDetailResponse;
import dev.meteor.product.dto.ProductListItemResponse;
import dev.meteor.product.service.ProductNotFoundException;
import dev.meteor.product.service.ProductQueryService;
import dev.meteor.web.error.GlobalExceptionHandler;
import dev.meteor.web.request.RequestCorrelationFilter;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ProductController.class)
@ContextConfiguration(classes = {
        ProductController.class,
        SecurityConfig.class,
        GlobalExceptionHandler.class
})
class ProductControllerWebTest {

    private static final long PRODUCT_ID = 1001L;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProductQueryService productQueryService;

    @MockitoBean
    private DatabaseUserDetailsService userDetailsService;

    @Test
    void listsPublishedProductsWithoutAuthenticationAndForwardsFilters() throws Exception {
        when(productQueryService.listPublishedProducts(" 100%_! ", "templates"))
                .thenReturn(List.of(new ProductListItemResponse(
                        PRODUCT_ID,
                        "Northstar Design",
                        "100% 求职模板",
                        "包含简历与作品集模板"
                )));

        mockMvc.perform(get("/api/products")
                        .queryParam("q", " 100%_! ")
                        .queryParam("category", "templates"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(PRODUCT_ID))
                .andExpect(jsonPath("$[0].shopName").value("Northstar Design"))
                .andExpect(jsonPath("$[0].title").value("100% 求职模板"));

        verify(productQueryService).listPublishedProducts(" 100%_! ", "templates");
    }

    @Test
    void returnsEmptyArrayForValidSearchWithoutMatches() throws Exception {
        when(productQueryService.listPublishedProducts("missing", null)).thenReturn(List.of());

        mockMvc.perform(get("/api/products").queryParam("q", "missing"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());

        verify(productQueryService).listPublishedProducts("missing", null);
    }

    @Test
    void returnsPublishedProductDetailWithSalePackagesWithoutAuthentication() throws Exception {
        when(productQueryService.getPublishedProduct(PRODUCT_ID)).thenReturn(new ProductDetailResponse(
                PRODUCT_ID,
                "Northstar Design",
                "求职材料模板包",
                "模板说明",
                List.of(
                        new ProductDetailResponse.PackageOptionResponse(2001L, "Basic", 1900L, "CNY"),
                        new ProductDetailResponse.PackageOptionResponse(2002L, "Pro", 3900L, "CNY")
                )
        ));

        mockMvc.perform(get("/api/products/{productId}", PRODUCT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(PRODUCT_ID))
                .andExpect(jsonPath("$.title").value("求职材料模板包"))
                .andExpect(jsonPath("$.packages[0].id").value(2001))
                .andExpect(jsonPath("$.packages[0].priceCents").value(1900))
                .andExpect(jsonPath("$.packages[1].name").value("Pro"))
                .andExpect(jsonPath("$.packages[1].currency").value("CNY"));

        verify(productQueryService).getPublishedProduct(PRODUCT_ID);
    }

    @Test
    void mapsMissingOrUnpublishedProductToNotFound() throws Exception {
        when(productQueryService.getPublishedProduct(PRODUCT_ID))
                .thenThrow(new ProductNotFoundException(PRODUCT_ID));

        mockMvc.perform(get("/api/products/{productId}", PRODUCT_ID)
                        .header(RequestCorrelationFilter.HEADER_NAME, "product-not-found-001"))
                .andExpect(status().isNotFound())
                .andExpect(header().string(RequestCorrelationFilter.HEADER_NAME, "product-not-found-001"))
                .andExpect(jsonPath("$.code").value("PRODUCT_NOT_FOUND"))
                .andExpect(jsonPath("$.requestId").value("product-not-found-001"));
    }

    @Test
    void mapsInvalidPublicSearchToBadRequest() throws Exception {
        when(productQueryService.listPublishedProducts("x".repeat(101), null))
                .thenThrow(new IllegalArgumentException("query must not exceed 100 characters"));

        mockMvc.perform(get("/api/products").queryParam("q", "x".repeat(101)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_ARGUMENT"))
                .andExpect(jsonPath("$.message").value("query must not exceed 100 characters"));
    }

    @Test
    void mapsInvalidPathVariableTypeToStableBadRequest() throws Exception {
        mockMvc.perform(get("/api/products/not-a-number"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_ARGUMENT"))
                .andExpect(jsonPath("$.message")
                        .value("Request parameter or path variable has an invalid type"))
                .andExpect(jsonPath("$.timestamp").isString());
    }
}
