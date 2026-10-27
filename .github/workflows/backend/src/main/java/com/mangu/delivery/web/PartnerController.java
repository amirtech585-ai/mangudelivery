package com.mangu.delivery.web;

import com.mangu.delivery.dto.Dtos.ApplicationDetails;
import com.mangu.delivery.dto.Dtos.MeResponse;
import com.mangu.delivery.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/partner")
@RequiredArgsConstructor
public class PartnerController {

    private final AuthService auth;

    /** Seller or rider (re)submits an application for admin review. */
    @PostMapping("/application")
    @ResponseStatus(HttpStatus.CREATED)
    public MeResponse submit(@AuthenticationPrincipal Long userId, @RequestBody ApplicationDetails details) {
        return auth.submitApplication(userId, details);
    }
}
