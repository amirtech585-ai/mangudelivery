package com.mangu.delivery.web;

import com.mangu.delivery.dto.Dtos.AuditView;
import com.mangu.delivery.dto.Dtos.SuspendRequest;
import com.mangu.delivery.dto.Dtos.UserView;
import com.mangu.delivery.service.AdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Account management and audit log for the administrator. Requires ROLE_ADMIN (see SecurityConfig). */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminUserController {

    private final AdminService admin;

    @GetMapping("/users")
    public List<UserView> users() {
        return admin.listUsers();
    }

    @PostMapping("/users/{id}/suspend")
    public UserView suspend(@AuthenticationPrincipal Long adminId,
                            @PathVariable Long id,
                            @Valid @RequestBody SuspendRequest req) {
        return admin.suspend(adminId, id, req.reason().trim());
    }

    @PostMapping("/users/{id}/reinstate")
    public UserView reinstate(@AuthenticationPrincipal Long adminId, @PathVariable Long id) {
        return admin.reinstate(adminId, id);
    }

    @GetMapping("/audit")
    public List<AuditView> audit() {
        return admin.recentAudit();
    }
}
