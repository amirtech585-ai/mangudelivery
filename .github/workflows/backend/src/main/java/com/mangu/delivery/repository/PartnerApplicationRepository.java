package com.mangu.delivery.repository;

import com.mangu.delivery.domain.ApplicationStatus;
import com.mangu.delivery.domain.PartnerApplication;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PartnerApplicationRepository extends JpaRepository<PartnerApplication, Long> {

    List<PartnerApplication> findByStatusOrderBySubmittedAtAsc(ApplicationStatus status);

    Optional<PartnerApplication> findFirstByUserIdOrderBySubmittedAtDesc(Long userId);
}
