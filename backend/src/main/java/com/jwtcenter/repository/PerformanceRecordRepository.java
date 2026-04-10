package com.jwtcenter.repository;

import com.jwtcenter.entity.PerformanceRecord;
import com.jwtcenter.enums.PerformanceStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PerformanceRecordRepository extends JpaRepository<PerformanceRecord, Long> {

    List<PerformanceRecord> findByOwner_IdOrderByOccurredOnDescCreatedAtDesc(Long ownerId);

    List<PerformanceRecord> findByDepartmentOrderByOccurredOnDescCreatedAtDesc(String department);

    List<PerformanceRecord> findByDepartment(String department);

    List<PerformanceRecord> findAllByOrderByOccurredOnDescCreatedAtDesc();

    List<PerformanceRecord> findByDepartmentAndStatusOrderByCreatedAtAsc(String department, PerformanceStatus status);

    List<PerformanceRecord> findByStatusOrderByCreatedAtAsc(PerformanceStatus status);

    long countByDepartment(String department);
}
