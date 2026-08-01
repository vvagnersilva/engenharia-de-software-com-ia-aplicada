package br.dev.showcase.mcp.core.tools;

import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

/**
 * Capacidade MCP: <b>tools</b> - o caso mais simples possivel.
 *
 * <p>Entrada de tipos primitivos, saida em texto. O Spring AI gera o
 * {@code inputSchema} a partir da assinatura do metodo e dos {@code @McpToolParam}.
 */
@Component
public class GreetingTools {

    @McpTool(name = "showcase_greet",
            annotations = @McpTool.McpAnnotations(readOnlyHint = true, destructiveHint = false,
                    idempotentHint = true, openWorldHint = false),
            title = "Saudacao",
            description = "Monta uma saudacao no idioma escolhido. Exemplo mais simples de tool MCP.")
    public String greet(
            @McpToolParam(description = "Nome de quem sera saudado", required = true) String name,
            @McpToolParam(description = "Idioma da saudacao: pt, en ou es. Padrao: pt", required = false)
            String language) {

        String lang = (language == null || language.isBlank()) ? "pt" : language.toLowerCase();
        return switch (lang) {
            case "en" -> "Hello, " + name + "! Welcome to the MCP Java Showcase.";
            case "es" -> "Hola, " + name + "! Bienvenido al MCP Java Showcase.";
            default -> "Ola, " + name + "! Bem-vindo ao MCP Java Showcase.";
        };
    }
}
