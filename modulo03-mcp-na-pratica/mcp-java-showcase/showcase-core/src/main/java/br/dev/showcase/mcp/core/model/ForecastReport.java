package br.dev.showcase.mcp.core.model;

import java.util.List;

/**
 * Saida estruturada (structured output) da tool de previsao do tempo.
 *
 * <p>Como a tool declara {@code generateOutputSchema = true}, este record vira
 * o {@code outputSchema} da tool e o resultado viaja em {@code structuredContent}.
 *
 * @param city         cidade consultada
 * @param unit         unidade de temperatura
 * @param generatedAt  instante da geracao (ISO-8601)
 * @param days         previsao dia a dia
 */
public record ForecastReport(String city, String unit, String generatedAt, List<Day> days) {

    /**
     * @param date        data no formato ISO-8601
     * @param minTemp     temperatura minima
     * @param maxTemp     temperatura maxima
     * @param condition   condicao predominante
     * @param rainChance  probabilidade de chuva de 0 a 100
     */
    public record Day(String date, double minTemp, double maxTemp, String condition, int rainChance) {
    }
}
