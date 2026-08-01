package br.dev.showcase.mcp.core.resources;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

import javax.imageio.ImageIO;

import br.dev.showcase.mcp.core.model.Product;
import br.dev.showcase.mcp.core.service.CatalogService;
import io.modelcontextprotocol.spec.McpSchema.BlobResourceContents;
import io.modelcontextprotocol.spec.McpSchema.ReadResourceResult;
import io.modelcontextprotocol.spec.McpSchema.TextResourceContents;
import org.springframework.ai.mcp.annotation.McpResource;
import org.springframework.ai.mcp.annotation.context.McpSyncRequestContext;
import org.springframework.stereotype.Component;

/**
 * Capacidade MCP: <b>resources</b>, estaticos e por template de URI.
 *
 * <p>Resource estatico tem URI fixa e aparece em {@code resources/list}. Resource
 * template tem URI com variaveis ({@code {sku}}) e aparece em
 * {@code resources/templates/list} - o cliente monta a URI concreta antes de ler.
 */
@Component
public class CatalogResources {

    private final CatalogService catalog;

    public CatalogResources(CatalogService catalog) {
        this.catalog = catalog;
    }

    // ---------------------------------------------------------------- estaticos

    @McpResource(uri = "showcase://catalog/overview",
            name = "catalog-overview",
            title = "Visao geral do catalogo",
            description = "Relatorio em texto com todos os produtos agrupados por categoria.",
            mimeType = "text/plain")
    public String catalogOverview() {
        return catalog.overviewAsText();
    }

    @McpResource(uri = "showcase://catalog/products.json",
            name = "catalog-json",
            title = "Catalogo em JSON",
            description = "Catalogo completo serializado como JSON.",
            mimeType = "application/json")
    public ReadResourceResult catalogJson() {
        StringBuilder json = new StringBuilder("[\n");
        List<Product> all = catalog.findAll();
        for (int i = 0; i < all.size(); i++) {
            Product p = all.get(i);
            json.append("  {\"sku\":\"").append(p.sku())
                    .append("\",\"name\":\"").append(p.name())
                    .append("\",\"category\":\"").append(p.category())
                    .append("\",\"priceCents\":").append(p.priceCents())
                    .append(",\"stock\":").append(p.stock()).append('}')
                    .append(i < all.size() - 1 ? ",\n" : "\n");
        }
        json.append(']');

        return ReadResourceResult.builder(List.of(new TextResourceContents(
                "showcase://catalog/products.json", "application/json", json.toString(), null))).build();
    }

    @McpResource(uri = "showcase://assets/logo.png",
            name = "showcase-logo",
            title = "Logo do projeto",
            description = "Imagem PNG servida como resource binario (base64 em blob).",
            mimeType = "image/png")
    public ReadResourceResult logo() {
        return ReadResourceResult.builder(List.of(new BlobResourceContents(
                "showcase://assets/logo.png", "image/png", renderLogoBase64(), null))).build();
    }

    // ---------------------------------------------------------------- templates

    @McpResource(uri = "showcase://products/{sku}",
            name = "product-by-sku",
            title = "Produto por SKU",
            description = "Ficha de um produto especifico. Exemplo: showcase://products/SKU-001",
            mimeType = "text/plain")
    public String productBySku(String sku) {
        Optional<Product> product = catalog.findBySku(sku);
        if (product.isEmpty()) {
            return "Produto nao encontrado para o SKU '" + sku + "'.";
        }
        Product p = product.get();
        return """
                SKU.......: %s
                Nome......: %s
                Categoria.: %s
                Preco.....: R$ %s
                Estoque...: %d unidade(s)
                """.formatted(p.sku(), p.name(), p.category(),
                CatalogService.formatPrice(p.priceCents()), p.stock());
    }

    @McpResource(uri = "showcase://categories/{category}/products",
            name = "products-by-category",
            title = "Produtos de uma categoria",
            description = "Lista os produtos de uma categoria. Exemplo: showcase://categories/monitores/products",
            mimeType = "text/plain")
    public ReadResourceResult productsByCategory(McpSyncRequestContext context, String category) {
        // Um resource tambem pode usar o request context (aqui, para emitir log estruturado).
        context.debug("Lendo resource de categoria: " + category);

        List<Product> products = catalog.findByCategory(category);
        String body = products.isEmpty()
                ? "Nenhum produto na categoria '" + category + "'. Categorias validas: "
                        + String.join(", ", catalog.categories())
                : products.stream()
                        .map(p -> "- " + p.sku() + " " + p.name() + " (estoque " + p.stock() + ")")
                        .reduce(new StringBuilder("Categoria: " + category + "\n"),
                                (sb, line) -> sb.append(line).append('\n'), StringBuilder::append)
                        .toString();

        return ReadResourceResult.builder(List.of(new TextResourceContents(
                "showcase://categories/" + category + "/products", "text/plain", body, null))).build();
    }

    // ---------------------------------------------------------------- auxiliares

    private String renderLogoBase64() {
        BufferedImage image = new BufferedImage(64, 64, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        try {
            g.setColor(new Color(0x0D, 0x47, 0xA1));
            g.fillRect(0, 0, 64, 64);
            g.setColor(Color.WHITE);
            g.fillRect(12, 12, 12, 40);
            g.fillRect(40, 12, 12, 40);
            g.fillRect(24, 26, 16, 12);
        }
        finally {
            g.dispose();
        }
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", out);
            return Base64.getEncoder().encodeToString(out.toByteArray());
        }
        catch (IOException ex) {
            throw new UncheckedIOException("Falha ao gerar o logo PNG", ex);
        }
    }
}
