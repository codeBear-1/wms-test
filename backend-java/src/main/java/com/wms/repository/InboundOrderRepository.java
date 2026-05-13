package com.wms.repository;

import com.wms.entity.InboundOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * 入库单 Repository — 候选人需要实现
 */
@Repository
public interface InboundOrderRepository extends JpaRepository<InboundOrder, Long> {

    /** 用于生成入库单号 IN-yyyyMMdd-XXX（当日序号） */
    long countByOrderNoStartingWith(String prefix);
}
