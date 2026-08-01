package br.dev.showcase.mcp.server.webmvc;

import br.dev.showcase.mcp.core.ShowcaseCore;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Servidor MCP <b>Streamable HTTP sincrono</b> sobre a pilha servlet (Spring MVC).
 *
 * <p>O endpoint unico {@code /mcp} aceita POST (requisicoes JSON-RPC), GET (stream
 * SSE de notificacoes servidor -> cliente) e DELETE (encerramento de sessao). E o
 * transporte HTTP recomendado desde a revisao 2025-03-26 do protocolo.
 *
 * <p>O perfil {@code sse} troca o transporte para o SSE classico (dois endpoints),
 * mantido apenas por compatibilidade com clientes antigos.
 */
@SpringBootApplication(scanBasePackages = { "br.dev.showcase.mcp.server.webmvc", ShowcaseCore.BASE_PACKAGE })
public class ShowcaseWebMvcServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(ShowcaseWebMvcServerApplication.class, args);
    }
}
