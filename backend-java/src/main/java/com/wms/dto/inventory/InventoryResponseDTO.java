package com.wms.dto.inventory;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryResponseDTO {
    private Long productId;
    private String productName;
    private String sku;
    private String locationCode;
    private String warehouseName;
    private Integer quantity;
    private LocalDateTime updatedAt;
}
