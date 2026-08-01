package br.dev.showcase.mcp.server.webflux;

import br.dev.showcase.mcp.core.config.ShowcaseServerCapabilities;
import org.springframework.ai.mcp.customizer.McpSyncServerCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Liga o {@code subscribe} de resources nas capabilities anunciadas.
 *
 * <p>Diferente do modulo webmvc, aqui nao ha customizer concorrente: o
 * {@code servletMcpSyncServerCustomizer} do Spring AI e condicionado a
 * {@code @ConditionalOnWebApplication(SERVLET)} e nao existe na pilha reativa.
 * Tambem nao usamos {@code immediateExecution}: no WebFlux os handlers rodam no
 * scheduler do SDK, sem bloquear o event loop.
 */
@Configuration(proxyBeanMethods = false)
class McpCapabilitiesConfiguration {

    @Bean
    McpSyncServerCustomizer showcaseServerCustomizer() {
        return spec -> spec.capabilities(ShowcaseServerCapabilities.full());
    }
}
