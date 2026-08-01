package br.dev.showcase.mcp.core.tools;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Base64;
import java.util.List;

import javax.imageio.ImageIO;

import br.dev.showcase.mcp.core.model.Product;
import br.dev.showcase.mcp.core.service.CatalogService;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.EmbeddedResource;
import io.modelcontextprotocol.spec.McpSchema.ImageContent;
import io.modelcontextprotocol.spec.McpSchema.TextResourceContents;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

/**
 * Capacidade MCP: <b>conteudo nao textual</b> no retorno de uma tool.
 *
 * <p>Devolve tres blocos de conteudo na mesma resposta:
 * <ul>
 *   <li>{@code TextContent} - o resumo legivel;</li>
 *   <li>{@code ImageContent} - um PNG gerado em memoria e codificado em base64;</li>
 *   <li>{@code EmbeddedResource} - um CSV embutido, com URI propria.</li>
 * </ul>
 *
 * <p>O grafico e desenhado so com retangulos, sem texto, para nao depender de
 * fontes instaladas na imagem Docker.
 */
@Component
public class MediaTools {

    private static final int WIDTH = 320;
    private static final int HEIGHT = 180;

    private final CatalogService catalog;

    public MediaTools(CatalogService catalog) {
        this.catalog = catalog;
    }

    @McpTool(name = "showcase_stock_chart",
            annotations = @McpTool.McpAnnotations(readOnlyHint = true, destructiveHint = false,
                    idempotentHint = true, openWorldHint = false),
            title = "Grafico de estoque (imagem + recurso embutido)",
            description = "Gera um grafico de barras PNG do estoque por produto e anexa o CSV correspondente.")
    public CallToolResult stockChart(
            @McpToolParam(description = "Categoria a plotar. Vazio plota o catalogo inteiro.", required = false)
            String category) {

        List<Product> products = (category == null || category.isBlank())
                ? catalog.findAll()
                : catalog.findByCategory(category);

        if (products.isEmpty()) {
            return CallToolResult.builder()
                    .addTextContent("Nenhum produto na categoria '" + category + "'. Categorias: "
                            + String.join(", ", catalog.categories()))
                    .isError(true)
                    .build();
        }

        String pngBase64 = Base64.getEncoder().encodeToString(renderBarChart(products));
        String csv = toCsv(products);

        return CallToolResult.builder()
                .addTextContent("Grafico de estoque para " + products.size() + " produto(s).")
                .addContent(new ImageContent(null, pngBase64, "image/png", null))
                .addContent(new EmbeddedResource(null,
                        new TextResourceContents("showcase://reports/stock.csv", "text/csv", csv, null),
                        null))
                .isError(false)
                .build();
    }

    private byte[] renderBarChart(List<Product> products) {
        BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        try {
            g.setColor(new Color(0xF5, 0xF5, 0xF5));
            g.fillRect(0, 0, WIDTH, HEIGHT);

            int maxStock = Math.max(1, products.stream().mapToInt(Product::stock).max().orElse(1));
            int slot = Math.max(1, WIDTH / products.size());
            int barWidth = Math.max(1, slot - 6);

            for (int i = 0; i < products.size(); i++) {
                int barHeight = (int) ((HEIGHT - 20L) * products.get(i).stock() / maxStock);
                g.setColor(products.get(i).stock() == 0 ? new Color(0xC6, 0x28, 0x28) : new Color(0x1E, 0x88, 0xE5));
                g.fillRect(i * slot + 3, HEIGHT - 10 - barHeight, barWidth, barHeight);
            }

            g.setColor(new Color(0x42, 0x42, 0x42));
            g.fillRect(0, HEIGHT - 10, WIDTH, 2);
        }
        finally {
            g.dispose();
        }

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", out);
            return out.toByteArray();
        }
        catch (IOException ex) {
            throw new UncheckedIOException("Falha ao gerar o PNG do grafico", ex);
        }
    }

    private String toCsv(List<Product> products) {
        StringBuilder csv = new StringBuilder("sku,name,category,price_cents,stock\n");
        products.forEach(p -> csv.append(p.sku()).append(',')
                .append('"').append(p.name()).append('"').append(',')
                .append(p.category()).append(',')
                .append(p.priceCents()).append(',')
                .append(p.stock()).append('\n'));
        return csv.toString();
    }
}
