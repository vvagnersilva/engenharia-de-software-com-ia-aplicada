export type ModelConfig = {
  apiKey: string;
  httpReferer: string;
  xTitle: string;

  models: string[];
  temperature: number;
  maxTokens: number;
};

console.assert(process.env.OPENROUTER_API_KEY, 'OPENROUTER_API_KEY is not set in environment variables');

export const config: ModelConfig = {
  apiKey: process.env.OPENROUTER_API_KEY!,
  httpReferer: '',
  xTitle: 'IA Devs - Transforming Services into Tools',
  models: [
    'meta-llama/llama-3.3-70b-instruct',
  ],
  temperature: 0.7,
  maxTokens: 2048,
};
