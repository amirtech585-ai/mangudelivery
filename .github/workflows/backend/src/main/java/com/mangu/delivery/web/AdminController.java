package com.mangu.delivery.web;

import com.mangu.delivery.domain.ApplicationStatus;
import com.mangu.delivery.dto.Dtos.ApplicationView;
import com.mangu.delivery.dto.Dtos.RejectRequest;
import com.mangu.delivery.service.AdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/applications")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService admin;

    @GetMapping
    public List<ApplicationView> list(@RequestParam(defaultValue = "PENDING") ApplicationStatus status) {
        return admin.list(status);
    }

    @PostMapping("/{id}/approve")
    public ApplicationView approve(@AuthenticationPrincipal Long adminId, @PathVariable Long id) {
        return admin.approve(adminId, id);
    }

    @PostMapping("/{id}/reject")
    public ApplicationView reject(@AuthenticationPrincipal Long adminId,
                                  @PathVariable Long id,
                                  @Valid @RequestBody RejectRequest req) {
        return admin.reject(adminId, id, req.reason().trim());
    }
}
