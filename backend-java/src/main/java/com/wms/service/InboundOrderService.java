package com.wms.service;

import com.wms.common.PageResult;
import com.wms.dto.inbound.InboundOrderRequest;
import com.wms.dto.inbound.InboundOrderResponse;

public interface InboundOrderService {

    InboundOrderResponse createInboundOrder(InboundOrderRequest request);

    PageResult<InboundOrderResponse> listInboundOrders(int page, int pageSize);

    InboundOrderResponse getInboundOrder(Long id);
}
