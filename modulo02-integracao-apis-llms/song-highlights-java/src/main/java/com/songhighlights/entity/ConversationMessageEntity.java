package com.songhighlights.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * Substitui o checkpointer do LangGraph (Postgres, indexado por thread_id) do
 * projeto TypeScript original: aqui o histórico de conversa é uma tabela
 * simples indexada por userId, já que cada turno agora é uma chamada REST
 * isolada em vez de um processo de CLI de longa duração.
 */
@Entity
@Table(name = "conversation_messages")
public class ConversationMessageEntity {

    public static final String ROLE_USER = "user";
    public static final String ROLE_AI = "ai";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(nullable = false)
    private String role;

    @Column(columnDefinition = "text", nullable = false)
    private String content;

    @Column(name = "created_at")
    private Instant createdAt;

    public ConversationMessageEntity() {
    }

    public ConversationMessageEntity(String userId, String role, String content) {
        this.userId = userId;
        this.role = role;
        this.content = content;
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getUserId() {
        return userId;
    }

    public String getRole() {
        return role;
    }

    public String getContent() {
        return content;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
