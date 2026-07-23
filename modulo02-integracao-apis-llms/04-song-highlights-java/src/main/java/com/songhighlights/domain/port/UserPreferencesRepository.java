package com.songhighlights.domain.port;

import com.songhighlights.domain.model.ConversationSummaryData;

import java.util.Optional;

/**
 * Porta de persistência das preferências consolidadas do usuário. Esconde da
 * aplicação o detalhe de que, na infraestrutura, gêneros/bandas são
 * serializados como JSON em texto.
 */
public interface UserPreferencesRepository {

    Optional<ConversationSummaryData> findByUserId(String userId);

    void save(String userId, ConversationSummaryData preferences);
}
