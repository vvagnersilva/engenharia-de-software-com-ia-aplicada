package com.songhighlights.domain.port;

import com.songhighlights.domain.model.ConversationMessage;

import java.util.List;

/**
 * Porta de persistência do histórico de conversa. A infraestrutura decide
 * como armazenar (hoje: JPA/Postgres); o domínio e a aplicação só conhecem
 * ConversationMessage.
 */
public interface ConversationRepository {

    List<ConversationMessage> findByUserIdOrderByIdAsc(String userId);

    long countByUserId(String userId);

    void save(ConversationMessage message);

    void deleteAllById(List<Long> ids);
}
