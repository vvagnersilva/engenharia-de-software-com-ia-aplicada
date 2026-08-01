package br.dev.showcase.mcp.core.tools;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import br.dev.showcase.mcp.core.model.ForecastReport;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

/**
 * Capacidade MCP: <b>structured output</b>.
 *
 * <p>Com {@code generateOutputSchema = true}, o Spring AI publica um
 * {@code outputSchema} derivado do record {@link ForecastReport} e devolve o objeto
 * em {@code structuredContent} - alem do texto JSON em {@code content}, que mantem
 * a compatibilidade com clientes que so leem texto.
 *
 * <p>Os dados sao deterministicos (semente derivada da cidade) para que os testes
 * possam validar o resultado sem depender de servico externo.
 */
@Component
public class WeatherTools {

    private static final List<String> CONDITIONS =
            List.of("ceu limpo", "parcialmente nublado", "nublado", "chuva fraca", "pancadas de chuva");

    @McpTool(name = "showcase_weather_forecast",
            annotations = @McpTool.McpAnnotations(readOnlyHint = true, destructiveHint = false,
                    idempotentHint = true, openWorldHint = false),
            title = "Previsao do tempo (saida estruturada)",
            description = "Devolve uma previsao do tempo simulada com schema de saida declarado.",
            generateOutputSchema = true)
    public ForecastReport forecast(
            @McpToolParam(description = "Cidade consultada", required = true) String city,
            @McpToolParam(description = "Quantidade de dias entre 1 e 7. Padrao: 3", required = false)
            Integer days) {

        int horizon = Math.clamp(days == null ? 3 : days, 1, 7);
        Random random = new Random(city.toLowerCase().hashCode());

        List<ForecastReport.Day> forecastDays = new ArrayList<>(horizon);
        LocalDate date = LocalDate.now(ZoneOffset.UTC);
        for (int i = 0; i < horizon; i++) {
            double min = 14 + random.nextInt(8);
            double max = min + 4 + random.nextInt(9);
            forecastDays.add(new ForecastReport.Day(
                    date.plusDays(i).toString(),
                    round(min),
                    round(max),
                    CONDITIONS.get(random.nextInt(CONDITIONS.size())),
                    random.nextInt(101)));
        }

        return new ForecastReport(city, "celsius",
                ZonedDateTime.now(ZoneOffset.UTC).toString(), forecastDays);
    }

    private static double round(double value) {
        return Math.round(value * 10.0) / 10.0;
    }
}
