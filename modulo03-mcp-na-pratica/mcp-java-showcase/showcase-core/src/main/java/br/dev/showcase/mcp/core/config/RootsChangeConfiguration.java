package br.dev.showcase.mcp.core.config;

import java.util.List;
import java.util.function.BiConsumer;

import br.dev.showcase.mcp.core.service.RootsRegistry;
import io.modelcontextprotocol.server.McpAsyncServerExchange;
import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema.Root;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Registra o handler de {@code notifications/roots/list_changed}.
 *
 * <p>Nao existe anotacao de servidor para roots (as anotacoes {@code @Mcp*ListChanged}
 * do Spring AI sao do lado do cliente). O gancho oficial e um bean com a assinatura
 * exata que o {@code McpServerAutoConfiguration} procura:
 * {@code BiConsumer<McpSyncServerExchange, List<Root>>}. O autoconfigure repassa esse
 * bean para o {@code rootsChangeHandler(...)} do builder do SDK.
 *
 * <p>Cuidado que custou um debug: o {@code McpSyncServerCustomizer} <b>nao</b> serve
 * aqui - o autoconfigure aceita apenas <em>um</em> customizer e os starters webmvc e
 * webflux ja registram o deles.
 */
@Configuration(proxyBeanMethods = false)
public class RootsChangeConfiguration {

    /** Servidores em modo SYNC (os tres deste projeto). */
    @Bean
    BiConsumer<McpSyncServerExchange, List<Root>> syncRootsChangeHandler(RootsRegistry registry) {
        return (exchange, roots) -> registry.onRootsChanged(roots);
    }

    /** Servidores em modo ASYNC, caso o projeto seja adaptado para essa variante. */
    @Bean
    BiConsumer<McpAsyncServerExchange, List<Root>> asyncRootsChangeHandler(RootsRegistry registry) {
        return (exchange, roots) -> registry.onRootsChanged(roots);
    }
}
