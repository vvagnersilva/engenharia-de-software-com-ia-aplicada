package com.songhighlights.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserPreferencesJpaRepository extends JpaRepository<UserPreferencesJpaEntity, Long> {

    Optional<UserPreferencesJpaEntity> findByUserId(String userId);
}
