package com.jwtcenter.controller;

import com.jwtcenter.dto.common.ApiResponse;
import com.jwtcenter.dto.common.OperationLogResponse;
import com.jwtcenter.service.AccessService;
import com.jwtcenter.service.OperationLogService;
import com.jwtcenter.security.PermissionCodes;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/logs")
public class OperationLogController {

    private final OperationLogService operationLogService;
    private final AccessService accessService;

    public OperationLogController(OperationLogService operationLogService, AccessService accessService) {
        this.operationLogService = operationLogService;
        this.accessService = accessService;
    }

    @GetMapping
    public ApiResponse<List<OperationLogResponse>> logs(@RequestParam(defaultValue = "50") int limit) {
        accessService.requirePermission(accessService.currentUser(), PermissionCodes.LOG_VIEW);
        return ApiResponse.success("Operation logs loaded.", operationLogService.recentLogs(Math.min(limit, 200)));
    }
}
