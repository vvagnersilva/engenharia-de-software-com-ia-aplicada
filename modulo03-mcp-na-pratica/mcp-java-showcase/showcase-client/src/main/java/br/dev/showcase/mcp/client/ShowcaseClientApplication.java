package br.dev.showcase.mcp.client;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Cliente MCP que conecta simultaneamente nos tres servidores do projeto.
 *
 * <p>Duas formas de exercitar as demonstracoes:
 * <ul>
 *   <li>REST: {@code GET /demo} lista tudo, {@code POST /demo/{nome}} executa;</li>
 *   <li>CLI: {@code java -jar ... --demo=nome} roda uma demo e encerra.</li>
 * </ul>
 */
@SpringBootApplication
public class ShowcaseClientApplication {

    public static void main(String[] args) {
        SpringApplication.run(ShowcaseClientApplication.class, args);
    }
}
