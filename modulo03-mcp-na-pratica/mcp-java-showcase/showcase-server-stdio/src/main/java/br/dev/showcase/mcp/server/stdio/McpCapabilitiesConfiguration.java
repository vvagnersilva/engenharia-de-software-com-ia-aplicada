package br.dev.showcase.mcp.server.stdio;

import br.dev.showcase.mcp.core.config.ShowcaseServerCapabilities;
import org.springframework.ai.mcp.customizer.McpSyncServerCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Liga o {@code subscribe} de resources nas capabilities anunciadas.
 *
 * <p>Sem servidor web nao ha customizer concorrente do Spring AI, entao um bean
 * simples basta.
 */
@Configuration(proxyBeanMethods = false)
class McpCapabilitiesConfiguration {

    @Bean
    McpSyncServerCustomizer showcaseServerCustomizer() {
        return spec -> spec.capabilities(ShowcaseServerCapabilities.full());
    }
}
