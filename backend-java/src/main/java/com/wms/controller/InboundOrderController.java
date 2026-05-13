package com.wms.controller;

import com.wms.common.ApiResponse;
import com.wms.common.PageResult;
import com.wms.dto.inbound.InboundOrderRequest;
import com.wms.dto.inbound.InboundOrderResponse;
import com.wms.service.InboundOrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/inbound-orders")
@RequiredArgsConstructor
public class InboundOrderController {

    private final InboundOrderService inboundOrderService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<InboundOrderResponse> createInboundOrder(@Valid @RequestBody InboundOrderRequest request) {
        InboundOrderResponse response = inboundOrderService.createInboundOrder(request);
        return new ApiResponse<>(201, "入库单创建成功", response);
    }

    /**
     * API_SPEC 3.2 入库单列表
     */
    @GetMapping
    public ApiResponse<PageResult<InboundOrderResponse>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        int safePage = Math.max(1, page);
        int safeSize = Math.min(100, Math.max(1, pageSize));
        return ApiResponse.success(inboundOrderService.listInboundOrders(safePage, safeSize));
    }

    /**
     * API_SPEC 3.3 入库单详情
     */
    @GetMapping("/{id}")
    public ApiResponse<InboundOrderResponse> getById(@PathVariable Long id) {
        return ApiResponse.success(inboundOrderService.getInboundOrder(id));
    }
}
