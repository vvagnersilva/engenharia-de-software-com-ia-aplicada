package com.songhighlights;

import com.songhighlights.domain.model.ConversationSummaryData;
import com.songhighlights.domain.model.LlmChatResponse;
import com.songhighlights.domain.model.UserPreferencesData;
import com.songhighlights.domain.port.ChatModelPort;
import com.songhighlights.interfaces.web.dto.ChatApiResponse;
import com.songhighlights.interfaces.web.dto.ChatRequest;
import com.songhighlights.interfaces.web.dto.PreferencesResponse;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentCaptor.forClass;

/**
 * Prova que uma segunda chamada REST a /api/v1/chat "lembra" das preferências
 * extraídas na primeira chamada - equivalente ao que o teste
 * tests/chat.e2e.test.ts do projeto TypeScript original verificava através de
 * chamadas sucessivas a graph.invoke() com o mesmo thread_id/userId.
 *
 * O cliente do LLM é mockado (ChatModelPort) para não depender de uma
 * chave de API real; o Postgres também não é necessário aqui - o perfil
 * "test" (application-test.yml) usa H2 em memória.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class ChatFlowIntegrationTest {

    @MockitoBean
    private ChatModelPort chatModelPort;

    @org.springframework.beans.factory.annotation.Autowired
    private TestRestTemplate restTemplate;

    @Test
    void secondChatCallRemembersPreferencesExtractedInTheFirstCall() {
        String userId = "test-alex";

        LlmChatResponse firstTurnResponse = new LlmChatResponse(
                "E aí, Alex! Rock é demais! Recomendo \"Everlong\" do Foo Fighters!",
                new UserPreferencesData("Alex", null, List.of("rock"), null, null, null, null),
                true
        );

        LlmChatResponse secondTurnResponse = new LlmChatResponse(
                "Baseado no que sei sobre você, tente \"The Pretender\" do Foo Fighters!",
                null,
                false
        );

        ConversationSummaryData summaryAfterEachTurn = new ConversationSummaryData(
                "Alex", null, List.of("rock"), null, "Gosta de rock, especialmente Foo Fighters.", null
        );

        when(chatModelPort.generateStructured(anyString(), anyString(), eq(LlmChatResponse.class)))
                .thenReturn(firstTurnResponse, secondTurnResponse);

        when(chatModelPort.generateStructured(anyString(), anyString(), eq(ConversationSummaryData.class)))
                .thenReturn(summaryAfterEachTurn);

        // Primeira chamada REST - equivalente ao primeiro graph.invoke() do CLI original
        ResponseEntity<ChatApiResponse> firstCall = restTemplate.postForEntity(
                "/api/v1/chat",
                new ChatRequest(userId, "Oi! Meu nome é Alex e eu amo rock"),
                ChatApiResponse.class
        );

        assertThat(firstCall.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(firstCall.getBody()).isNotNull();
        assertThat(firstCall.getBody().preferencesUpdated()).isTrue();

        // As preferências extraídas na primeira chamada já devem estar persistidas
        ResponseEntity<PreferencesResponse> preferencesAfterFirstCall = restTemplate.getForEntity(
                "/api/v1/preferences/" + userId,
                PreferencesResponse.class
        );

        assertThat(preferencesAfterFirstCall.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(preferencesAfterFirstCall.getBody()).isNotNull();
        assertThat(preferencesAfterFirstCall.getBody().name()).isEqualTo("Alex");
        assertThat(preferencesAfterFirstCall.getBody().favoriteGenres()).contains("rock");

        // Segunda chamada REST - novo request HTTP, sem nenhum estado do lado do cliente
        // além do mesmo userId. O servidor precisa recarregar o contexto sozinho.
        ResponseEntity<ChatApiResponse> secondCall = restTemplate.postForEntity(
                "/api/v1/chat",
                new ChatRequest(userId, "Pode recomendar músicas?"),
                ChatApiResponse.class
        );

        assertThat(secondCall.getStatusCode()).isEqualTo(HttpStatus.OK);

        var systemPromptCaptor = forClass(String.class);
        var userPromptCaptor = forClass(String.class);

        verify(chatModelPort, times(2)).generateStructured(
                systemPromptCaptor.capture(),
                userPromptCaptor.capture(),
                eq(LlmChatResponse.class)
        );

        String systemPromptOnSecondCall = systemPromptCaptor.getAllValues().get(1);

        assertThat(systemPromptOnSecondCall)
                .as("o system prompt da segunda chamada deve carregar as preferências salvas na primeira")
                .contains("Alex")
                .contains("rock");
    }
}
