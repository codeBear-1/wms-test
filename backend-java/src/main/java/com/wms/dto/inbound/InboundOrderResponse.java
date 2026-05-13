package com.wms.dto.inbound;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class InboundOrderResponse {
    private Long id;
    private String orderNo;
    private String supplierName;
    private String status;
    private List<InboundOrderItemResponse> items;
    private LocalDateTime createdAt;
}
