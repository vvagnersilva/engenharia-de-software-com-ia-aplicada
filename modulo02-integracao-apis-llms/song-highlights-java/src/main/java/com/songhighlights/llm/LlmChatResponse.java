package com.songhighlights.llm;

/**
 * Resposta estruturada do LLM para um turno de chat.
 * Equivalente ao ChatResponseSchema (Zod) em chatResponse.ts do projeto TypeScript original.
 */
public record LlmChatResponse(
        String message,
        UserPreferencesData preferences,
        boolean shouldSavePreferences
) {
}
