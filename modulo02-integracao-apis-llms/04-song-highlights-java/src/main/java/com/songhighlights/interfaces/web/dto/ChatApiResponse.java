package com.songhighlights.interfaces.web.dto;

public record ChatApiResponse(
        String userId,
        String reply,
        boolean preferencesUpdated,
        boolean conversationSummarized
) {
}
