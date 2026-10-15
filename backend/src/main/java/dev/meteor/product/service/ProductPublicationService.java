package dev.meteor.product.service;

import dev.meteor.product.dto.ProductDraftResponse;
import dev.meteor.product.dto.ProductPublicationResponse;
import dev.meteor.product.dto.PublishProductRequest;
import dev.meteor.product.dto.ReusableDeliveryAssetResponse;
import dev.meteor.product.mapper.ProductPublicationMapper;
import dev.meteor.product.mapper.param.ProductPublicationInsert;
import dev.meteor.product.mapper.projection.OwnedProductRevisionProjection;
import dev.meteor.product.mapper.projection.ReusableDeliveryFileProjection;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 一个商品首次发布：一个套餐、一个正式版本、一份冻结单文件清单。 */
@Service
public class ProductPublicationService {
    private final ProductPublicationMapper mapper;
    public ProductPublicationService(ProductPublicationMapper mapper) { this.mapper = mapper; }

    @Transactional(readOnly = true)
    public List<ProductDraftResponse> listOwnProducts(long userId) {
        return mapper.findOwnProducts(userId);
    }

    @Transactional(readOnly = true)
    public List<ReusableDeliveryAssetResponse> listReusableAssets(long userId) {
        return mapper.findReusableFiles(userId).stream()
                .map(file -> new ReusableDeliveryAssetResponse(
                        file.id(), file.filename(), file.mediaType(), file.sizeBytes()))
                .toList();
    }

    @Transactional
    public ProductPublicationResponse publish(long userId, long productId, PublishProductRequest request) {
        validateRequest(productId, request);
        // 行锁串行处理同一个商品；查询同时限定 Session 用户与有效店铺，越权不进入后续写入。
        OwnedProductRevisionProjection revision = mapper.findOwnedRevisionForUpdate(userId, productId);
        if (revision == null) { throw new ProductDraftNotFoundException(); }
        if ("PUBLISHED".equals(revision.revisionStatus())) {
            ProductPublicationResponse existing = mapper.findPublication(userId, productId);
            // 发布后的结果不可编辑。相同参数重放返回原结果，换参数不能偷偷变更成交对象。
            if (existing != null && "ON_SALE".equals(revision.saleStatus())
                    && revision.publishedRevisionId() != null
                    && revision.publishedRevisionId() == revision.revisionId()
                    && existing.packageName().equals(request.packageName())
                    && existing.priceCents() == request.priceCents()
                    && existing.fileObjectId() == request.fileObjectId()) {
                return existing;
            }
            throw new ProductPublicationConflictException("Product was already published with different settings");
        }
        if (!"DRAFT".equals(revision.revisionStatus()) || !"OFF_SALE".equals(revision.saleStatus())
                || revision.publishedRevisionId() != null) {
            throw new ProductPublicationConflictException("Product is not an unpublished draft");
        }

        // FileObject 没有 owner 字段：通过自己店铺的正式交付清单证明可复用资格，并锁住来源。
        ReusableDeliveryFileProjection file = mapper.findReusableFileForUpdate(userId, request.fileObjectId());
        if (file == null) {
            throw new ProductPublicationConflictException("Selected delivery asset is unavailable to this shop");
        }
        verifyDeliveryFile(file);
        ProductPublicationInsert publication = new ProductPublicationInsert(
                productId, revision.revisionId(), userId, request.packageName(),
                request.priceCents(), file.id(), revision.summary(), file.filename());
        requireOne(mapper.insertPackage(publication));
        requireOne(mapper.insertRelease(publication));
        requireOne(mapper.insertManifest(publication));
        requireOne(mapper.insertManifestItem(publication));
        requireOne(mapper.markRevisionPublished(publication));
        // 完整交付准备好后才使商品可售；任一步异常都会回滚整组写入，公开目录不会看到半成品。
        requireOne(mapper.markProductOnSale(publication));
        ProductPublicationResponse response = mapper.findPublication(userId, productId);
        if (response == null) { throw new IllegalStateException("Published product could not be loaded"); }
        return response;
    }

    private static void validateRequest(long productId, PublishProductRequest request) {
        // Service 也可能被其他服务直接调用，不能只依赖 Controller 的 Bean Validation。
        if (productId <= 0 || request == null || request.packageName() == null
                || request.packageName().isBlank() || request.packageName().length() > 80
                || request.priceCents() < 1 || request.priceCents() > 100_000_000
                || request.fileObjectId() <= 0) {
            throw new IllegalArgumentException("A valid draft, package name, price and delivery asset are required");
        }
    }

    private static void requireOne(int rows) {
        if (rows != 1) { throw new IllegalStateException("Publication write did not affect exactly one row"); }
    }

    private static void verifyDeliveryFile(ReusableDeliveryFileProjection file) {
        // 发布时也检查实物，防止只有清单元数据、文件缺失或摘要不符的商品上架。
        try (InputStream source = new ClassPathResource(file.storageKey()).getInputStream()) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            long bytes;
            try (DigestInputStream input = new DigestInputStream(source, digest)) {
                bytes = input.transferTo(OutputStream.nullOutputStream());
            }
            if (bytes != file.sizeBytes()
                    || !HexFormat.of().formatHex(digest.digest()).equalsIgnoreCase(file.contentSha256())) {
                throw new ProductPublicationConflictException("Delivery asset integrity check failed");
            }
        } catch (IOException exception) {
            throw new ProductPublicationConflictException("Delivery asset could not be read");
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}
