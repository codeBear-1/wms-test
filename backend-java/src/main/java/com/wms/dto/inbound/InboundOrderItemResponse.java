package com.wms.dto.inbound;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class InboundOrderItemResponse {
    private Long productId;
    private String productName;
    private Integer quantity;
    private String locationCode;
}
