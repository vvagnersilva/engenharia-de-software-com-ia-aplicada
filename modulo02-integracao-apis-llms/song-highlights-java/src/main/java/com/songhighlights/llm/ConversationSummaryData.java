package com.songhighlights.llm;

import java.util.List;

/**
 * Sumário condensado de uma conversa, e também a forma como as preferências
 * ficam persistidas para um usuário. Equivalente ao SummarySchema (Zod) em
 * summarization.ts / ConversationSummary do projeto TypeScript original.
 */
public record ConversationSummaryData(
        String name,
        Integer age,
        List<String> favoriteGenres,
        List<String> favoriteBands,
        String keyPreferences,
        String importantContext
) {
}
