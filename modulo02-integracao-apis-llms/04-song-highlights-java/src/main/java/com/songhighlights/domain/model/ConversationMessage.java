package com.songhighlights.domain.model;

import java.time.Instant;

/**
 * Mensagem de uma conversa (usuário ou IA). Modelo de domínio puro - a forma
 * como é persistida (tabela, JSON, etc.) é responsabilidade da camada de
 * infraestrutura.
 */
public record ConversationMessage(Long id, String userId, String role, String content, Instant createdAt) {

    public static final String ROLE_USER = "user";
    public static final String ROLE_AI = "ai";

    public static ConversationMessage newMessage(String userId, String role, String content) {
        return new ConversationMessage(null, userId, role, content, Instant.now());
    }
}
