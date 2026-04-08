package com.jwtcenter.controller;

import com.jwtcenter.dto.common.ApiResponse;
import com.jwtcenter.dto.token.TokenInspectRequest;
import com.jwtcenter.dto.token.TokenInspectResponse;
import com.jwtcenter.service.TokenInspectionService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/tokens")
public class TokenController {

    private final TokenInspectionService tokenInspectionService;

    public TokenController(TokenInspectionService tokenInspectionService) {
        this.tokenInspectionService = tokenInspectionService;
    }

    @PostMapping("/validate")
    public ApiResponse<TokenInspectResponse> validate(@Valid @RequestBody TokenInspectRequest request) {
        return ApiResponse.success("Token inspected.", tokenInspectionService.inspect(request.token()));
    }

    @PostMapping("/parse")
    public ApiResponse<TokenInspectResponse> parse(@Valid @RequestBody TokenInspectRequest request) {
        return ApiResponse.success("Token parsed.", tokenInspectionService.inspect(request.token()));
    }
}
