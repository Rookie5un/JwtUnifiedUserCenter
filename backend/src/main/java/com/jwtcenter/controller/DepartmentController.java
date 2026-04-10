package com.jwtcenter.controller;

import com.jwtcenter.dto.common.ApiResponse;
import com.jwtcenter.dto.department.DepartmentRequest;
import com.jwtcenter.dto.department.DepartmentResponse;
import com.jwtcenter.service.DepartmentService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/departments")
public class DepartmentController {

    private final DepartmentService departmentService;

    public DepartmentController(DepartmentService departmentService) {
        this.departmentService = departmentService;
    }

    @GetMapping("/public")
    public ApiResponse<List<DepartmentResponse>> publicDepartments() {
        return ApiResponse.success("Departments loaded.", departmentService.publicDepartments());
    }

    @GetMapping
    public ApiResponse<List<DepartmentResponse>> listDepartments() {
        return ApiResponse.success("Departments loaded.", departmentService.listDepartments());
    }

    @PostMapping
    public ApiResponse<DepartmentResponse> createDepartment(@Valid @RequestBody DepartmentRequest request) {
        return ApiResponse.success("Department created.", departmentService.createDepartment(request));
    }

    @PutMapping("/{departmentId}")
    public ApiResponse<DepartmentResponse> updateDepartment(@PathVariable Long departmentId, @Valid @RequestBody DepartmentRequest request) {
        return ApiResponse.success("Department updated.", departmentService.updateDepartment(departmentId, request));
    }

    @DeleteMapping("/{departmentId}")
    public ApiResponse<Void> deleteDepartment(@PathVariable Long departmentId) {
        departmentService.deleteDepartment(departmentId);
        return ApiResponse.success("Department deleted.", null);
    }
}
