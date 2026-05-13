package com.wms.controller;

import com.wms.common.ApiResponse;
import com.wms.common.PageResult;
import com.wms.dto.inventory.InventoryResponseDTO;
import com.wms.repository.InventoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryRepository inventoryRepository;

    @GetMapping
    public ApiResponse<PageResult<InventoryResponseDTO>> getInventory(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long warehouseId,
            @RequestParam(required = false) String locationCode,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {

        String kw = (keyword == null || keyword.isBlank()) ? null : keyword.trim();
        String loc = (locationCode == null || locationCode.isBlank()) ? null : locationCode.trim();
        int safePage = Math.max(1, page);
        int safeSize = Math.min(100, Math.max(1, pageSize));

        Page<InventoryResponseDTO> pageData = inventoryRepository.findInventoryWithDetails(
                kw, warehouseId, loc, PageRequest.of(safePage - 1, safeSize));

        PageResult<InventoryResponseDTO> result = new PageResult<>();
        result.setList(pageData.getContent());
        result.setTotal(pageData.getTotalElements());
        result.setPage(safePage);
        result.setPageSize(safeSize);

        return ApiResponse.success(result);
    }
}
