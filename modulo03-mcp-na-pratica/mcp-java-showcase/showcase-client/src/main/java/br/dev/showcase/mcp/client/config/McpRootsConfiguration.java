package br.dev.showcase.mcp.client.config;

import java.nio.file.Path;
import java.util.List;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.spec.McpSchema.ClientCapabilities;
import io.modelcontextprotocol.spec.McpSchema.Root;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.mcp.customizer.McpClientCustomizer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Declara os <b>roots</b> que este cliente expoe aos servidores.
 *
 * <p>Roots sao as raizes de trabalho (normalmente pastas) que o cliente autoriza o
 * servidor a considerar. O {@code McpClientCustomizer} recebe o nome da conexao e a
 * spec do cliente antes do build, o que permite dar roots diferentes por servidor -
 * util quando um deles so deve enxergar parte do workspace.
 */
@Configuration(proxyBeanMethods = false)
public class McpRootsConfiguration {

    private static final Logger log = LoggerFactory.getLogger(McpRootsConfiguration.class);

    @Bean
    McpClientCustomizer<McpClient.SyncSpec> rootsCustomizer(
            @Value("${showcase.client.workspace:.}") String workspace) {

        String workspaceUri = Path.of(workspace).toAbsolutePath().normalize().toUri().toString();

        return (name, spec) -> {
            List<Root> roots = List.of(
                    new Root(workspaceUri, "workspace"),
                    new Root("file:///tmp/showcase-" + name, "scratch-" + name));
            log.info("Conexao '{}' recebera {} root(s): {}", name, roots.size(),
                    roots.stream().map(Root::uri).toList());
            spec.roots(roots);

            // O starter monta as capabilities so a partir dos handlers anotados
            // (@McpSampling/@McpElicitation) e NAO inclui roots. Este customizer roda
            // por ultimo, entao reaplicamos o conjunto completo - roots(listChanged)
            // mais as duas capabilities que os handlers deste projeto sustentam.
            spec.capabilities(ClientCapabilities.builder()
                    .roots(true)
                    .sampling()
                    .elicitation()
                    .build());
        };
    }
}
