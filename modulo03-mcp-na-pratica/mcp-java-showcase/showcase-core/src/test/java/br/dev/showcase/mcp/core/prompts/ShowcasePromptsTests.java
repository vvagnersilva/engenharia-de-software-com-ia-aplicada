package br.dev.showcase.mcp.core.prompts;

import br.dev.showcase.mcp.core.service.CatalogService;
import io.modelcontextprotocol.spec.McpSchema.GetPromptResult;
import io.modelcontextprotocol.spec.McpSchema.Role;
import io.modelcontextprotocol.spec.McpSchema.TextContent;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ShowcasePromptsTests {

    private final ShowcasePrompts prompts = new ShowcasePrompts(new CatalogService());

    @Test
    void promptSemArgumentosTemUmaMensagemDeUsuario() {
        GetPromptResult result = prompts.dailyStandup();
        assertThat(result.messages()).hasSize(1);
        assertThat(result.messages().get(0).role()).isEqualTo(Role.USER);
    }

    @Test
    void promptComArgumentoOpcionalUsaPadraoQuandoAusente() {
        GetPromptResult result = prompts.codeReview("java", null);
        assertThat(text(result)).contains("qualidade geral");

        GetPromptResult focused = prompts.codeReview("java", "seguranca");
        assertThat(text(focused)).contains("seguranca");
    }

    @Test
    void promptMultiMensagemAlternaPapeis() {
        GetPromptResult result = prompts.salesAnalysis(4);

        assertThat(result.messages()).hasSize(3);
        assertThat(result.messages().get(0).role()).isEqualTo(Role.ASSISTANT);
        assertThat(result.messages().get(1).role()).isEqualTo(Role.USER);
        // A ultima mensagem incorpora o argumento
        assertThat(((TextContent) result.messages().get(2).content()).text()).contains("4 unidades");
    }

    private static String text(GetPromptResult result) {
        return ((TextContent) result.messages().get(0).content()).text();
    }
}
