package com.songhighlights.controller;

import com.songhighlights.dto.ChatApiResponse;
import com.songhighlights.dto.ChatRequest;
import com.songhighlights.dto.PreferencesResponse;
import com.songhighlights.service.ChatOrchestrationService;
import com.songhighlights.service.ChatTurnResult;
import com.songhighlights.service.PreferencesService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Substitui o loop de CLI de src/index.ts: cada turno do chat (antes uma
 * chamada graph.invoke()) agora é uma chamada REST. O servidor carrega
 * histórico + preferências do userId, chama o LLM e persiste o resultado -
 * o cliente HTTP não precisa (nem deve) manter estado entre chamadas além
 * de reenviar o mesmo userId.
 */
@RestController
@RequestMapping("/api/v1")
public class ChatController {

    private final ChatOrchestrationService chatOrchestrationService;
    private final PreferencesService preferencesService;

    public ChatController(ChatOrchestrationService chatOrchestrationService, PreferencesService preferencesService) {
        this.chatOrchestrationService = chatOrchestrationService;
        this.preferencesService = preferencesService;
    }

    @PostMapping("/chat")
    public ChatApiResponse chat(@Valid @RequestBody ChatRequest request) {
        ChatTurnResult result = chatOrchestrationService.handleTurn(request.userId(), request.message());
        return new ChatApiResponse(
                result.userId(),
                result.reply(),
                result.preferencesUpdated(),
                result.conversationSummarized()
        );
    }

    @GetMapping("/preferences/{userId}")
    public ResponseEntity<PreferencesResponse> getPreferences(@PathVariable String userId) {
        return preferencesService.getSummary(userId)
                .map(summary -> ResponseEntity.ok(PreferencesResponse.from(userId, summary)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
