package com.jwtcenter.controller;

import com.jwtcenter.dto.common.ApiResponse;
import com.jwtcenter.dto.performance.ApprovalDecisionRequest;
import com.jwtcenter.dto.performance.DashboardResponse;
import com.jwtcenter.dto.performance.MonthlyStatResponse;
import com.jwtcenter.dto.performance.PerformanceRecordRequest;
import com.jwtcenter.dto.performance.PerformanceRecordResponse;
import com.jwtcenter.dto.performance.RankingResponse;
import com.jwtcenter.service.PerformanceService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/performance")
public class PerformanceController {

    private final PerformanceService performanceService;

    public PerformanceController(PerformanceService performanceService) {
        this.performanceService = performanceService;
    }

    @GetMapping("/records")
    public ApiResponse<List<PerformanceRecordResponse>> listRecords() {
        return ApiResponse.success("Performance records loaded.", performanceService.listRecords());
    }

    @PostMapping("/records")
    public ApiResponse<PerformanceRecordResponse> createRecord(@Valid @RequestBody PerformanceRecordRequest request) {
        return ApiResponse.success("Performance record created.", performanceService.createRecord(request));
    }

    @PutMapping("/records/{recordId}")
    public ApiResponse<PerformanceRecordResponse> updateRecord(@PathVariable Long recordId, @Valid @RequestBody PerformanceRecordRequest request) {
        return ApiResponse.success("Performance record updated.", performanceService.updateRecord(recordId, request));
    }

    @DeleteMapping("/records/{recordId}")
    public ApiResponse<Void> deleteRecord(@PathVariable Long recordId) {
        performanceService.deleteRecord(recordId);
        return ApiResponse.success("Performance record deleted.", null);
    }

    @GetMapping("/approvals/pending")
    public ApiResponse<List<PerformanceRecordResponse>> pendingApprovals() {
        return ApiResponse.success("Pending approvals loaded.", performanceService.pendingApprovals());
    }

    @PostMapping("/approvals/{recordId}/approve")
    public ApiResponse<PerformanceRecordResponse> approve(@PathVariable Long recordId) {
        return ApiResponse.success("Performance record approved.", performanceService.approve(recordId));
    }

    @PostMapping("/approvals/{recordId}/reject")
    public ApiResponse<PerformanceRecordResponse> reject(@PathVariable Long recordId, @Valid @RequestBody ApprovalDecisionRequest request) {
        return ApiResponse.success("Performance record rejected.", performanceService.reject(recordId, request));
    }

    @GetMapping("/dashboard/personal")
    public ApiResponse<DashboardResponse> personalDashboard() {
        return ApiResponse.success("Personal dashboard loaded.", performanceService.personalDashboard());
    }

    @GetMapping("/dashboard/department")
    public ApiResponse<DashboardResponse> departmentDashboard() {
        return ApiResponse.success("Department dashboard loaded.", performanceService.departmentDashboard());
    }

    @GetMapping("/dashboard/global")
    public ApiResponse<DashboardResponse> globalDashboard() {
        return ApiResponse.success("Global dashboard loaded.", performanceService.globalDashboard());
    }

    @GetMapping("/statistics/monthly")
    public ApiResponse<List<MonthlyStatResponse>> monthlyStatistics(@RequestParam(defaultValue = "personal") String scope) {
        return ApiResponse.success("Monthly statistics loaded.", performanceService.monthlyStatistics(scope));
    }

    @GetMapping("/statistics/ranking")
    public ApiResponse<List<RankingResponse>> ranking(@RequestParam(defaultValue = "personal") String scope) {
        return ApiResponse.success("Ranking loaded.", performanceService.ranking(scope));
    }
}
