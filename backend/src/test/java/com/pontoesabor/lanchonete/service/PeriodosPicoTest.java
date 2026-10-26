package com.pontoesabor.lanchonete.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.pontoesabor.lanchonete.dto.RelatorioDtos.PeriodoPico;
import java.time.LocalDate;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** Limites dos períodos dos dashboards de picos de venda. */
class PeriodosPicoTest {

    @ParameterizedTest(name = "{0} de {1}: {2} a {3}")
    @CsvSource({
        // Semana de segunda a domingo (14/10/2026 é quarta; 18/10 é domingo)
        "SEMANA,   2026-10-14, 2026-10-12, 2026-10-18",
        "SEMANA,   2026-10-18, 2026-10-12, 2026-10-18",
        "SEMANA,   2026-10-12, 2026-10-12, 2026-10-18",
        // Semana que atravessa a virada do ano
        "SEMANA,   2027-01-01, 2026-12-28, 2027-01-03",
        "MES,      2026-02-10, 2026-02-01, 2026-02-28",
        "MES,      2028-02-10, 2028-02-01, 2028-02-29",
        "SEMESTRE, 2026-06-30, 2026-01-01, 2026-06-30",
        "SEMESTRE, 2026-07-01, 2026-07-01, 2026-12-31",
        "ANO,      2026-10-14, 2026-01-01, 2026-12-31"
    })
    void limitesDoPeriodo(PeriodoPico periodo, LocalDate referencia, LocalDate inicio, LocalDate fim) {
        LocalDate calculado = RelatorioService.inicioDoPeriodo(periodo, referencia);

        assertThat(calculado).isEqualTo(inicio);
        assertThat(RelatorioService.fimDoPeriodo(periodo, calculado)).isEqualTo(fim);
    }
}
