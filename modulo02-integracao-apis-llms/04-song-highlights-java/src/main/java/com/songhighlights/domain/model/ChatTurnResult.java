package com.songhighlights.domain.model;

public record ChatTurnResult(
        String userId,
        String reply,
        boolean preferencesUpdated,
        boolean conversationSummarized
) {
}
