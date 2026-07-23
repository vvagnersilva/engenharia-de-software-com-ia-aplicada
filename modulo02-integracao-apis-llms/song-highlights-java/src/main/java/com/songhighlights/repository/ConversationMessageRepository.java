package com.songhighlights.repository;

import com.songhighlights.entity.ConversationMessageEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ConversationMessageRepository extends JpaRepository<ConversationMessageEntity, Long> {

    List<ConversationMessageEntity> findByUserIdOrderByIdAsc(String userId);

    long countByUserId(String userId);
}
