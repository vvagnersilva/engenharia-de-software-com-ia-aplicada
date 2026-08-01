package br.dev.showcase.mcp.client.handlers;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import br.dev.showcase.mcp.client.sampling.SamplingProvider;
import br.dev.showcase.mcp.client.support.NotificationLog;
import io.modelcontextprotocol.spec.McpSchema.CreateMessageRequest;
import io.modelcontextprotocol.spec.McpSchema.CreateMessageResult;
import io.modelcontextprotocol.spec.McpSchema.ElicitFormRequest;
import io.modelcontextprotocol.spec.McpSchema.ElicitRequest;
import io.modelcontextprotocol.spec.McpSchema.ElicitResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.mcp.annotation.McpElicitation;
import org.springframework.ai.mcp.annotation.McpSampling;
import org.springframework.stereotype.Component;

/**
 * Lado do <b>cliente</b> das capacidades que o servidor solicita: sampling e elicitation.
 *
 * <p>Declarar estes handlers e o que faz o cliente anunciar as capabilities
 * correspondentes no {@code initialize}. Sem eles, os servidores veem
 * {@code sampling}/{@code elicitation} como nao suportados.
 *
 * <p>O atributo {@code clients} casa com o nome da conexao configurada em
 * {@code spring.ai.mcp.client.*.connections.<nome>}.
 */
@Component
public class ServerRequestHandlers {

    private static final Logger log = LoggerFactory.getLogger(ServerRequestHandlers.class);

    /** Nomes das conexoes atendidas por estes handlers. */
    public static final String[] ALL_CONNECTIONS = { "stdio", "webmvc", "webflux" };

    private final SamplingProvider samplingProvider;
    private final NotificationLog notifications;

    /**
     * Proxima resposta de elicitation, configuravel pela API REST para dar para
     * testar os tres desfechos do protocolo sem interface grafica.
     */
    private final AtomicReference<ElicitResult.Action> nextElicitationAction =
            new AtomicReference<>(ElicitResult.Action.ACCEPT);

    public ServerRequestHandlers(SamplingProvider samplingProvider, NotificationLog notifications) {
        this.samplingProvider = samplingProvider;
        this.notifications = notifications;
    }

    // -------------------------------------------------------------- sampling

    @McpSampling(clients = { "stdio", "webmvc", "webflux" })
    public CreateMessageResult handleSampling(CreateMessageRequest request) {
        log.info("Sampling solicitado pelo servidor: {} mensagem(ns), maxTokens={}",
                request.messages().size(), request.maxTokens());
        notifications.record("*", "sampling/createMessage",
                request.messages().size() + " mensagem(ns) recebida(s)");
        return samplingProvider.generate(request);
    }

    // ----------------------------------------------------------- elicitation

    @McpElicitation(clients = { "stdio", "webmvc", "webflux" })
    public ElicitResult handleElicitation(ElicitRequest request) {
        ElicitResult.Action action = nextElicitationAction.get();
        log.info("Elicitation solicitada pelo servidor: '{}' - respondendo {}", request.message(), action);
        notifications.record("*", "elicitation/create", request.message() + " -> " + action);

        if (action != ElicitResult.Action.ACCEPT) {
            // DECLINE e CANCEL nao carregam conteudo.
            return ElicitResult.builder(action).build();
        }

        return ElicitResult.builder(ElicitResult.Action.ACCEPT)
                .content(answerFor(request))
                .build();
    }

    /**
     * Monta uma resposta compativel com o schema pedido pelo servidor.
     *
     * <p>Um cliente de verdade abriria um formulario para o usuario a partir do
     * {@code requestedSchema}. Aqui preenchemos automaticamente para a demo ser
     * reproduzivel: campos booleanos viram {@code true}, textuais recebem uma
     * justificativa e numericos viram {@code 1}.
     */
    private Map<String, Object> answerFor(ElicitRequest request) {
        Map<String, Object> answer = new LinkedHashMap<>();
        if (!(request instanceof ElicitFormRequest form) || form.requestedSchema() == null) {
            return answer;
        }
        Object properties = form.requestedSchema().get("properties");
        if (!(properties instanceof Map<?, ?> fields)) {
            return answer;
        }
        fields.forEach((name, definition) -> {
            String type = (definition instanceof Map<?, ?> spec && spec.get("type") != null)
                    ? String.valueOf(spec.get("type")) : "string";
            answer.put(String.valueOf(name), switch (type) {
                case "boolean" -> Boolean.TRUE;
                case "integer", "number" -> 1;
                default -> "Preenchido automaticamente pelo showcase-client";
            });
        });
        return answer;
    }

    // ------------------------------------------------------------- controles

    public void setNextElicitationAction(ElicitResult.Action action) {
        nextElicitationAction.set(action);
    }

    public ElicitResult.Action getNextElicitationAction() {
        return nextElicitationAction.get();
    }
}
