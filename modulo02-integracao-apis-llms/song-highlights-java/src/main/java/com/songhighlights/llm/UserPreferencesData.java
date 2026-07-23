package com.songhighlights.llm;

import java.util.List;

/**
 * Preferências extraídas de UMA mensagem do usuário pelo LLM.
 * Equivalente ao UserPreferencesSchema (Zod) em chatResponse.ts do projeto TypeScript original.
 */
public record UserPreferencesData(
        String name,
        Integer age,
        List<String> favoriteGenres,
        List<String> favoriteBands,
        String mood,
        String listeningContext,
        String additionalInfo
) {
}
