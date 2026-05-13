package com.wms.service;

import com.wms.common.BusinessException;
import com.wms.dto.inbound.InboundOrderItemRequest;
import com.wms.dto.inbound.InboundOrderRequest;
import com.wms.dto.inbound.InboundOrderResponse;
import com.wms.entity.InboundOrder;
import com.wms.entity.Inventory;
import com.wms.entity.Product;
import com.wms.repository.*;
import com.wms.service.impl.InboundOrderServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InboundOrderServiceImplTest {

    @Mock
    private InboundOrderRepository inboundOrderRepository;
    @Mock
    private InboundOrderItemRepository inboundOrderItemRepository;
    @Mock
    private InventoryRepository inventoryRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private LocationRepository locationRepository;

    @InjectMocks
    private InboundOrderServiceImpl inboundOrderService;

    private InboundOrderRequest request;

    @BeforeEach
    void setUp() {
        request = new InboundOrderRequest();
        request.setSupplierName("Test Supplier");

        InboundOrderItemRequest item = new InboundOrderItemRequest();
        item.setProductId(1L);
        item.setQuantity(100);
        item.setLocationCode("LOC-01");

        request.setItems(List.of(item));
    }

    @Test
    void createInboundOrder_Success() {
        when(inboundOrderRepository.countByOrderNoStartingWith(anyString())).thenReturn(0L);

        Product product = new Product();
        product.setId(1L);
        product.setName("Test Product");
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        when(locationRepository.existsByCode("LOC-01")).thenReturn(true);

        when(inboundOrderRepository.save(any(InboundOrder.class))).thenAnswer(inv -> {
            InboundOrder o = inv.getArgument(0);
            o.setId(100L);
            return o;
        });

        Inventory inventory = new Inventory();
        inventory.setProductId(1L);
        inventory.setLocationCode("LOC-01");
        inventory.setQuantity(50);
        when(inventoryRepository.findByProductIdAndLocationCode(1L, "LOC-01"))
                .thenReturn(Optional.of(inventory));

        when(inboundOrderItemRepository.findByOrderId(100L)).thenReturn(List.of(
                com.wms.entity.InboundOrderItem.builder()
                        .id(1L)
                        .orderId(100L)
                        .productId(1L)
                        .quantity(100)
                        .locationCode("LOC-01")
                        .build()
        ));

        InboundOrderResponse response = inboundOrderService.createInboundOrder(request);

        assertNotNull(response);
        assertEquals(100L, response.getId());
        assertTrue(response.getOrderNo().matches("IN-\\d{8}-\\d{3}"));
        assertEquals("Test Supplier", response.getSupplierName());
        assertEquals(1, response.getItems().size());
        assertEquals(150, inventory.getQuantity());

        verify(inboundOrderRepository, times(1)).save(any());
        verify(inboundOrderItemRepository, times(1)).save(any());
        verify(inventoryRepository, times(1)).save(any());
    }

    @Test
    void createInboundOrder_ProductNotFound() {
        when(productRepository.findById(1L)).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> inboundOrderService.createInboundOrder(request));

        assertTrue(ex.getMessage().contains("商品不存在"));
        verify(inboundOrderRepository, never()).save(any());
        verify(inboundOrderItemRepository, never()).save(any());
    }
}
