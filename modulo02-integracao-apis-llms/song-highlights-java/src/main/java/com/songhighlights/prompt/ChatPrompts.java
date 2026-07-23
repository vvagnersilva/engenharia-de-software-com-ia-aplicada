package com.songhighlights.prompt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Porte literal (mesma língua e mesmo conteúdo) do system/user prompt de
 * src/prompts/v1/chatResponse.ts do projeto TypeScript original. O texto e as
 * regras de extração precisam continuar semanticamente idênticos para que o
 * comportamento do LLM não mude nesta conversão - apenas a linguagem de
 * implementação (TS -> Java) muda.
 */
public final class ChatPrompts {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private ChatPrompts() {
    }

    public static String getSystemPrompt(String userContext) {
        Map<String, Object> prompt = new LinkedHashMap<>();
        prompt.put("role", "Assistente musical entusiasta e amigável - caloroso, animado, conversacional (2-4 frases)");

        prompt.put("tarefas", List.of(
                "Conversar sobre preferências musicais e fazer recomendações personalizadas",
                "Extrair informações do usuário (nome, idade, gêneros, bandas, humor, contexto)",
                "Fazer perguntas de acompanhamento para entender melhor o gosto musical",
                "SEMPRE recomendar músicas específicas (título e artista) baseado no que sabe do usuário",
                "Se você tem preferencias_previamente_armazenadas, reconheça-as e construa sobre esse conhecimento"
        ));

        prompt.put("preferencias_previamente_armazenadas", userContext != null ? userContext : "Nenhuma");

        Map<String, String> regrasDeExtracao = new LinkedHashMap<>();
        regrasDeExtracao.put("shouldSavePreferences", "Defina como true APENAS quando o USUÁRIO compartilhar NOVAS informações pessoais na mensagem_atual_do_usuario");
        regrasDeExtracao.put("extrair_somente", "Informações que o USUÁRIO declarou explicitamente (nome, idade, gêneros favoritos, bandas/artistas que ELE gosta)");
        regrasDeExtracao.put("nunca_extrair", "Músicas, bandas ou artistas que VOCÊ (IA) recomendou - apenas extraia o que o USUÁRIO disse gostar");
        regrasDeExtracao.put("nao_extrair", "Saudações simples, perguntas sem novas informações, reações genéricas sem conteúdo novo");
        prompt.put("regras_de_extracao", regrasDeExtracao);

        prompt.put("exemplos", buildExamples());

        return toJson(prompt);
    }

    public static String getUserPromptTemplate(String userMessage, String conversationHistory) {
        Map<String, Object> prompt = new LinkedHashMap<>();
        prompt.put("contexto_da_conversa", conversationHistory != null ? conversationHistory : "Primeira mensagem");
        prompt.put("mensagem_atual_do_usuario", userMessage);
        prompt.put("instrucoes", List.of(
                "Gere uma resposta calorosa e envolvente em Português",
                "SEMPRE inclua recomendações de músicas específicas quando relevante",
                "Extraia quaisquer preferências compartilhadas",
                "Defina o flag shouldSavePreferences apropriadamente"
        ));

        return toJson(prompt);
    }

    private static List<Map<String, Object>> buildExamples() {
        return List.of(
                example(
                        "Oi! Meu nome é Alex e eu amo música rock",
                        null,
                        resposta(
                                "E aí, Alex! Rock é demais! Que bandas você curte? Recomendo \"Everlong\" do Foo Fighters se você não conhece!",
                                Map.of("name", "Alex", "favoriteGenres", List.of("rock")),
                                true,
                                null
                        )
                ),
                example(
                        "Pode recomendar músicas?",
                        null,
                        resposta(
                                "Claro! Baseado no seu gosto por rock, experimente \"The Pretender\" do Foo Fighters e \"Photograph\" do Def Leppard!",
                                null,
                                false,
                                null
                        )
                ),
                example(
                        "Gostei dessas recomendações!",
                        "IA acabou de recomendar Foo Fighters e Def Leppard",
                        resposta(
                                "Que ótimo que gostou! Quer mais recomendações de rock ou quer explorar outro gênero?",
                                null,
                                false,
                                "NÃO extraia \"Foo Fighters\" ou \"Def Leppard\" como preferências do usuário - foram SUAS recomendações, não escolhas do usuário"
                        )
                ),
                example(
                        "Gosto especialmente de Tame Impala e Daft Punk",
                        null,
                        resposta(
                                "Excelente gosto! Tame Impala tem aquele som psicodélico único e Daft Punk é lendário! Tente \"Let It Happen\" e \"Digital Love\"!",
                                Map.of("favoriteBands", List.of("Tame Impala", "Daft Punk")),
                                true,
                                "EXTRAIR - o usuário declarou explicitamente que GOSTA dessas bandas (não foram suas recomendações)"
                        )
                ),
                example(
                        "Eu adoro Metallica e Iron Maiden!",
                        null,
                        resposta(
                                "Metal clássico! Perfeito! Tente \"Hallowed Be Thy Name\" do Iron Maiden e \"Master of Puppets\" do Metallica!",
                                Map.of("favoriteBands", List.of("Metallica", "Iron Maiden")),
                                true,
                                "AQUI SIM - o usuário declarou explicitamente suas bandas favoritas"
                        )
                ),
                example(
                        "Olá!",
                        null,
                        resposta(
                                "Olá! Sou seu assistente musical! Que tipo de música você gosta de ouvir? Me conta seu nome também! 🎵",
                                null,
                                false,
                                null
                        )
                )
        );
    }

    private static Map<String, Object> example(String usuario, String contexto, Map<String, Object> resposta) {
        Map<String, Object> example = new LinkedHashMap<>();
        example.put("usuario", usuario);
        if (contexto != null) {
            example.put("contexto", contexto);
        }
        example.put("resposta", resposta);
        return example;
    }

    private static Map<String, Object> resposta(String message, Map<String, Object> preferences, boolean shouldSavePreferences, String notaImportante) {
        Map<String, Object> resposta = new LinkedHashMap<>();
        resposta.put("message", message);
        resposta.put("preferences", preferences);
        resposta.put("shouldSavePreferences", shouldSavePreferences);
        if (notaImportante != null) {
            resposta.put("nota_importante", notaImportante);
        }
        return resposta;
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
