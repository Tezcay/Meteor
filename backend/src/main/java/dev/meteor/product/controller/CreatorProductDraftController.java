package dev.meteor.product.controller;

import dev.meteor.auth.security.MeteorUserPrincipal;
import dev.meteor.product.dto.CreateProductDraftRequest;
import dev.meteor.product.dto.ProductDraftResponse;
import dev.meteor.product.service.ProductDraftService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** 创作者入口。SecurityConfig 限制 CREATOR 角色，POST 还需要有效 CSRF token。 */
@RestController
@RequestMapping("/api/creator/product-drafts")
public class CreatorProductDraftController {
    private final ProductDraftService service;

    public CreatorProductDraftController(ProductDraftService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProductDraftResponse create(
            @AuthenticationPrincipal MeteorUserPrincipal principal,
            @Valid @RequestBody CreateProductDraftRequest request
    ) {
        // 用户 ID 来自服务端 Session，不相信请求体指定的 userId 或 shopId。
        return service.createDraft(principal.id(), request);
    }

    @GetMapping
    public List<ProductDraftResponse> list(@AuthenticationPrincipal MeteorUserPrincipal principal) {
        return service.listOwnDrafts(principal.id());
    }
}
