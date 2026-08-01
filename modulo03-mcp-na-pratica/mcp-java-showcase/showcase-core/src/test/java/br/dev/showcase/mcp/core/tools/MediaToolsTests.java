package br.dev.showcase.mcp.core.tools;

import java.util.Base64;

import br.dev.showcase.mcp.core.service.CatalogService;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.EmbeddedResource;
import io.modelcontextprotocol.spec.McpSchema.ImageContent;
import io.modelcontextprotocol.spec.McpSchema.TextContent;
import io.modelcontextprotocol.spec.McpSchema.TextResourceContents;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** Conteudo nao textual: a resposta carrega texto, PNG valido e CSV embutido. */
class MediaToolsTests {

    private final MediaTools tools = new MediaTools(new CatalogService());

    @Test
    void respostaTrazTextoImagemERecursoEmbutido() {
        CallToolResult result = tools.stockChart(null);

        assertThat(result.isError()).isFalse();
        assertThat(result.content()).hasSize(3);
        assertThat(result.content().get(0)).isInstanceOf(TextContent.class);
        assertThat(result.content().get(1)).isInstanceOf(ImageContent.class);
        assertThat(result.content().get(2)).isInstanceOf(EmbeddedResource.class);
    }

    @Test
    void imagemEUmPngValidoEmBase64() {
        ImageContent image = (ImageContent) tools.stockChart("monitores").content().get(1);

        assertThat(image.mimeType()).isEqualTo("image/png");
        byte[] bytes = Base64.getDecoder().decode(image.data());
        // Assinatura PNG: 0x89 'P' 'N' 'G'
        assertThat(bytes[0] & 0xFF).isEqualTo(0x89);
        assertThat(new String(bytes, 1, 3)).isEqualTo("PNG");
    }

    @Test
    void csvEmbutidoTemCabecalhoEUmaLinhaPorProduto() {
        EmbeddedResource resource = (EmbeddedResource) tools.stockChart("monitores").content().get(2);
        TextResourceContents csv = (TextResourceContents) resource.resource();

        assertThat(csv.mimeType()).isEqualTo("text/csv");
        assertThat(csv.text()).startsWith("sku,name,category,price_cents,stock");
        assertThat(csv.text().lines()).hasSize(3); // cabecalho + 2 monitores
    }

    @Test
    void categoriaInexistenteDevolveIsError() {
        CallToolResult result = tools.stockChart("nao-existe");
        assertThat(result.isError()).isTrue();
    }
}
