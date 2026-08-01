package br.dev.showcase.mcp.client.demo;

import java.util.List;

/**
 * Saida de uma demonstracao.
 *
 * @param demo          identificador da demo
 * @param title         titulo legivel
 * @param output        linhas do relatorio
 * @param notifications notificacoes que chegaram do servidor durante a execucao
 */
public record DemoResult(String demo, String title, List<String> output, List<String> notifications) {

    public String asText() {
        StringBuilder sb = new StringBuilder("== ").append(title).append(" ==\n");
        output.forEach(line -> sb.append(line).append('\n'));
        if (!notifications.isEmpty()) {
            sb.append("\n-- notificacoes recebidas --\n");
            notifications.forEach(line -> sb.append(line).append('\n'));
        }
        return sb.toString();
    }
}
