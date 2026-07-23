package com.songhighlights.service;

import com.songhighlights.entity.ConversationMessageEntity;
import com.songhighlights.llm.ConversationSummaryData;
import com.songhighlights.llm.StructuredChatClient;
import com.songhighlights.prompt.SummarizationPrompts;
import com.songhighlights.repository.ConversationMessageRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Porte de src/graph/nodes/summarizationNode.ts: condensa o histórico da
 * conversa em um sumário estruturado, persiste-o em user_preferences (via
 * PreferencesService.storeSummary) e reduz o histórico de mensagens às
 * últimas 2 - equivalente ao RemoveMessage aplicado no projeto original.
 */
@Service
public class SummarizationService {

    private static final Logger log = LoggerFactory.getLogger(SummarizationService.class);
    private static final int MESSAGES_TO_KEEP_AFTER_SUMMARY = 2;

    private final StructuredChatClient structuredChatClient;
    private final PreferencesService preferencesService;
    private final ConversationMessageRepository conversationMessageRepository;

    public SummarizationService(
            StructuredChatClient structuredChatClient,
            PreferencesService preferencesService,
            ConversationMessageRepository conversationMessageRepository
    ) {
        this.structuredChatClient = structuredChatClient;
        this.preferencesService = preferencesService;
        this.conversationMessageRepository = conversationMessageRepository;
    }

    public boolean summarize(String userId) {
        List<ConversationMessageEntity> messages = conversationMessageRepository.findByUserIdOrderByIdAsc(userId);

        List<Map<String, String>> conversationHistory = messages.stream()
                .map(msg -> {
                    Map<String, String> entry = new LinkedHashMap<>();
                    entry.put("role", ConversationMessageEntity.ROLE_USER.equals(msg.getRole()) ? "User" : "AI");
                    entry.put("content", msg.getContent());
                    return entry;
                })
                .toList();

        ConversationSummaryData previousSummary = preferencesService.getSummary(userId).orElse(null);

        String systemPrompt = SummarizationPrompts.getSummarizationSystemPrompt();
        String userPrompt = SummarizationPrompts.getSummarizationUserPrompt(conversationHistory, previousSummary);

        ConversationSummaryData summary = generateWithRetry(userId, systemPrompt, userPrompt);
        if (summary == null) {
            return false;
        }

        preferencesService.storeSummary(userId, summary);
        trimHistory(userId, messages);

        return true;
    }

    private void trimHistory(String userId, List<ConversationMessageEntity> messages) {
        if (messages.size() <= MESSAGES_TO_KEEP_AFTER_SUMMARY) {
            return;
        }
        List<ConversationMessageEntity> toDelete = messages.subList(0, messages.size() - MESSAGES_TO_KEEP_AFTER_SUMMARY);
        conversationMessageRepository.deleteAll(toDelete);
    }

    /**
     * Modelos gratuitos ocasionalmente "gaguejam" e devolvem JSON malformado,
     * o que quebra o parser estrito do BeanOutputConverter. Uma segunda
     * tentativa costuma resolver, já que é uma falha de amostragem do modelo.
     */
    private ConversationSummaryData generateWithRetry(String userId, String systemPrompt, String userPrompt) {
        Exception lastError = null;
        for (int attempt = 1; attempt <= 2; attempt++) {
            try {
                return structuredChatClient.generateStructured(systemPrompt, userPrompt, ConversationSummaryData.class);
            } catch (Exception e) {
                lastError = e;
                log.warn("Tentativa {} falhou ao sumarizar conversa do usuário {}: {}", attempt, userId, e.getMessage());
            }
        }
        log.error("Falha ao sumarizar conversa do usuário {} após retentativas", userId, lastError);
        return null;
    }
}
