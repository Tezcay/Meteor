package dev.meteor.product.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import dev.meteor.product.dto.ProductPublicationResponse;
import dev.meteor.product.dto.PublishProductRequest;
import dev.meteor.product.mapper.ProductPublicationMapper;
import dev.meteor.product.mapper.param.ProductPublicationInsert;
import dev.meteor.product.mapper.projection.OwnedProductRevisionProjection;
import dev.meteor.product.mapper.projection.ReusableDeliveryFileProjection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ProductPublicationServiceTest {
    private static final String HASH = "d56ccbeda6e7ca6610a9cb5b12fd7fb9231ec12787707b3577dfa87d56430f21";
    private static final PublishProductRequest REQUEST = new PublishProductRequest(" Basic ", 1900, 5001);
    private static final ProductPublicationResponse RESULT =
            new ProductPublicationResponse(1002, 2, 2003, 3003, 4003, 6007, "Basic", 1900, "CNY", 5001);
    private final ProductPublicationMapper mapper = mock(ProductPublicationMapper.class);
    private final ProductPublicationService service = new ProductPublicationService(mapper);

    @BeforeEach
    void setupDraft() {
        when(mapper.findOwnedRevisionForUpdate(1, 1002)).thenReturn(
                new OwnedProductRevisionProjection(1002, 2, null, "DRAFT", "OFF_SALE", "简介"));
        when(mapper.findReusableFileForUpdate(1, 5001)).thenReturn(
                new ReusableDeliveryFileProjection(5001, "basic-resume-template.html", "text/html",
                        634, "delivery-fixtures/basic/resume-template.html", HASH));
    }

    @Test
    void publishesOnlyAfterVerifyingAccessibleRealFile() {
        when(mapper.insertPackage(any())).thenAnswer(call -> {
            call.getArgument(0, ProductPublicationInsert.class).setPackageId(2003); return 1;
        });
        when(mapper.insertRelease(any())).thenAnswer(call -> {
            assertThat(call.getArgument(0, ProductPublicationInsert.class).getPackageId()).isEqualTo(2003);
            call.getArgument(0, ProductPublicationInsert.class).setReleaseId(3003); return 1;
        });
        when(mapper.insertManifest(any())).thenReturn(1);
        when(mapper.insertManifestItem(any())).thenReturn(1);
        when(mapper.markRevisionPublished(any())).thenReturn(1);
        when(mapper.markProductOnSale(any())).thenReturn(1);
        when(mapper.findPublication(1, 1002)).thenReturn(RESULT);
        assertThat(service.publish(1, 1002, REQUEST)).isEqualTo(RESULT);
        verify(mapper).findReusableFileForUpdate(1, 5001);
    }

    @Test
    void samePublishedSettingsReplayWithoutCreatingAnotherRelease() {
        published();
        assertThat(service.publish(1, 1002, REQUEST)).isEqualTo(RESULT);
        verify(mapper, never()).findReusableFileForUpdate(anyLong(), anyLong());
        verify(mapper, never()).insertPackage(any());
    }

    @Test
    void publishedSettingsCannotBeChangedOnReplay() {
        published();
        assertThatThrownBy(() -> service.publish(1, 1002, new PublishProductRequest("Pro", 3900, 5001)))
                .isInstanceOf(ProductPublicationConflictException.class);
        verify(mapper, never()).insertPackage(any());
    }

    @Test
    void missingOrForeignDraftStopsBeforeAssetLookup() {
        assertThatThrownBy(() -> service.publish(2, 1002, REQUEST))
                .isInstanceOf(ProductDraftNotFoundException.class);
        verify(mapper, never()).findReusableFileForUpdate(anyLong(), anyLong());
    }

    @Test
    void validatesPriceBeforeAccessingDatabase() {
        assertThatThrownBy(() -> service.publish(1, 1002, new PublishProductRequest("Basic", 0, 5001)))
                .isInstanceOf(IllegalArgumentException.class);
        verify(mapper, never()).findOwnedRevisionForUpdate(anyLong(), anyLong());
    }

    @Test
    void unavailableAssetCannotBeUsedEvenWhenDraftIsOwned() {
        when(mapper.findReusableFileForUpdate(1, 5001)).thenReturn(null);
        assertThatThrownBy(() -> service.publish(1, 1002, REQUEST))
                .isInstanceOf(ProductPublicationConflictException.class);
        verify(mapper, never()).insertPackage(any());
    }

    @Test
    void corruptedAssetStopsBeforeAnyPublicationWrite() {
        when(mapper.findReusableFileForUpdate(1, 5001)).thenReturn(
                new ReusableDeliveryFileProjection(5001, "bad.html", "text/html", 635,
                        "delivery-fixtures/basic/resume-template.html", HASH));
        assertThatThrownBy(() -> service.publish(1, 1002, REQUEST))
                .isInstanceOf(ProductPublicationConflictException.class);
        verify(mapper, never()).insertPackage(any());
    }

    private void published() {
        when(mapper.findOwnedRevisionForUpdate(1, 1002)).thenReturn(
                new OwnedProductRevisionProjection(1002, 2, 2L, "PUBLISHED", "ON_SALE", "简介"));
        when(mapper.findPublication(1, 1002)).thenReturn(RESULT);
    }
}
