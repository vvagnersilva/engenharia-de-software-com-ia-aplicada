package com.songhighlights.service;

public record ChatTurnResult(
        String userId,
        String reply,
        boolean preferencesUpdated,
        boolean conversationSummarized
) {
}
