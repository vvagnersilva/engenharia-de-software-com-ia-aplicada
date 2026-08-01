package br.dev.showcase.mcp.core.service;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import io.modelcontextprotocol.spec.McpSchema.Root;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Guarda o ultimo estado conhecido dos roots do cliente.
 *
 * <p>Os roots chegam por duas vias: sob demanda ({@code roots/list}, via
 * {@code McpSyncRequestContext#roots()}) e por notificacao
 * ({@code notifications/roots/list_changed}), tratada pelo handler registrado em
 * {@code RootsChangeConfiguration}. Esta classe e o ponto de encontro das duas.
 */
@Service
public class RootsRegistry {

    private static final Logger log = LoggerFactory.getLogger(RootsRegistry.class);

    private final AtomicReference<Snapshot> snapshot =
            new AtomicReference<>(new Snapshot(List.of(), null, "nunca recebido"));
    private final AtomicInteger changeCount = new AtomicInteger();

    /** Registra os roots recebidos em uma notificacao de mudanca. */
    public void onRootsChanged(List<Root> roots) {
        int count = changeCount.incrementAndGet();
        snapshot.set(new Snapshot(List.copyOf(roots), Instant.now().toString(),
                "notifications/roots/list_changed"));
        log.info("Roots do cliente mudaram (evento #{}): {}", count,
                roots.stream().map(Root::uri).toList());
    }

    /** Registra os roots obtidos por uma consulta explicita roots/list. */
    public void onRootsListed(List<Root> roots) {
        snapshot.set(new Snapshot(List.copyOf(roots), Instant.now().toString(), "roots/list"));
    }

    public Snapshot current() {
        return snapshot.get();
    }

    public int changeCount() {
        return changeCount.get();
    }

    /**
     * @param roots      roots conhecidos
     * @param updatedAt  quando foram atualizados (ISO-8601), ou {@code null}
     * @param source     origem da informacao
     */
    public record Snapshot(List<Root> roots, String updatedAt, String source) {
    }
}
