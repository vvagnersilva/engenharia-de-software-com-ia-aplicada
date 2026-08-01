package br.dev.showcase.mcp.server.webflux;

import br.dev.showcase.mcp.core.ShowcaseCore;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Servidor MCP <b>Streamable HTTP reativo</b> (WebFlux, I/O nao bloqueante).
 *
 * <p>Dois modos, escolhidos por perfil:
 * <ul>
 *   <li>padrao - {@code protocol: STREAMABLE}, com sessao: da suporte a sampling,
 *       elicitation, roots, progresso e logging, porque existe um canal aberto de
 *       volta para o cliente;</li>
 *   <li>{@code stateless} - {@code protocol: STATELESS}, sem sessao: cada requisicao
 *       e independente, o que permite escalar horizontalmente sem sticky session, em
 *       troca de perder tudo que depende do canal servidor -> cliente.</li>
 * </ul>
 */
@SpringBootApplication(scanBasePackages = { "br.dev.showcase.mcp.server.webflux", ShowcaseCore.BASE_PACKAGE })
public class ShowcaseWebFluxServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(ShowcaseWebFluxServerApplication.class, args);
    }
}
