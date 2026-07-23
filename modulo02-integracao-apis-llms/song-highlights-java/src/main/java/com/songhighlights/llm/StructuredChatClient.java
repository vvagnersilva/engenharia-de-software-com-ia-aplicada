package com.songhighlights.llm;

/**
 * Abstração sobre o cliente do LLM que retorna saída estruturada (JSON validado
 * contra um tipo Java), equivalente ao OpenRouterService.generateStructured do
 * projeto TypeScript original. Existe como interface para permitir mock em testes
 * sem precisar de uma chave de API real.
 */
public interface StructuredChatClient {

    <T> T generateStructured(String systemPrompt, String userPrompt, Class<T> responseType);
}
