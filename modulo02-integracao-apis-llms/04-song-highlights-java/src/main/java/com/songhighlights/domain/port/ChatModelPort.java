package com.songhighlights.domain.port;

/**
 * Abstração sobre o cliente do LLM que retorna saída estruturada (JSON validado
 * contra um tipo Java), equivalente ao OpenRouterService.generateStructured do
 * projeto TypeScript original. Existe como porta para permitir mock em testes
 * sem precisar de uma chave de API real e para isolar o domínio/aplicação do
 * provedor concreto (OpenRouter, Spring AI, etc.).
 */
public interface ChatModelPort {

    <T> T generateStructured(String systemPrompt, String userPrompt, Class<T> responseType);
}
