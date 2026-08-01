package br.dev.showcase.mcp.core.prompts;

import java.util.List;

import br.dev.showcase.mcp.core.service.CatalogService;
import io.modelcontextprotocol.spec.McpSchema.GetPromptResult;
import io.modelcontextprotocol.spec.McpSchema.PromptMessage;
import io.modelcontextprotocol.spec.McpSchema.Role;
import io.modelcontextprotocol.spec.McpSchema.TextContent;
import org.springframework.ai.mcp.annotation.McpArg;
import org.springframework.ai.mcp.annotation.McpPrompt;
import org.springframework.stereotype.Component;

/**
 * Capacidade MCP: <b>prompts</b> - modelos de conversa versionados no servidor.
 *
 * <p>Diferente de tools, prompts nao executam nada: devolvem mensagens prontas para
 * o cliente injetar na conversa. Sao "atalhos" que o usuario escolhe (no Claude
 * Desktop, aparecem no menu de comandos).
 */
@Component
public class ShowcasePrompts {

    private final CatalogService catalog;

    public ShowcasePrompts(CatalogService catalog) {
        this.catalog = catalog;
    }

    /** Prompt sem argumentos. */
    @McpPrompt(name = "daily-standup",
            title = "Daily standup",
            description = "Roteiro curto de daily standup para o time.")
    public GetPromptResult dailyStandup() {
        String text = """
                Conduza minha daily standup. Pergunte, um item por vez:
                1. O que eu entreguei desde ontem?
                2. O que vou fazer hoje?
                3. O que esta me bloqueando?
                Ao final, resuma em tres linhas e destaque os bloqueios.
                """;
        return GetPromptResult.builder(List.of(
                        new PromptMessage(Role.USER, TextContent.builder(text).build())))
                .description("Roteiro de daily standup")
                .build();
    }

    /** Prompt com argumentos, sendo um obrigatorio e um opcional. */
    @McpPrompt(name = "code-review",
            title = "Revisao de codigo",
            description = "Monta um pedido de revisao de codigo com foco e severidade configuraveis.")
    public GetPromptResult codeReview(
            @McpArg(name = "language", description = "Linguagem do codigo, por exemplo java ou typescript",
                    required = true) String language,
            @McpArg(name = "focus", description = "Foco da revisao: seguranca, performance, legibilidade ou testes",
                    required = false) String focus) {

        String area = (focus == null || focus.isBlank()) ? "qualidade geral" : focus;
        String text = """
                Revise o codigo %s a seguir com foco em %s.

                Para cada achado informe: arquivo e linha, severidade (alta, media, baixa),
                o problema em uma frase e a correcao sugerida em codigo.
                Nao comente o que esta correto.
                """.formatted(language, area);

        return GetPromptResult.builder(List.of(
                        new PromptMessage(Role.USER, TextContent.builder(text).build())))
                .description("Revisao de codigo " + language + " com foco em " + area)
                .build();
    }

    /** Prompt multi-mensagem: alterna papeis para dar contexto ao modelo. */
    @McpPrompt(name = "sales-analysis",
            title = "Analise de catalogo",
            description = "Conversa pre-montada (varias mensagens) para analisar o catalogo e sugerir reposicao.")
    public GetPromptResult salesAnalysis(
            @McpArg(name = "threshold", description = "Estoque minimo considerado saudavel. Padrao: 5",
                    required = false) Integer threshold) {

        int minimum = threshold == null ? 5 : threshold;

        return GetPromptResult.builder(List.of(
                        new PromptMessage(Role.ASSISTANT, TextContent.builder(
                                "Sou o analista de estoque desta loja. Trabalho apenas com os dados do catalogo "
                                        + "abaixo e nao invento numeros.").build()),
                        new PromptMessage(Role.USER, TextContent.builder(
                                "Este e o catalogo atual:\n\n" + catalog.overviewAsText()).build()),
                        new PromptMessage(Role.USER, TextContent.builder(
                                "Considerando estoque minimo saudavel de " + minimum + " unidades, liste os "
                                        + "produtos que precisam de reposicao urgente, em ordem de prioridade, "
                                        + "e justifique cada um em uma linha.").build())))
                .description("Analise de catalogo com limite de estoque em " + minimum)
                .build();
    }
}
