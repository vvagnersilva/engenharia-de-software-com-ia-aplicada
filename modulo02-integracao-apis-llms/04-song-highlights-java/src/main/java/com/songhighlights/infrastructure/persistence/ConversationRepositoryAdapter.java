package com.songhighlights.infrastructure.persistence;

import com.songhighlights.domain.model.ConversationMessage;
import com.songhighlights.domain.port.ConversationRepository;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Implementação de ConversationRepository sobre Spring Data JPA. Traduz entre
 * o modelo de domínio (ConversationMessage) e a entidade JPA
 * (ConversationMessageJpaEntity) - o resto da aplicação nunca vê a entidade.
 */
@Component
public class ConversationRepositoryAdapter implements ConversationRepository {

    private final ConversationMessageJpaRepository jpaRepository;

    public ConversationRepositoryAdapter(ConversationMessageJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public List<ConversationMessage> findByUserIdOrderByIdAsc(String userId) {
        return jpaRepository.findByUserIdOrderByIdAsc(userId).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public long countByUserId(String userId) {
        return jpaRepository.countByUserId(userId);
    }

    @Override
    public void save(ConversationMessage message) {
        jpaRepository.save(new ConversationMessageJpaEntity(
                message.userId(), message.role(), message.content(), message.createdAt()
        ));
    }

    @Override
    public void deleteAllById(List<Long> ids) {
        jpaRepository.deleteAllById(ids);
    }

    private ConversationMessage toDomain(ConversationMessageJpaEntity entity) {
        return new ConversationMessage(
                entity.getId(), entity.getUserId(), entity.getRole(), entity.getContent(), entity.getCreatedAt()
        );
    }
}
