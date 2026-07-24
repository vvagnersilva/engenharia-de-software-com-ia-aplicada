import { PostgresSaver } from "@langchain/langgraph-checkpoint-postgres"
import { PostgresStore } from "@langchain/langgraph-checkpoint-postgres/store"
import { MemorySaver } from "@langchain/langgraph-checkpoint"
import { InMemoryStore } from "@langchain/langgraph"
import { config } from "../config.ts"

export type MemoryService = {
    checkpointer: MemorySaver | PostgresSaver
    store: InMemoryStore | PostgresStore
}

export async function createMemoryService(): Promise<MemoryService> {
    const dbUri = config.memory.dbUri

    try {
        const store = PostgresStore.fromConnString(dbUri)
        const checkpointer = PostgresSaver.fromConnString(dbUri)

        await Promise.all([store.setup(), checkpointer.setup()])

        console.log(`✅ Memória configurada: PostgreSQL`);
        return {
            checkpointer,
            store,
        }
    } catch (error) {
        console.warn(`⚠️ PostgreSQL indisponível. Usando fallback em memória para a conversa: ${error instanceof Error ? error.message : String(error)}`)

        const store = new InMemoryStore()
        const checkpointer = new MemorySaver()

        console.log(`✅ Memória configurada: memória local`);
        return {
            checkpointer,
            store,
        }
    }
}