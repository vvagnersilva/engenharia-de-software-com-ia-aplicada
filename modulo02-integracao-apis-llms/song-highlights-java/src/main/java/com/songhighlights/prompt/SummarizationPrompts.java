package com.songhighlights.prompt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.songhighlights.llm.ConversationSummaryData;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Porte literal de src/prompts/v1/summarization.ts do projeto TypeScript original.
 */
public final class SummarizationPrompts {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private SummarizationPrompts() {
    }

    public static String getSummarizationSystemPrompt() {
        Map<String, Object> prompt = new LinkedHashMap<>();
        prompt.put("role", "Sumarizador de conversação para preferências musicais");
        prompt.put("tarefa", "Analisar conversa e extrair preferências musicais estruturadas");

        Map<String, String> campos = new LinkedHashMap<>();
        campos.put("name", "Nome do usuário");
        campos.put("age", "Idade do usuário");
        campos.put("favoriteGenres", "Todos os gêneros mencionados");
        campos.put("favoriteBands", "Todas as bandas/artistas mencionados");
        campos.put("keyPreferences", "Sumário de 2-4 frases sobre gostos, padrões de humor e contexto de escuta");
        campos.put("importantContext", "Outros detalhes relevantes");
        prompt.put("campos_para_extrair", campos);

        prompt.put("regras", List.of(
                "Combinar informações duplicadas",
                "Ser específico sobre gêneros e artistas",
                "Incluir associações de humor (ex: \"gosta de rock animado ao fazer exercícios\")",
                "Se atualizando sumário anterior, preservar info não discutida na nova conversa",
                "Incluir apenas informações explicitamente declaradas"
        ));

        return toJson(prompt);
    }

    public static String getSummarizationUserPrompt(List<Map<String, String>> conversationHistory, ConversationSummaryData previousSummary) {
        String conversa = conversationHistory.stream()
                .map(msg -> msg.get("role") + ": " + msg.get("content"))
                .reduce((a, b) -> a + "\n" + b)
                .orElse("");

        Map<String, Object> prompt = new LinkedHashMap<>();
        prompt.put("conversa", conversa);
        prompt.put("sumario_anterior", previousSummary != null ? previousSummary : "Nenhum");
        prompt.put("instrucoes", List.of(
                "Atualizar sumário com novas informações desta conversa",
                "Preservar info existente não discutida nas novas mensagens"
        ));

        return toJson(prompt);
    }

    private static String toJson(Object value) {
        try {
            ObjectNode node = MAPPER.valueToTree(value);
            return MAPPER.writeValueAsString(node);
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao serializar prompt para JSON", e);
        }
    }
}
