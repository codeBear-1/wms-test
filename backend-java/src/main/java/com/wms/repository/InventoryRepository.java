package com.wms.repository;

import com.wms.dto.inventory.InventoryResponseDTO;
import com.wms.entity.Inventory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 库存 Repository — 候选人需要实现库存查询（任务2）
 * 提示：你可能需要添加自定义查询方法
 */
@Repository
public interface InventoryRepository extends JpaRepository<Inventory, Long> {

    Optional<Inventory> findByProductIdAndLocationCode(Long productId, String locationCode);

    boolean existsByProductId(Long productId);

    @Query("SELECT new com.wms.dto.inventory.InventoryResponseDTO(p.id, p.name, p.sku, i.locationCode, w.name, i.quantity, i.updatedAt) "
            + "FROM Inventory i "
            + "JOIN Product p ON i.productId = p.id "
            + "JOIN Location l ON i.locationCode = l.code "
            + "JOIN Warehouse w ON l.warehouseId = w.id "
            + "WHERE (:keyword IS NULL OR :keyword = '' "
            + "OR LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) "
            + "OR LOWER(p.sku) LIKE LOWER(CONCAT('%', :keyword, '%'))) "
            + "AND (:warehouseId IS NULL OR w.id = :warehouseId) "
            + "AND (:locationCode IS NULL OR :locationCode = '' OR i.locationCode = :locationCode)")
    Page<InventoryResponseDTO> findInventoryWithDetails(
            @Param("keyword") String keyword,
            @Param("warehouseId") Long warehouseId,
            @Param("locationCode") String locationCode,
            Pageable pageable);
}
