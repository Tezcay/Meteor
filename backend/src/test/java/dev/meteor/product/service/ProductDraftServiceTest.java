package dev.meteor.product.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import dev.meteor.product.dto.CreateProductDraftRequest;
import dev.meteor.product.dto.ProductDraftResponse;
import dev.meteor.product.mapper.ProductDraftMapper;
import dev.meteor.product.mapper.param.ProductDraftInsert;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

class ProductDraftServiceTest {
    private ProductDraftMapper mapper;
    private ProductDraftService service;

    @BeforeEach
    void setup() {
        mapper = mock(ProductDraftMapper.class);
        service = new ProductDraftService(mapper);
    }

    @Test
    void createsBothRowsUsingServerSelectedShopAndGeneratedKeys() {
        when(mapper.findActiveShopForUpdate(1L)).thenReturn(101L);
        when(mapper.insertProduct(any())).thenAnswer(invocation -> {
            ProductDraftInsert insert = invocation.getArgument(0);
            assertThat(insert.getShopId()).isEqualTo(101);
            assertThat(insert.getUserId()).isEqualTo(1);
            assertThat(insert.getTitle()).isEqualTo("新模板");
            insert.setProductId(1002L);
            return 1;
        });
        when(mapper.insertRevision(any())).thenAnswer(invocation -> {
            ProductDraftInsert insert = invocation.getArgument(0);
            assertThat(insert.getProductId()).isEqualTo(1002);
            insert.setRevisionId(2L);
            return 1;
        });
        ProductDraftResponse response = draft();
        when(mapper.findOwnedDraft(1L, 1002L, 2L)).thenReturn(response);

        assertThat(service.createDraft(1L, new CreateProductDraftRequest(" 新模板 ", " 简介 ")))
                .isEqualTo(response);
        InOrder order = inOrder(mapper);
        order.verify(mapper).findActiveShopForUpdate(1L);
        order.verify(mapper).insertProduct(any());
        order.verify(mapper).insertRevision(any());
        order.verify(mapper).findOwnedDraft(1L, 1002L, 2L);
    }

    @Test
    void rejectsMissingOrSuspendedShopBeforeWriting() {
        // Mockito 默认把 Long 模拟为 0，这里显式模拟 SQL 没查到记录时的 null。
        when(mapper.findActiveShopForUpdate(1L)).thenReturn(null);
        assertThatThrownBy(() -> service.createDraft(1L, new CreateProductDraftRequest("标题", "简介")))
                .isInstanceOf(CreatorShopUnavailableException.class);
        verify(mapper, never()).insertProduct(any());
        verify(mapper, never()).insertRevision(any());
    }

    @Test
    void rejectsInvalidDataEvenWhenCalledOutsideController() {
        assertThatThrownBy(() -> service.createDraft(1L, new CreateProductDraftRequest(" ", "简介")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.createDraft(1L, new CreateProductDraftRequest("x".repeat(161), "简介")))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(mapper);
    }

    @Test
    void stopsWhenProductInsertDoesNotWriteARow() {
        when(mapper.findActiveShopForUpdate(1L)).thenReturn(101L);
        when(mapper.insertProduct(any())).thenReturn(0);
        assertThatThrownBy(() -> service.createDraft(1L, new CreateProductDraftRequest("标题", "简介")))
                .isInstanceOf(IllegalStateException.class);
        verify(mapper, never()).insertRevision(any());
    }

    @Test
    void listsOnlyCurrentUsersDraftsAndSupportsEmptyList() {
        when(mapper.findOwnDrafts(1L)).thenReturn(List.of(draft()));
        when(mapper.findOwnDrafts(9L)).thenReturn(List.of());
        assertThat(service.listOwnDrafts(1L)).containsExactly(draft());
        assertThat(service.listOwnDrafts(9L)).isEmpty();
    }

    private ProductDraftResponse draft() {
        return new ProductDraftResponse(1002L, 2L, "新模板", "简介", "DRAFT", "OFF_SALE", Instant.EPOCH);
    }
}
