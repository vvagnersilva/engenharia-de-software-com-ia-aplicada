package com.songhighlights.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ConversationMessageJpaRepository extends JpaRepository<ConversationMessageJpaEntity, Long> {

    List<ConversationMessageJpaEntity> findByUserIdOrderByIdAsc(String userId);

    long countByUserId(String userId);
}
