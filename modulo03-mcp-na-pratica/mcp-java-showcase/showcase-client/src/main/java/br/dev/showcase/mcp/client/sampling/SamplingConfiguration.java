package br.dev.showcase.mcp.client.sampling;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Escolhe o provedor de sampling conforme o ambiente.
 *
 * <p>Por padrao {@code spring.ai.model.chat=none}, entao nenhum {@code ChatModel} e
 * criado e vale o provedor mockado - o projeto roda sem nenhuma chave de API. Com o
 * perfil {@code real-llm} a propriedade vira {@code anthropic}, o {@code ChatModel}
 * aparece no contexto e o provedor real assume.
 *
 * <p>Usamos {@link ObjectProvider} em vez de {@code @ConditionalOnBean} porque
 * condicionais em {@code @Configuration} de usuario sao avaliadas antes das
 * autoconfiguracoes, e o {@code ChatModel} vem de uma delas.
 */
@Configuration(proxyBeanMethods = false)
public class SamplingConfiguration {

    private static final Logger log = LoggerFactory.getLogger(SamplingConfiguration.class);

    @Bean
    SamplingProvider samplingProvider(ObjectProvider<ChatModel> chatModels) {
        ChatModel chatModel = chatModels.getIfAvailable();
        if (chatModel == null) {
            log.info("Sampling: provedor mockado (nenhum ChatModel no contexto). "
                    + "Ative o perfil real-llm e informe ANTHROPIC_API_KEY para usar um LLM real.");
            return new MockSamplingProvider();
        }
        log.info("Sampling: LLM real via {}", chatModel.getClass().getSimpleName());
        return new ChatModelSamplingProvider(chatModel);
    }
}
