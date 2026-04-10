package com.jwtcenter.service;

import com.jwtcenter.dto.performance.ApprovalDecisionRequest;
import com.jwtcenter.dto.performance.DashboardResponse;
import com.jwtcenter.dto.performance.MonthlyStatResponse;
import com.jwtcenter.dto.performance.PerformanceRecordRequest;
import com.jwtcenter.dto.performance.PerformanceRecordResponse;
import com.jwtcenter.dto.performance.RankingResponse;
import com.jwtcenter.entity.ApprovalRecord;
import com.jwtcenter.entity.PerformanceRecord;
import com.jwtcenter.entity.UserAccount;
import com.jwtcenter.enums.ApprovalDecision;
import com.jwtcenter.enums.OperationResult;
import com.jwtcenter.enums.PerformanceStatus;
import com.jwtcenter.exception.ApiException;
import com.jwtcenter.repository.ApprovalRecordRepository;
import com.jwtcenter.repository.PerformanceRecordRepository;
import com.jwtcenter.security.PermissionCodes;
import com.jwtcenter.util.MapperUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class PerformanceService {

    private final PerformanceRecordRepository performanceRecordRepository;
    private final ApprovalRecordRepository approvalRecordRepository;
    private final AccessService accessService;
    private final OperationLogService operationLogService;

    public PerformanceService(
        PerformanceRecordRepository performanceRecordRepository,
        ApprovalRecordRepository approvalRecordRepository,
        AccessService accessService,
        OperationLogService operationLogService
    ) {
        this.performanceRecordRepository = performanceRecordRepository;
        this.approvalRecordRepository = approvalRecordRepository;
        this.accessService = accessService;
        this.operationLogService = operationLogService;
    }

    @Transactional(readOnly = true)
    public List<PerformanceRecordResponse> listRecords() {
        UserAccount actor = accessService.currentUser();
        return visibleRecords(actor).stream().map(MapperUtils::toPerformanceRecordResponse).toList();
    }

    @Transactional
    public PerformanceRecordResponse createRecord(PerformanceRecordRequest request) {
        UserAccount actor = accessService.currentUser();
        accessService.requirePermission(actor, PermissionCodes.PERFORMANCE_CREATE);
        PerformanceRecord record = new PerformanceRecord();
        record.setOwner(actor);
        record.setDepartment(actor.getDepartment());
        apply(record, request);
        PerformanceRecord saved = performanceRecordRepository.save(record);
        operationLogService.log(actor, "CREATE_PERFORMANCE", "PERFORMANCE_RECORD", String.valueOf(saved.getId()), OperationResult.SUCCESS, "Performance record submitted.");
        return MapperUtils.toPerformanceRecordResponse(saved);
    }

    @Transactional
    public PerformanceRecordResponse updateRecord(Long recordId, PerformanceRecordRequest request) {
        UserAccount actor = accessService.currentUser();
        accessService.requirePermission(actor, PermissionCodes.PERFORMANCE_EDIT_SELF);
        PerformanceRecord record = findRecord(recordId);
        if (!record.getOwner().getId().equals(actor.getId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN", "You can only edit your own records.");
        }
        if (record.getStatus() == PerformanceStatus.APPROVED) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "RECORD_LOCKED", "Approved records cannot be changed.");
        }
        apply(record, request);
        record.setStatus(PerformanceStatus.PENDING);
        record.setRejectedReason(null);
        record.setApprovedAt(null);
        record.setApprovedBy(null);
        PerformanceRecord saved = performanceRecordRepository.save(record);
        operationLogService.log(actor, "UPDATE_PERFORMANCE", "PERFORMANCE_RECORD", String.valueOf(recordId), OperationResult.SUCCESS, "Performance record updated.");
        return MapperUtils.toPerformanceRecordResponse(saved);
    }

    @Transactional
    public void deleteRecord(Long recordId) {
        UserAccount actor = accessService.currentUser();
        accessService.requirePermission(actor, PermissionCodes.PERFORMANCE_DELETE_SELF);
        PerformanceRecord record = findRecord(recordId);
        if (!record.getOwner().getId().equals(actor.getId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN", "You can only delete your own records.");
        }
        if (record.getStatus() == PerformanceStatus.APPROVED) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "RECORD_LOCKED", "Approved records cannot be deleted.");
        }
        performanceRecordRepository.delete(record);
        operationLogService.log(actor, "DELETE_PERFORMANCE", "PERFORMANCE_RECORD", String.valueOf(recordId), OperationResult.SUCCESS, "Performance record deleted.");
    }

    @Transactional(readOnly = true)
    public List<PerformanceRecordResponse> pendingApprovals() {
        UserAccount actor = accessService.currentUser();
        accessService.requirePermission(actor, PermissionCodes.PERFORMANCE_APPROVE);
        List<PerformanceRecord> records = canViewGlobal(actor)
            ? performanceRecordRepository.findByStatusOrderByCreatedAtAsc(PerformanceStatus.PENDING)
            : canViewDepartment(actor)
                ? performanceRecordRepository.findByDepartmentAndStatusOrderByCreatedAtAsc(actor.getDepartment(), PerformanceStatus.PENDING)
                : forbidden("Department or global performance visibility is required for approvals.");
        return records.stream().map(MapperUtils::toPerformanceRecordResponse).toList();
    }

    @Transactional
    public PerformanceRecordResponse approve(Long recordId) {
        UserAccount actor = accessService.currentUser();
        PerformanceRecord record = enforceApprovalScope(actor, findRecord(recordId));
        record.setStatus(PerformanceStatus.APPROVED);
        record.setRejectedReason(null);
        record.setApprovedAt(Instant.now());
        record.setApprovedBy(actor);
        PerformanceRecord saved = performanceRecordRepository.save(record);
        createApprovalRecord(saved, actor, ApprovalDecision.APPROVED, null);
        operationLogService.log(actor, "APPROVE_PERFORMANCE", "PERFORMANCE_RECORD", String.valueOf(recordId), OperationResult.SUCCESS, "Performance approved.");
        return MapperUtils.toPerformanceRecordResponse(saved);
    }

    @Transactional
    public PerformanceRecordResponse reject(Long recordId, ApprovalDecisionRequest request) {
        UserAccount actor = accessService.currentUser();
        if (request.reason() == null || request.reason().isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "REASON_REQUIRED", "Reject reason is required.");
        }
        PerformanceRecord record = enforceApprovalScope(actor, findRecord(recordId));
        record.setStatus(PerformanceStatus.REJECTED);
        record.setRejectedReason(request.reason());
        record.setApprovedAt(null);
        record.setApprovedBy(null);
        PerformanceRecord saved = performanceRecordRepository.save(record);
        createApprovalRecord(saved, actor, ApprovalDecision.REJECTED, request.reason());
        operationLogService.log(actor, "REJECT_PERFORMANCE", "PERFORMANCE_RECORD", String.valueOf(recordId), OperationResult.SUCCESS, "Performance rejected.");
        return MapperUtils.toPerformanceRecordResponse(saved);
    }

    @Transactional(readOnly = true)
    public DashboardResponse personalDashboard() {
        UserAccount actor = accessService.currentUser();
        accessService.requireAnyPermission(actor,
            PermissionCodes.PERFORMANCE_VIEW_SELF,
            PermissionCodes.PERFORMANCE_VIEW_DEPARTMENT,
            PermissionCodes.PERFORMANCE_VIEW_GLOBAL);
        return dashboard("personal", performanceRecordRepository.findByOwner_IdOrderByOccurredOnDescCreatedAtDesc(actor.getId()));
    }

    @Transactional(readOnly = true)
    public DashboardResponse departmentDashboard() {
        UserAccount actor = accessService.currentUser();
        accessService.requireAnyPermission(actor, PermissionCodes.PERFORMANCE_VIEW_DEPARTMENT, PermissionCodes.PERFORMANCE_VIEW_GLOBAL);
        return dashboard("department", performanceRecordRepository.findByDepartmentOrderByOccurredOnDescCreatedAtDesc(actor.getDepartment()));
    }

    @Transactional(readOnly = true)
    public DashboardResponse globalDashboard() {
        UserAccount actor = accessService.currentUser();
        accessService.requirePermission(actor, PermissionCodes.PERFORMANCE_VIEW_GLOBAL);
        return dashboard("global", performanceRecordRepository.findAllByOrderByOccurredOnDescCreatedAtDesc());
    }

    @Transactional(readOnly = true)
    public List<MonthlyStatResponse> monthlyStatistics(String scope) {
        return dashboard(scope, scopedRecords(scope)).monthly();
    }

    @Transactional(readOnly = true)
    public List<RankingResponse> ranking(String scope) {
        return dashboard(scope, scopedRecords(scope)).ranking();
    }

    private void apply(PerformanceRecord record, PerformanceRecordRequest request) {
        record.setAmount(request.amount());
        record.setOccurredOn(request.occurredOn());
        record.setType(request.type());
        record.setNote(request.note());
    }

    private PerformanceRecord findRecord(Long recordId) {
        return performanceRecordRepository.findById(recordId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "RECORD_NOT_FOUND", "Performance record not found."));
    }

    private PerformanceRecord enforceApprovalScope(UserAccount actor, PerformanceRecord record) {
        accessService.requirePermission(actor, PermissionCodes.PERFORMANCE_APPROVE);
        if (record.getStatus() != PerformanceStatus.PENDING) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_STATUS", "Only pending records can be reviewed.");
        }
        if (!canViewGlobal(actor) && (!canViewDepartment(actor) || !actor.getDepartment().equals(record.getDepartment()))) {
            throw new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN", "You can only review records from your department.");
        }
        return record;
    }

    private void createApprovalRecord(PerformanceRecord record, UserAccount reviewer, ApprovalDecision decision, String reason) {
        ApprovalRecord approvalRecord = new ApprovalRecord();
        approvalRecord.setPerformanceRecord(record);
        approvalRecord.setReviewer(reviewer);
        approvalRecord.setDecision(decision);
        approvalRecord.setReason(reason);
        approvalRecordRepository.save(approvalRecord);
    }

    private List<PerformanceRecord> visibleRecords(UserAccount actor) {
        if (canViewGlobal(actor)) {
            return performanceRecordRepository.findAllByOrderByOccurredOnDescCreatedAtDesc();
        }
        if (canViewDepartment(actor)) {
            return performanceRecordRepository.findByDepartmentOrderByOccurredOnDescCreatedAtDesc(actor.getDepartment());
        }
        if (accessService.hasPermission(actor, PermissionCodes.PERFORMANCE_VIEW_SELF)) {
            return performanceRecordRepository.findByOwner_IdOrderByOccurredOnDescCreatedAtDesc(actor.getId());
        }
        throw new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN", "You do not have permission to view performance records.");
    }

    private List<PerformanceRecord> scopedRecords(String scope) {
        UserAccount actor = accessService.currentUser();
        return switch (scope == null ? "personal" : scope) {
            case "department" -> {
                accessService.requireAnyPermission(actor, PermissionCodes.PERFORMANCE_VIEW_DEPARTMENT, PermissionCodes.PERFORMANCE_VIEW_GLOBAL);
                yield performanceRecordRepository.findByDepartmentOrderByOccurredOnDescCreatedAtDesc(actor.getDepartment());
            }
            case "global" -> {
                accessService.requirePermission(actor, PermissionCodes.PERFORMANCE_VIEW_GLOBAL);
                yield performanceRecordRepository.findAllByOrderByOccurredOnDescCreatedAtDesc();
            }
            default -> {
                accessService.requireAnyPermission(actor,
                    PermissionCodes.PERFORMANCE_VIEW_SELF,
                    PermissionCodes.PERFORMANCE_VIEW_DEPARTMENT,
                    PermissionCodes.PERFORMANCE_VIEW_GLOBAL);
                yield performanceRecordRepository.findByOwner_IdOrderByOccurredOnDescCreatedAtDesc(actor.getId());
            }
        };
    }

    private boolean canViewDepartment(UserAccount actor) {
        return canViewGlobal(actor) || accessService.hasPermission(actor, PermissionCodes.PERFORMANCE_VIEW_DEPARTMENT);
    }

    private boolean canViewGlobal(UserAccount actor) {
        return accessService.hasPermission(actor, PermissionCodes.PERFORMANCE_VIEW_GLOBAL);
    }

    private List<PerformanceRecord> forbidden(String message) {
        throw new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN", message);
    }

    private DashboardResponse dashboard(String scope, List<PerformanceRecord> records) {
        BigDecimal totalAmount = records.stream()
            .filter(record -> record.getStatus() == PerformanceStatus.APPROVED)
            .map(PerformanceRecord::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        long pending = records.stream().filter(record -> record.getStatus() == PerformanceStatus.PENDING).count();
        long approved = records.stream().filter(record -> record.getStatus() == PerformanceStatus.APPROVED).count();
        long rejected = records.stream().filter(record -> record.getStatus() == PerformanceStatus.REJECTED).count();

        Map<YearMonth, BigDecimal> monthly = new LinkedHashMap<>();
        records.stream()
            .filter(record -> record.getStatus() == PerformanceStatus.APPROVED)
            .sorted(Comparator.comparing(PerformanceRecord::getOccurredOn))
            .forEach(record -> monthly.merge(YearMonth.from(record.getOccurredOn()), record.getAmount(), BigDecimal::add));

        List<MonthlyStatResponse> monthlyStats = monthly.entrySet().stream()
            .map(entry -> new MonthlyStatResponse(entry.getKey().toString(), entry.getValue()))
            .toList();

        Map<String, BigDecimal> totalsByOwner = new LinkedHashMap<>();
        Map<String, String> departmentByOwner = new LinkedHashMap<>();
        records.stream()
            .filter(record -> record.getStatus() == PerformanceStatus.APPROVED)
            .forEach(record -> {
                totalsByOwner.merge(record.getOwner().getDisplayName(), record.getAmount(), BigDecimal::add);
                departmentByOwner.put(record.getOwner().getDisplayName(), record.getDepartment());
            });

        List<RankingResponse> ranking = totalsByOwner.entrySet().stream()
            .map(entry -> new RankingResponse(entry.getKey(), departmentByOwner.get(entry.getKey()), entry.getValue()))
            .sorted(Comparator.comparing(RankingResponse::totalAmount).reversed())
            .limit(5)
            .toList();

        return new DashboardResponse(scope, totalAmount, records.size(), pending, approved, rejected, monthlyStats, ranking);
    }
}
