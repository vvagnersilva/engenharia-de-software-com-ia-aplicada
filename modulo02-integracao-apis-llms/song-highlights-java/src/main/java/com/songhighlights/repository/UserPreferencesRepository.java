package com.songhighlights.repository;

import com.songhighlights.entity.UserPreferencesEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserPreferencesRepository extends JpaRepository<UserPreferencesEntity, Long> {

    Optional<UserPreferencesEntity> findByUserId(String userId);
}
