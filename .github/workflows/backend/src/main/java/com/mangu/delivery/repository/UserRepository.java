package com.mangu.delivery.repository;

import com.mangu.delivery.domain.Role;
import com.mangu.delivery.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmailAndRole(String email, Role role);

    boolean existsByEmailAndRole(String email, Role role);

    List<User> findAllByOrderByIdDesc();
}
