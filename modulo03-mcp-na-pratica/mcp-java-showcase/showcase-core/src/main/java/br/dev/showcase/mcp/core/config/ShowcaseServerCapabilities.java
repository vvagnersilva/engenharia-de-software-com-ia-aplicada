package br.dev.showcase.mcp.core.config;

import io.modelcontextprotocol.spec.McpSchema.ServerCapabilities;

/**
 * Capabilities anunciadas no {@code initialize}.
 *
 * <p>Por que declarar isso a mao: o {@code McpServerAutoConfiguration} do Spring AI
 * 2.0.0 monta as capabilities a partir das propriedades
 * {@code spring.ai.mcp.server.capabilities.*}, mas passa {@code subscribe = false}
 * fixo em {@code resources(...)} - nao ha propriedade para ligar. Como este projeto
 * demonstra {@code resources/subscribe} e {@code notifications/resources/updated},
 * cada aplicacao de servidor reaplica as capabilities via
 * {@code McpSyncServerCustomizer}, que roda depois do autoconfigure e antes do
 * {@code build()}.
 */
public final class ShowcaseServerCapabilities {

    private ShowcaseServerCapabilities() {
    }

    /**
     * Todas as capabilities de servidor do protocolo, com subscription de resources ligada.
     */
    public static ServerCapabilities full() {
        return ServerCapabilities.builder()
                .tools(true)            // tools + notifications/tools/list_changed
                .resources(true, true)  // subscribe + notifications/resources/list_changed
                .prompts(true)          // prompts + notifications/prompts/list_changed
                .completions()          // completion/complete
                .logging()              // notifications/message + logging/setLevel
                .build();
    }
}
