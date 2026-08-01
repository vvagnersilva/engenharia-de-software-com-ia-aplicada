package br.dev.showcase.mcp.server.stdio;

import br.dev.showcase.mcp.core.ShowcaseCore;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Servidor MCP com transporte <b>STDIO</b>.
 *
 * <p>O processo troca JSON-RPC por stdin/stdout, entao a saida padrao precisa
 * conter <em>somente</em> mensagens do protocolo. Por isso o banner e o log de
 * console estao desligados no {@code application.yml} e o log vai para arquivo.
 *
 * <p>E o transporte usado por Claude Desktop, Claude Code e MCP Inspector quando
 * o servidor roda como subprocesso local.
 */
@SpringBootApplication(scanBasePackages = { "br.dev.showcase.mcp.server.stdio", ShowcaseCore.BASE_PACKAGE })
public class ShowcaseStdioServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(ShowcaseStdioServerApplication.class, args);
    }
}
