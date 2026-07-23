package com.songhighlights.dto;

public record ChatApiResponse(
        String userId,
        String reply,
        boolean preferencesUpdated,
        boolean conversationSummarized
) {
}
