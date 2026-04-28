package com.klu.repo;

import com.klu.entity.BoothApplication;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BoothApplicationRepository extends JpaRepository<BoothApplication, Long> {
    boolean existsByBoothIdAndUserId(Long boothId, Long userId);
    Optional<BoothApplication> findByBoothIdAndUserId(Long boothId, Long userId);
}
