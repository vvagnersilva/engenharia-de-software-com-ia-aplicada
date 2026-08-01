package br.dev.showcase.mcp.server.webmvc;

import br.dev.showcase.mcp.core.config.ShowcaseServerCapabilities;
import org.springframework.ai.mcp.customizer.McpSyncServerCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Ajusta as capabilities do servidor antes do {@code build()}.
 *
 * <p>O {@code McpServerAutoConfiguration} aceita apenas <b>um</b>
 * {@code McpSyncServerCustomizer} ({@code Optional<...>}, nao lista). Na pilha
 * servlet ele ja registra o {@code servletMcpSyncServerCustomizer}, cujo unico
 * efeito e {@code immediateExecution(true)}. Por isso este bean e {@code @Primary}
 * e <b>reaplica</b> aquele ajuste, alem de ligar o {@code subscribe} de resources.
 */
@Configuration(proxyBeanMethods = false)
class McpCapabilitiesConfiguration {

    @Bean
    @Primary
    McpSyncServerCustomizer showcaseServerCustomizer() {
        return spec -> spec
                // Preserva o comportamento do customizer servlet que estamos substituindo:
                // os handlers rodam na thread da requisicao, sem salto de scheduler.
                .immediateExecution(true)
                .capabilities(ShowcaseServerCapabilities.full());
    }
}
