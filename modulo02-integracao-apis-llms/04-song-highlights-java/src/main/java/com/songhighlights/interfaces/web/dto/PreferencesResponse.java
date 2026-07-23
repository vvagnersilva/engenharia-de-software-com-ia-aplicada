package com.songhighlights.interfaces.web.dto;

import com.songhighlights.domain.model.ConversationSummaryData;

import java.util.List;

public record PreferencesResponse(
        String userId,
        String name,
        Integer age,
        List<String> favoriteGenres,
        List<String> favoriteBands,
        String keyPreferences,
        String importantContext
) {
    public static PreferencesResponse from(String userId, ConversationSummaryData summary) {
        return new PreferencesResponse(
                userId,
                summary.name(),
                summary.age(),
                summary.favoriteGenres(),
                summary.favoriteBands(),
                summary.keyPreferences(),
                summary.importantContext()
        );
    }
}
