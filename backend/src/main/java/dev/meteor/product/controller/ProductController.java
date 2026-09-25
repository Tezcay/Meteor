package dev.meteor.product.controller;

import dev.meteor.product.dto.ProductDetailResponse;
import dev.meteor.product.dto.ProductListItemResponse;
import dev.meteor.product.service.ProductQueryService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductQueryService productQueryService;

    public ProductController(ProductQueryService productQueryService) {
        this.productQueryService = productQueryService;
    }

    @GetMapping
    public List<ProductListItemResponse> listPublishedProducts(
            @RequestParam(name = "q", required = false) String query,
            @RequestParam(name = "category", required = false) String category
    ) {
        return productQueryService.listPublishedProducts(query, category);
    }

    @GetMapping("/{productId}")
    public ProductDetailResponse getPublishedProduct(@PathVariable long productId) {
        return productQueryService.getPublishedProduct(productId);
    }
}
