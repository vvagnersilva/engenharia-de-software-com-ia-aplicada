package br.dev.showcase.mcp.core.tools;

import br.dev.showcase.mcp.core.model.ForecastReport;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** A previsao e deterministica por cidade, o que mantem os testes estaveis. */
class WeatherToolsTests {

    private final WeatherTools tools = new WeatherTools();

    @Test
    void previsaoRespeitaLimitesDeDias() {
        assertThat(tools.forecast("Curitiba", null).days()).hasSize(3);
        assertThat(tools.forecast("Curitiba", 7).days()).hasSize(7);
        assertThat(tools.forecast("Curitiba", 99).days()).hasSize(7);
        assertThat(tools.forecast("Curitiba", -1).days()).hasSize(1);
    }

    @Test
    void mesmaCidadeProduzMesmaSequencia() {
        ForecastReport first = tools.forecast("Recife", 5);
        ForecastReport second = tools.forecast("Recife", 5);
        assertThat(first.days()).isEqualTo(second.days());
    }

    @Test
    void temperaturasEProbabilidadesSaoCoerentes() {
        ForecastReport report = tools.forecast("Manaus", 7);
        report.days().forEach(day -> {
            assertThat(day.maxTemp()).isGreaterThan(day.minTemp());
            assertThat(day.rainChance()).isBetween(0, 100);
        });
    }
}
