package com.jwtcenter.controller;

import com.jwtcenter.dto.common.ApiResponse;
import com.jwtcenter.dto.portal.PortalAppResponse;
import com.jwtcenter.dto.portal.SsoAuthorizeResponse;
import com.jwtcenter.dto.portal.SsoTicketVerifyResponse;
import com.jwtcenter.service.PortalService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/portal")
public class PortalController {

    private final PortalService portalService;

    public PortalController(PortalService portalService) {
        this.portalService = portalService;
    }

    @GetMapping("/apps")
    public ApiResponse<List<PortalAppResponse>> apps() {
        return ApiResponse.success("Authorized business systems loaded.", portalService.visibleApps());
    }

    @PostMapping("/apps/{appKey}/authorize")
    public ApiResponse<SsoAuthorizeResponse> authorize(@PathVariable String appKey) {
        return ApiResponse.success("Business system access authorized.", portalService.authorize(appKey));
    }

    @PostMapping("/sso/tickets/{ticket}/verify")
    public ApiResponse<SsoTicketVerifyResponse> verifyTicket(@PathVariable String ticket) {
        return ApiResponse.success("SSO ticket verified.", portalService.verifyTicket(ticket));
    }
}
