package com.mangu.delivery.service;

import com.mangu.delivery.domain.AccountStatus;
import com.mangu.delivery.domain.ApplicationStatus;
import com.mangu.delivery.domain.AuditLog;
import com.mangu.delivery.domain.PartnerApplication;
import com.mangu.delivery.domain.Role;
import com.mangu.delivery.domain.User;
import com.mangu.delivery.dto.Dtos.ApplicationDetails;
import com.mangu.delivery.dto.Dtos.ApplicationView;
import com.mangu.delivery.dto.Dtos.AuditView;
import com.mangu.delivery.dto.Dtos.UserView;
import com.mangu.delivery.repository.AuditLogRepository;
import com.mangu.delivery.repository.PartnerApplicationRepository;
import com.mangu.delivery.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminService {

    private static final String TARGET_APPLICATION = "PARTNER_APPLICATION";
    private static final String TARGET_USER = "USER";

    private final PartnerApplicationRepository applications;
    private final UserRepository users;
    private final AuditLogRepository auditLogs;

    // ---- applications ----

    @Transactional(readOnly = true)
    public List<ApplicationView> list(ApplicationStatus status) {
        return applications.findByStatusOrderBySubmittedAtAsc(status).stream()
                .map(AdminService::toView)
                .toList();
    }

    @Transactional
    public ApplicationView approve(Long adminId, Long applicationId) {
        PartnerApplication app = requirePending(applicationId);
        User user = app.getUser();
        requireUserStatus(user, AccountStatus.PENDING_APPROVAL);
        app.setStatus(ApplicationStatus.APPROVED);
        app.setReviewedAt(Instant.now());
        app.setReviewedBy(adminId);
        user.setStatus(AccountStatus.ACTIVE);
        audit(adminId, "APPROVE_APPLICATION", TARGET_APPLICATION, applicationId,
                "userId=" + user.getId() + " role=" + app.getRole());
        return toView(app);
    }

    @Transactional
    public ApplicationView reject(Long adminId, Long applicationId, String reason) {
        PartnerApplication app = requirePending(applicationId);
        User user = app.getUser();
        requireUserStatus(user, AccountStatus.PENDING_APPROVAL);
        app.setStatus(ApplicationStatus.REJECTED);
        app.setRejectionReason(reason);
        app.setReviewedAt(Instant.now());
        app.setReviewedBy(adminId);
        user.setStatus(AccountStatus.REJECTED);
        audit(adminId, "REJECT_APPLICATION", TARGET_APPLICATION, applicationId,
                "userId=" + user.getId() + " role=" + app.getRole() + " reason=" + reason);
        return toView(app);
    }

    // ---- accounts ----

    @Transactional(readOnly = true)
    public List<UserView> listUsers() {
        return users.findAllByOrderByIdDesc().stream().map(AdminService::toUserView).toList();
    }

    /** Blocks the account. Existing tokens are refused on the next business request. */
    @Transactional
    public UserView suspend(Long adminId, Long userId, String reason) {
        User user = requireManageableUser(userId);
        if (user.getStatus() == AccountStatus.SUSPENDED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This account is already suspended");
        }
        user.setStatus(AccountStatus.SUSPENDED);
        audit(adminId, "SUSPEND_USER", TARGET_USER, userId, "role=" + user.getRole() + " reason=" + reason);
        return toUserView(user);
    }

    /** Restores the status the account had before suspension, based on its application history. */
    @Transactional
    public UserView reinstate(Long adminId, Long userId) {
        User user = requireManageableUser(userId);
        if (user.getStatus() != AccountStatus.SUSPENDED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This account is not suspended");
        }
        AccountStatus restored = restoredStatus(user);
        user.setStatus(restored);
        audit(adminId, "REINSTATE_USER", TARGET_USER, userId, "restoredTo=" + restored);
        return toUserView(user);
    }

    @Transactional(readOnly = true)
    public List<AuditView> recentAudit() {
        return auditLogs.findTop200ByOrderByCreatedAtDesc().stream().map(AdminService::toAuditView).toList();
    }

    // ---- helpers ----

    private AccountStatus restoredStatus(User user) {
        if (user.getRole() == Role.BUYER) {
            return AccountStatus.ACTIVE;
        }
        return applications.findFirstByUserIdOrderBySubmittedAtDesc(user.getId())
                .map(app -> switch (app.getStatus()) {
                    case APPROVED -> AccountStatus.ACTIVE;
                    case REJECTED -> AccountStatus.REJECTED;
                    case PENDING -> AccountStatus.PENDING_APPROVAL;
                })
                .orElse(AccountStatus.PENDING_APPROVAL);
    }

    private User requireManageableUser(Long userId) {
        User user = users.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found"));
        if (user.getRole() == Role.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Administrator accounts cannot be changed here");
        }
        return user;
    }

    private static void requireUserStatus(User user, AccountStatus expected) {
        if (user.getStatus() != expected) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "This account is not awaiting a decision (status: " + user.getStatus() + ")");
        }
    }

    private PartnerApplication requirePending(Long applicationId) {
        PartnerApplication app = applications.findById(applicationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Application not found"));
        if (app.getStatus() != ApplicationStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Application has already been reviewed");
        }
        return app;
    }

    private void audit(Long actorId, String action, String targetType, Long targetId, String details) {
        AuditLog entry = new AuditLog();
        entry.setActorId(actorId);
        entry.setAction(action);
        entry.setTargetType(targetType);
        entry.setTargetId(targetId);
        entry.setDetails(details);
        auditLogs.save(entry);
    }

    private static ApplicationView toView(PartnerApplication a) {
        User u = a.getUser();
        ApplicationDetails d = new ApplicationDetails(
                a.getBusinessName(),
                a.getProductCategories(),
                a.getNationalId(),
                a.getLicenceNumber(),
                a.getVehicleType(),
                a.getVehicleRegistration(),
                a.getLocation());
        return new ApplicationView(
                a.getId(), u.getId(), u.getFullName(), u.getEmail(), u.getPhone(),
                a.getRole(), a.getStatus(), d, a.getRejectionReason(), a.getSubmittedAt());
    }

    private static UserView toUserView(User u) {
        return new UserView(u.getId(), u.getEmail(), u.getFullName(), u.getRole(), u.getStatus(), u.getCreatedAt());
    }

    private static AuditView toAuditView(AuditLog a) {
        return new AuditView(a.getId(), a.getActorId(), a.getAction(), a.getTargetType(),
                a.getTargetId(), a.getDetails(), a.getCreatedAt());
    }
}
