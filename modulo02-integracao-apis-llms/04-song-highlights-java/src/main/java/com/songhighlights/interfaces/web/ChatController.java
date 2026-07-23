package com.songhighlights.interfaces.web;

import com.songhighlights.application.service.PreferencesService;
import com.songhighlights.application.usecase.HandleChatTurnUseCase;
import com.songhighlights.domain.model.ChatTurnResult;
import com.songhighlights.interfaces.web.dto.ChatApiResponse;
import com.songhighlights.interfaces.web.dto.ChatRequest;
import com.songhighlights.interfaces.web.dto.PreferencesResponse;
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

    private final HandleChatTurnUseCase handleChatTurnUseCase;
    private final PreferencesService preferencesService;

    public ChatController(HandleChatTurnUseCase handleChatTurnUseCase, PreferencesService preferencesService) {
        this.handleChatTurnUseCase = handleChatTurnUseCase;
        this.preferencesService = preferencesService;
    }

    @PostMapping("/chat")
    public ChatApiResponse chat(@Valid @RequestBody ChatRequest request) {
        ChatTurnResult result = handleChatTurnUseCase.handleTurn(request.userId(), request.message());
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
