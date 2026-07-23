import { ChatOpenAI } from "@langchain/openai";
import { config, type ModelConfig } from "../config.ts";
import { z } from 'zod/v3'
import { createAgent, HumanMessage, providerStrategy, SystemMessage } from "langchain";

export class OpenRouterService {
    private config: ModelConfig
    private llmClient: ChatOpenAI
    // temperature 0 para extrações estruturadas (ex: datas), garantindo que a
    // mesma frase ("hoje às 14h") seja sempre convertida para o mesmo ISO,
    // o que é essencial para casar agendamento e cancelamento no banco
    private deterministicClient: ChatOpenAI

    constructor(configOverride?: ModelConfig) {
        this.config = configOverride ?? config
        this.llmClient = this.buildClient(this.config.temperature)
        this.deterministicClient = this.buildClient(0)
    }

    private buildClient(temperature: number): ChatOpenAI {
        return new ChatOpenAI({
            apiKey: this.config.apiKey,
            modelName: this.config.models.at(0),
            temperature,
            configuration: {
                baseURL: 'https://openrouter.ai/api/v1',
                defaultHeaders: {
                    'HTTP-Referer': this.config.httpReferer,
                    'X-Title': this.config.xTitle
                }
            },

            // aqui vai a conf do open router (smart model)
            modelKwargs: {
                models: this.config.models,
                provider: this.config.provider
            }
        })
    }

    async generateStructured<T>(
        systemPrompt: string,
        userPrompt: string,
        schema: z.ZodSchema<T>,
        options?: { deterministic?: boolean }
    ) {
        try {
            const agent = createAgent({
                model: options?.deterministic ? this.deterministicClient : this.llmClient,
                tools: [],
                responseFormat: providerStrategy(schema)
            })
            const messages = [
                new SystemMessage(systemPrompt),
                new HumanMessage(userPrompt)
            ]
            const data = await agent.invoke({ messages })
            return {
                success: true,
                data: data.structuredResponse,
            }
        } catch (error) {
            console.error('Error OpenRouterService', error)

            return {
                success: true,
                error: error instanceof Error ?
                    error.message :
                    String(error),
            }
        }
    }
}