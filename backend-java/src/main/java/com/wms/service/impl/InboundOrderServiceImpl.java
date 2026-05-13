package com.wms.service.impl;

import com.wms.common.BusinessException;
import com.wms.common.PageResult;
import com.wms.dto.inbound.InboundOrderItemRequest;
import com.wms.dto.inbound.InboundOrderItemResponse;
import com.wms.dto.inbound.InboundOrderRequest;
import com.wms.dto.inbound.InboundOrderResponse;
import com.wms.entity.InboundOrder;
import com.wms.entity.InboundOrderItem;
import com.wms.entity.Inventory;
import com.wms.entity.Product;
import com.wms.repository.InboundOrderItemRepository;
import com.wms.repository.InboundOrderRepository;
import com.wms.repository.InventoryRepository;
import com.wms.repository.LocationRepository;
import com.wms.repository.ProductRepository;
import com.wms.service.InboundOrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class InboundOrderServiceImpl implements InboundOrderService {

    /** 单机环境下保证入库单号序号生成的原子性（与 TASK/API_SPEC：IN-yyyyMMdd-XXX 一致） */
    private static final Object ORDER_NO_LOCK = new Object();

    private final InboundOrderRepository inboundOrderRepository;
    private final InboundOrderItemRepository inboundOrderItemRepository;
    private final InventoryRepository inventoryRepository;
    private final ProductRepository productRepository;
    private final LocationRepository locationRepository;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public InboundOrderResponse createInboundOrder(InboundOrderRequest request) {
        // 先校验明细，避免无效数据占用单号（仍在同一事务内）
        validateItems(request);

        String orderNo = generateOrderNo();
        log.info("创建入库单开始: orderNo={}, supplier={}, lineCount={}",
                orderNo, request.getSupplierName(), request.getItems().size());

        InboundOrder order = InboundOrder.builder()
                .orderNo(orderNo)
                .supplierName(request.getSupplierName())
                .status("COMPLETED")
                .build();
        order = inboundOrderRepository.save(order);

        for (InboundOrderItemRequest itemReq : request.getItems()) {
            InboundOrderItem orderItem = InboundOrderItem.builder()
                    .orderId(order.getId())
                    .productId(itemReq.getProductId())
                    .quantity(itemReq.getQuantity())
                    .locationCode(itemReq.getLocationCode())
                    .build();
            inboundOrderItemRepository.save(orderItem);

            Inventory inventory = inventoryRepository
                    .findByProductIdAndLocationCode(itemReq.getProductId(), itemReq.getLocationCode())
                    .orElse(Inventory.builder()
                            .productId(itemReq.getProductId())
                            .locationCode(itemReq.getLocationCode())
                            .quantity(0)
                            .build());

            inventory.setQuantity(inventory.getQuantity() + itemReq.getQuantity());
            inventoryRepository.save(inventory);
        }

        log.info("创建入库单完成: id={}, orderNo={}", order.getId(), order.getOrderNo());
        return buildResponseWithItems(order);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<InboundOrderResponse> listInboundOrders(int page, int pageSize) {
        Pageable pageable = PageRequest.of(page - 1, pageSize, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<InboundOrder> po = inboundOrderRepository.findAll(pageable);
        List<InboundOrderResponse> list = po.getContent().stream()
                .map(this::buildResponseWithItems)
                .toList();

        PageResult<InboundOrderResponse> result = new PageResult<>();
        result.setList(list);
        result.setTotal(po.getTotalElements());
        result.setPage(page);
        result.setPageSize(pageSize);

        log.debug("入库单分页查询: page={}, pageSize={}, total={}", page, pageSize, po.getTotalElements());
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public InboundOrderResponse getInboundOrder(Long id) {
        InboundOrder order = inboundOrderRepository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "入库单不存在"));
        log.info("查询入库单详情: id={}, orderNo={}", id, order.getOrderNo());
        return buildResponseWithItems(order);
    }

    private void validateItems(InboundOrderRequest request) {
        for (InboundOrderItemRequest itemReq : request.getItems()) {
            productRepository.findById(itemReq.getProductId())
                    .orElseThrow(() -> new BusinessException(400, "商品不存在: " + itemReq.getProductId()));
            if (!locationRepository.existsByCode(itemReq.getLocationCode())) {
                throw new BusinessException(400, "库位不存在: " + itemReq.getLocationCode());
            }
        }
    }

    private String generateOrderNo() {
        synchronized (ORDER_NO_LOCK) {
            String dateStr = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
            String prefix = "IN-" + dateStr + "-";
            long next = inboundOrderRepository.countByOrderNoStartingWith(prefix) + 1;
            String suffix = next <= 999 ? String.format("%03d", next) : Long.toString(next);
            return prefix + suffix;
        }
    }

    private InboundOrderResponse buildResponseWithItems(InboundOrder order) {
        List<InboundOrderItem> items = inboundOrderItemRepository.findByOrderId(order.getId());
        List<InboundOrderItemResponse> itemResponses = new ArrayList<>();
        for (InboundOrderItem item : items) {
            Product p = productRepository.findById(item.getProductId())
                    .orElseThrow(() -> new BusinessException(500, "商品数据不一致: " + item.getProductId()));
            itemResponses.add(InboundOrderItemResponse.builder()
                    .productId(item.getProductId())
                    .productName(p.getName())
                    .quantity(item.getQuantity())
                    .locationCode(item.getLocationCode())
                    .build());
        }
        return InboundOrderResponse.builder()
                .id(order.getId())
                .orderNo(order.getOrderNo())
                .supplierName(order.getSupplierName())
                .status(order.getStatus())
                .items(itemResponses)
                .createdAt(order.getCreatedAt())
                .build();
    }
}
