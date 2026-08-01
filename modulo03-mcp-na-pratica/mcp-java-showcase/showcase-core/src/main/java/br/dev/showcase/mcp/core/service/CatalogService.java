package br.dev.showcase.mcp.core.service;

import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import br.dev.showcase.mcp.core.model.Product;
import br.dev.showcase.mcp.core.model.ProductPage;
import org.springframework.stereotype.Service;

/**
 * Catalogo em memoria usado por tools e resources.
 *
 * <p>Nao ha banco de dados de proposito: o foco do projeto e o protocolo MCP,
 * nao a persistencia.
 */
@Service
public class CatalogService {

    private final Map<String, Product> products = new ConcurrentHashMap<>();
    private final AtomicInteger orderSequence = new AtomicInteger(1000);

    public CatalogService() {
        seed(new Product("SKU-001", "Teclado mecanico ABNT2", "perifericos", 34990, 12));
        seed(new Product("SKU-002", "Mouse ergonomico sem fio", "perifericos", 19990, 30));
        seed(new Product("SKU-003", "Monitor 27 polegadas QHD", "monitores", 149900, 5));
        seed(new Product("SKU-004", "Monitor 24 polegadas Full HD", "monitores", 89900, 0));
        seed(new Product("SKU-005", "Headset com cancelamento de ruido", "audio", 74900, 8));
        seed(new Product("SKU-006", "Webcam 1080p", "video", 29900, 15));
        seed(new Product("SKU-007", "Hub USB-C 7 portas", "acessorios", 24900, 22));
        seed(new Product("SKU-008", "Suporte de notebook aluminio", "acessorios", 15900, 40));
        seed(new Product("SKU-009", "Cadeira ergonomica", "mobiliario", 189900, 3));
        seed(new Product("SKU-010", "Mesa com regulagem de altura", "mobiliario", 349900, 2));
    }

    private void seed(Product product) {
        products.put(product.sku(), product);
    }

    public List<Product> findAll() {
        return products.values().stream()
                .sorted(Comparator.comparing(Product::sku))
                .toList();
    }

    public Optional<Product> findBySku(String sku) {
        return Optional.ofNullable(products.get(sku == null ? "" : sku.trim().toUpperCase()));
    }

    public List<String> categories() {
        return products.values().stream()
                .map(Product::category)
                .distinct()
                .sorted()
                .toList();
    }

    public List<Product> findByCategory(String category) {
        return findAll().stream()
                .filter(p -> p.category().equalsIgnoreCase(category))
                .toList();
    }

    /**
     * Pagina o catalogo no mesmo modelo de cursor opaco que o MCP usa nas listagens.
     *
     * @param cursor   cursor devolvido pela chamada anterior, ou {@code null} para a primeira pagina
     * @param pageSize tamanho da pagina
     */
    public ProductPage page(String cursor, int pageSize) {
        List<Product> all = findAll();
        int offset = decodeCursor(cursor);
        int size = Math.clamp(pageSize, 1, 50);
        int from = Math.min(offset, all.size());
        int to = Math.min(from + size, all.size());
        List<Product> slice = new ArrayList<>(all.subList(from, to));
        String nextCursor = to < all.size() ? encodeCursor(to) : null;
        return new ProductPage(slice, nextCursor, all.size());
    }

    /**
     * Reserva estoque. Devolve {@link Optional#empty()} quando a reserva e possivel
     * e uma mensagem de erro de negocio quando nao e.
     */
    public Optional<String> reserve(String sku, int quantity) {
        if (quantity <= 0) {
            return Optional.of("A quantidade precisa ser maior que zero (recebido: " + quantity + ").");
        }
        Optional<Product> found = findBySku(sku);
        if (found.isEmpty()) {
            return Optional.of("SKU desconhecido: " + sku + ". Use showcase_list_products para ver os validos.");
        }
        Product product = found.get();
        if (product.stock() < quantity) {
            return Optional.of("Estoque insuficiente para " + product.sku() + ": disponivel "
                    + product.stock() + ", solicitado " + quantity + ".");
        }
        products.put(product.sku(), new Product(product.sku(), product.name(), product.category(),
                product.priceCents(), product.stock() - quantity));
        return Optional.empty();
    }

    public String nextOrderId() {
        return "ORD-" + orderSequence.incrementAndGet();
    }

    /** Registra um produto novo em tempo de execucao (usado na demo de list changed). */
    public Product publish(String sku, String name, String category, long priceCents, int stock) {
        Product product = new Product(sku, name, category, priceCents, stock);
        products.put(sku, product);
        return product;
    }

    /** Visao textual do catalogo, servida como resource estatico. */
    public String overviewAsText() {
        StringBuilder sb = new StringBuilder("Catalogo MCP Showcase\n=====================\n");
        Map<String, List<Product>> byCategory = new LinkedHashMap<>();
        for (Product p : findAll()) {
            byCategory.computeIfAbsent(p.category(), k -> new ArrayList<>()).add(p);
        }
        byCategory.forEach((category, items) -> {
            sb.append("\n[").append(category).append("]\n");
            items.forEach(p -> sb.append("  ").append(p.sku()).append(" - ").append(p.name())
                    .append(" - R$ ").append(formatPrice(p.priceCents()))
                    .append(" - estoque ").append(p.stock()).append('\n'));
        });
        return sb.toString();
    }

    public static String formatPrice(long priceCents) {
        return "%d,%02d".formatted(priceCents / 100, priceCents % 100);
    }

    private static String encodeCursor(int offset) {
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(("offset:" + offset).getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    private static int decodeCursor(String cursor) {
        if (cursor == null || cursor.isBlank()) {
            return 0;
        }
        try {
            String decoded = new String(Base64.getUrlDecoder().decode(cursor),
                    java.nio.charset.StandardCharsets.UTF_8);
            return Integer.parseInt(decoded.substring("offset:".length()));
        }
        catch (RuntimeException ex) {
            throw new IllegalArgumentException("Cursor invalido: " + cursor, ex);
        }
    }
}
