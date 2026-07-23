import { type GraphState } from "../graph.ts";

export function identifyIntent(state: GraphState): GraphState {
    const input = state.messages.at(-1)?.content?.toString() ?? ""
    const inputLower = input.toLowerCase()

    let command: GraphState['command'] = 'unknown'

    if(inputLower.includes('upper')) {
        command = 'uppercase'
    } else if (inputLower.includes('lower')) {
        command = 'lowercase'
    }
PerformanceEntry

    return {
        ...state,
        command,
        output: input
    }

}