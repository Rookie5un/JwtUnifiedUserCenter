package com.jwtcenter.service;

import com.jwtcenter.dto.department.DepartmentRequest;
import com.jwtcenter.dto.department.DepartmentResponse;
import com.jwtcenter.entity.Department;
import com.jwtcenter.entity.PerformanceRecord;
import com.jwtcenter.entity.UserAccount;
import com.jwtcenter.enums.OperationResult;
import com.jwtcenter.exception.ApiException;
import com.jwtcenter.repository.DepartmentRepository;
import com.jwtcenter.repository.PerformanceRecordRepository;
import com.jwtcenter.repository.UserRepository;
import com.jwtcenter.security.PermissionCodes;
import com.jwtcenter.util.MapperUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class DepartmentService {

    private final DepartmentRepository departmentRepository;
    private final UserRepository userRepository;
    private final PerformanceRecordRepository performanceRecordRepository;
    private final AccessService accessService;
    private final OperationLogService operationLogService;

    public DepartmentService(
        DepartmentRepository departmentRepository,
        UserRepository userRepository,
        PerformanceRecordRepository performanceRecordRepository,
        AccessService accessService,
        OperationLogService operationLogService
    ) {
        this.departmentRepository = departmentRepository;
        this.userRepository = userRepository;
        this.performanceRecordRepository = performanceRecordRepository;
        this.accessService = accessService;
        this.operationLogService = operationLogService;
    }

    @Transactional(readOnly = true)
    public List<DepartmentResponse> publicDepartments() {
        return departmentRepository.findAllByOrderByNameAsc().stream().map(MapperUtils::toDepartmentResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<DepartmentResponse> listDepartments() {
        accessService.requirePermission(accessService.currentUser(), PermissionCodes.USER_MANAGE);
        return publicDepartments();
    }

    @Transactional
    public DepartmentResponse createDepartment(DepartmentRequest request) {
        UserAccount actor = accessService.currentUser();
        accessService.requirePermission(actor, PermissionCodes.USER_MANAGE);
        String name = normalizedName(request.name());
        if (departmentRepository.existsByName(name)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "DEPARTMENT_EXISTS", "Department already exists.");
        }

        Department department = new Department();
        department.setName(name);
        department.setDescription(normalizedDescription(request.description()));
        Department saved = departmentRepository.save(department);
        operationLogService.log(actor, "CREATE_DEPARTMENT", "DEPARTMENT", String.valueOf(saved.getId()), OperationResult.SUCCESS, "Department created.");
        return MapperUtils.toDepartmentResponse(saved);
    }

    @Transactional
    public DepartmentResponse updateDepartment(Long departmentId, DepartmentRequest request) {
        UserAccount actor = accessService.currentUser();
        accessService.requirePermission(actor, PermissionCodes.USER_MANAGE);
        Department department = findDepartment(departmentId);
        String nextName = normalizedName(request.name());
        String currentName = department.getName();

        departmentRepository.findByName(nextName)
            .filter(existing -> !existing.getId().equals(departmentId))
            .ifPresent(existing -> {
                throw new ApiException(HttpStatus.BAD_REQUEST, "DEPARTMENT_EXISTS", "Department already exists.");
            });

        if (!currentName.equals(nextName)) {
            userRepository.findAllByDepartment(currentName).forEach(user -> user.setDepartment(nextName));
            performanceRecordRepository.findByDepartment(currentName).forEach(record -> record.setDepartment(nextName));
        }

        department.setName(nextName);
        department.setDescription(normalizedDescription(request.description()));
        Department saved = departmentRepository.save(department);
        operationLogService.log(actor, "UPDATE_DEPARTMENT", "DEPARTMENT", String.valueOf(departmentId), OperationResult.SUCCESS, "Department updated.");
        return MapperUtils.toDepartmentResponse(saved);
    }

    @Transactional
    public void deleteDepartment(Long departmentId) {
        UserAccount actor = accessService.currentUser();
        accessService.requirePermission(actor, PermissionCodes.USER_MANAGE);
        Department department = findDepartment(departmentId);
        String name = department.getName();
        if (userRepository.countByDepartmentAndDeletedAtIsNull(name) > 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "DEPARTMENT_IN_USE", "Department is still assigned to active users.");
        }
        if (performanceRecordRepository.countByDepartment(name) > 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "DEPARTMENT_IN_USE", "Department is still referenced by performance records.");
        }
        departmentRepository.delete(department);
        operationLogService.log(actor, "DELETE_DEPARTMENT", "DEPARTMENT", String.valueOf(departmentId), OperationResult.SUCCESS, "Department deleted.");
    }

    @Transactional(readOnly = true)
    public void requireExistingDepartment(String departmentName) {
        String normalized = normalizedName(departmentName);
        if (!departmentRepository.existsByName(normalized)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "DEPARTMENT_NOT_FOUND", "Department does not exist.");
        }
    }

    private Department findDepartment(Long departmentId) {
        return departmentRepository.findById(departmentId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "DEPARTMENT_NOT_FOUND", "Department not found."));
    }

    private String normalizedName(String value) {
        return value == null ? null : value.trim();
    }

    private String normalizedDescription(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
