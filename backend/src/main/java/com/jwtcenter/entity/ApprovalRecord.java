package com.jwtcenter.entity;

import com.jwtcenter.enums.ApprovalDecision;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "approval_records")
public class ApprovalRecord extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "performance_record_id", nullable = false)
    private PerformanceRecord performanceRecord;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reviewer_id", nullable = false)
    private UserAccount reviewer;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ApprovalDecision decision;

    @Column(length = 255)
    private String reason;

    public PerformanceRecord getPerformanceRecord() {
        return performanceRecord;
    }

    public void setPerformanceRecord(PerformanceRecord performanceRecord) {
        this.performanceRecord = performanceRecord;
    }

    public UserAccount getReviewer() {
        return reviewer;
    }

    public void setReviewer(UserAccount reviewer) {
        this.reviewer = reviewer;
    }

    public ApprovalDecision getDecision() {
        return decision;
    }

    public void setDecision(ApprovalDecision decision) {
        this.decision = decision;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
