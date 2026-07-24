import { describe, it, beforeEach, afterEach } from 'node:test';
import assert from 'node:assert/strict';
import { z } from 'zod/v3';
import { OpenRouterService } from '../src/services/openRouterService.ts';

describe('OpenRouterService.generateStructured - error handling', () => {
    let originalFetch: typeof fetch;

    beforeEach(() => {
        originalFetch = global.fetch;
    });

    afterEach(() => {
        global.fetch = originalFetch;
    });

    it('returns success: false when the provider response is malformed (missing choices)', async () => {
        global.fetch = (async () => new Response(
            JSON.stringify({ id: 'x', object: 'chat.completion', model: 'test' }),
            { status: 200, headers: { 'content-type': 'application/json' } }
        )) as typeof fetch;

        const service = new OpenRouterService({
            apiKey: 'test-key',
            httpReferer: '',
            xTitle: 'test',
            models: ['test/model'],
            provider: { sort: { by: 'throughput' } },
            temperature: 0,
        });

        const result = await service.generateStructured(
            'system',
            'user',
            z.object({ message: z.string() }),
        );

        assert.equal(result.success, false);
        assert.ok(result.error);
    });
});
