package br.dev.showcase.mcp.client.support;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

import org.springframework.stereotype.Component;

/**
 * Buffer circular com as notificacoes que os servidores enviaram para este cliente.
 *
 * <p>Notificacoes MCP sao assincronas e chegam fora do fluxo de requisicao/resposta.
 * Guardar as ultimas em memoria e o que permite a API REST mostrar depois o que
 * chegou durante uma demo.
 */
@Component
public class NotificationLog {

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");
    private static final int CAPACITY = 200;

    private final Deque<String> entries = new ArrayDeque<>(CAPACITY);

    public synchronized void record(String connection, String kind, String detail) {
        if (entries.size() >= CAPACITY) {
            entries.removeFirst();
        }
        entries.addLast("%s [%s] %s: %s".formatted(LocalTime.now().format(TIME), connection, kind, detail));
    }

    public synchronized List<String> recent(int max) {
        return entries.stream().skip(Math.max(0, entries.size() - max)).toList();
    }

    public synchronized List<String> drain() {
        List<String> snapshot = List.copyOf(entries);
        entries.clear();
        return snapshot;
    }
}
