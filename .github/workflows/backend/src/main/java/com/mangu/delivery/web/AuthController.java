package com.mangu.delivery.web;

import com.mangu.delivery.dto.Dtos.AuthResponse;
import com.mangu.delivery.dto.Dtos.LoginRequest;
import com.mangu.delivery.dto.Dtos.MeResponse;
import com.mangu.delivery.dto.Dtos.RegisterRequest;
import com.mangu.delivery.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class AuthController {

    private final AuthService auth;

    @PostMapping("/api/auth/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@Valid @RequestBody RegisterRequest req) {
        return auth.register(req);
    }

    @PostMapping("/api/auth/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest req) {
        return auth.login(req);
    }

    /**
     * Tokens are stateless. The app discards its token; server-side revocation
     * is listed as a future item in docs/DECISIONS.md.
     */
    @PostMapping("/api/auth/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout() {
    }

    @GetMapping("/api/me")
    public MeResponse me(@AuthenticationPrincipal Long userId) {
        return auth.me(userId);
    }
}
