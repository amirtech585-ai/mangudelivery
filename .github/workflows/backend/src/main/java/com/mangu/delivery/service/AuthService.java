package com.mangu.delivery.service;

import com.mangu.delivery.domain.AccountStatus;
import com.mangu.delivery.domain.PartnerApplication;
import com.mangu.delivery.domain.Role;
import com.mangu.delivery.domain.User;
import com.mangu.delivery.dto.Dtos.ApplicationDetails;
import com.mangu.delivery.dto.Dtos.AuthResponse;
import com.mangu.delivery.dto.Dtos.LoginRequest;
import com.mangu.delivery.dto.Dtos.MeResponse;
import com.mangu.delivery.dto.Dtos.RegisterRequest;
import com.mangu.delivery.repository.PartnerApplicationRepository;
import com.mangu.delivery.repository.UserRepository;
import com.mangu.delivery.security.JwtService;
import com.mangu.delivery.security.LoginRateLimiter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final List<Role> PARTNER_ROLES = List.of(Role.SELLER, Role.RIDER);

    private final UserRepository users;
    private final PartnerApplicationRepository applications;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final LoginRateLimiter loginLimiter;

    /**
     * Creates one account for one role. The same email may register again under a
     * different role (for example, a seller creating a buyer account).
     */
    @Transactional
    public AuthResponse register(RegisterRequest req) {
        if (req.role() == Role.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin accounts cannot be self-registered");
        }
        String email = normalise(req.email());
        if (users.existsByEmailAndRole(email, req.role())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "We could not create this account. If you already registered for this role, log in instead.");
        }

        boolean partner = PARTNER_ROLES.contains(req.role());
        if (partner) {
            validateApplication(req.role(), req.application());
        }

        User user = new User();
        user.setEmail(email);
        user.setPhone(req.phone());
        user.setFullName(req.fullName().trim());
        user.setPasswordHash(passwordEncoder.encode(req.password()));
        user.setRole(req.role());
        user.setStatus(partner ? AccountStatus.PENDING_APPROVAL : AccountStatus.ACTIVE);
        users.save(user);

        if (partner) {
            applications.save(buildApplication(user, req.role(), req.application()));
        }
        return authResponse(user);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest req) {
        loginLimiter.check(req.role() + ":" + normalise(req.email()));
        User user = users.findByEmailAndRole(normalise(req.email()), req.role())
                .orElseThrow(AuthService::invalidCredentials);
        if (!passwordEncoder.matches(req.password(), user.getPasswordHash())) {
            throw invalidCredentials();
        }
        if (user.getStatus() == AccountStatus.SUSPENDED) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This account is suspended");
        }
        return authResponse(user);
    }

    /** Seller or rider resubmits after rejection (or submits for the first time). */
    @Transactional
    public MeResponse submitApplication(Long userId, ApplicationDetails details) {
        User user = requireUser(userId);
        if (!PARTNER_ROLES.contains(user.getRole())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only sellers and riders apply for approval");
        }
        switch (user.getStatus()) {
            case PENDING_APPROVAL -> throw new ResponseStatusException(HttpStatus.CONFLICT, "An application is already under review");
            case ACTIVE -> throw new ResponseStatusException(HttpStatus.CONFLICT, "This account is already approved");
            case SUSPENDED -> throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This account is suspended");
            case REJECTED -> { /* allowed: resubmission */ }
        }
        validateApplication(user.getRole(), details);
        applications.save(buildApplication(user, user.getRole(), details));
        user.setStatus(AccountStatus.PENDING_APPROVAL);
        return toMe(user);
    }

    @Transactional(readOnly = true)
    public MeResponse me(Long userId) {
        return toMe(requireUser(userId));
    }

    /** Used by every business endpoint: the database status is the source of truth. */
    @Transactional(readOnly = true)
    public User requireActive(Long userId) {
        User user = requireUser(userId);
        if (user.getStatus() != AccountStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Account is not active (" + user.getStatus() + ")");
        }
        return user;
    }

    @Transactional(readOnly = true)
    public User requireUser(Long userId) {
        return users.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unknown account"));
    }

    // ---- helpers ----

    private AuthResponse authResponse(User user) {
        return new AuthResponse(jwtService.issue(user.getId(), user.getRole()),
                user.getId(), user.getRole(), user.getStatus());
    }

    private MeResponse toMe(User user) {
        PartnerApplication latest = applications
                .findFirstByUserIdOrderBySubmittedAtDesc(user.getId()).orElse(null);
        return new MeResponse(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getRole(),
                user.getStatus(),
                latest == null ? null : latest.getStatus().name(),
                latest == null ? null : latest.getRejectionReason());
    }

    private static PartnerApplication buildApplication(User user, Role role, ApplicationDetails d) {
        PartnerApplication app = new PartnerApplication();
        app.setUser(user);
        app.setRole(role);
        app.setBusinessName(d.businessName());
        app.setProductCategories(d.productCategories());
        app.setNationalId(d.nationalId());
        app.setLicenceNumber(d.licenceNumber());
        app.setVehicleType(d.vehicleType());
        app.setVehicleRegistration(d.vehicleRegistration());
        app.setLocation(d.location());
        return app;
    }

    private static void validateApplication(Role role, ApplicationDetails d) {
        if (d == null) {
            throw badRequest("Application details are required");
        }
        if (role == Role.SELLER) {
            requireText(d.businessName(), "businessName");
            requireText(d.location(), "location");
            requireText(d.productCategories(), "productCategories");
        } else {
            requireText(d.nationalId(), "nationalId");
            requireText(d.licenceNumber(), "licenceNumber");
            requireText(d.vehicleType(), "vehicleType");
            requireText(d.vehicleRegistration(), "vehicleRegistration");
            requireText(d.location(), "location");
        }
    }

    private static void requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw badRequest(field + " is required");
        }
    }

    private static ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }

    private static ResponseStatusException invalidCredentials() {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email, password, or role");
    }

    private static String normalise(String email) {
        return email.trim().toLowerCase();
    }
}
