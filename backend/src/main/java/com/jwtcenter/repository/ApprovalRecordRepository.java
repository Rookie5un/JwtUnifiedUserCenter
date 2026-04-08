package com.jwtcenter.repository;

import com.jwtcenter.entity.ApprovalRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ApprovalRecordRepository extends JpaRepository<ApprovalRecord, Long> {

    List<ApprovalRecord> findByPerformanceRecord_IdOrderByCreatedAtDesc(Long performanceRecordId);
}
