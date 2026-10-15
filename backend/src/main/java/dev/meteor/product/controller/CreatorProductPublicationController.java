package dev.meteor.product.controller;

import dev.meteor.auth.security.MeteorUserPrincipal;
import dev.meteor.product.dto.ProductDraftResponse;
import dev.meteor.product.dto.ProductPublicationResponse;
import dev.meteor.product.dto.PublishProductRequest;
import dev.meteor.product.dto.ReusableDeliveryAssetResponse;
import dev.meteor.product.service.ProductPublicationService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 角色／CSRF 在 SecurityConfig 校验，商品和素材归属在 Service／SQL 中重新检查。 */
@RestController
@RequestMapping("/api/creator")
public class CreatorProductPublicationController {
    private final ProductPublicationService service;
    public CreatorProductPublicationController(ProductPublicationService service) { this.service = service; }

    @GetMapping("/products")
    public List<ProductDraftResponse> list(@AuthenticationPrincipal MeteorUserPrincipal principal) {
        return service.listOwnProducts(principal.id());
    }

    @GetMapping("/delivery-assets")
    public List<ReusableDeliveryAssetResponse> assets(@AuthenticationPrincipal MeteorUserPrincipal principal) {
        return service.listReusableAssets(principal.id());
    }

    @PostMapping("/product-drafts/{productId}/publish")
    public ProductPublicationResponse publish(
            @AuthenticationPrincipal MeteorUserPrincipal principal, @PathVariable long productId,
            @Valid @RequestBody PublishProductRequest request) {
        return service.publish(principal.id(), productId, request);
    }
}
