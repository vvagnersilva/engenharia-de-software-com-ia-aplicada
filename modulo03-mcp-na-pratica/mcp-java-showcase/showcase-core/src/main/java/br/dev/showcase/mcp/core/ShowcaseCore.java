package br.dev.showcase.mcp.core;

/**
 * Marcador do pacote raiz do modulo core.
 *
 * <p>As aplicacoes de servidor importam as capacidades apontando o component scan
 * para {@link #BASE_PACKAGE}, mantendo o core totalmente independente de transporte.
 */
public final class ShowcaseCore {

    /** Pacote base varrido pelas aplicacoes de servidor. */
    public static final String BASE_PACKAGE = "br.dev.showcase.mcp.core";

    private ShowcaseCore() {
    }
}
