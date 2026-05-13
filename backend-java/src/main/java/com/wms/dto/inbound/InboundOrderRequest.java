package com.wms.dto.inbound;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class InboundOrderRequest {
    private String supplierName;

    @NotEmpty(message = "入库明细不能为空")
    @Valid
    private List<InboundOrderItemRequest> items;
}
