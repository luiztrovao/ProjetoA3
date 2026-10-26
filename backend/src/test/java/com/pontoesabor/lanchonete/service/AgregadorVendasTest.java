package com.pontoesabor.lanchonete.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.pontoesabor.lanchonete.dto.RelatorioDtos.DesempenhoVendedor;
import com.pontoesabor.lanchonete.dto.RelatorioDtos.Indicadores;
import com.pontoesabor.lanchonete.dto.RelatorioDtos.Ponto;
import com.pontoesabor.lanchonete.dto.RelatorioDtos.TotalFormaPagamento;
import com.pontoesabor.lanchonete.dto.RelatorioDtos.VendaResumida;
import com.pontoesabor.lanchonete.model.FormaPagamento;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class AgregadorVendasTest {

    private static long proximoId = 1;

    /** 14/10/2026 é uma quarta-feira. */
    private static VendaResumida venda(String dataHora, String total, FormaPagamento forma, long vendedor) {
        return new VendaResumida(proximoId++, LocalDateTime.parse(dataHora), new BigDecimal(total), forma, vendedor,
                vendedor == 1 ? "Fernanda" : "Carlos");
    }

    private final List<VendaResumida> vendas = List.of(
            venda("2026-10-12T10:15", "20.00", FormaPagamento.PIX, 1),
            venda("2026-10-14T12:05", "50.00", FormaPagamento.DINHEIRO, 1),
            venda("2026-10-14T12:40", "30.00", FormaPagamento.CREDITO, 2),
            venda("2026-10-17T19:30", "40.00", FormaPagamento.PIX, 2));

    @Test
    void indicadoresSomamEMedem() {
        Indicadores indicadores = AgregadorVendas.indicadores(vendas);

        assertThat(indicadores.faturamento()).isEqualByComparingTo("140.00");
        assertThat(indicadores.pedidos()).isEqualTo(4);
        assertThat(indicadores.ticketMedio()).isEqualByComparingTo("35.00");
    }

    @Test
    void semVendasTudoZerado() {
        Indicadores indicadores = AgregadorVendas.indicadores(List.of());

        assertThat(indicadores.faturamento()).isEqualByComparingTo("0");
        assertThat(indicadores.ticketMedio()).isEqualByComparingTo("0");
        assertThat(AgregadorVendas.pico(AgregadorVendas.porHora(List.of()))).isNull();
    }

    @Test
    void porDiaIncluiDiasSemVenda() {
        List<Ponto> pontos = AgregadorVendas.porDia(vendas, LocalDate.parse("2026-10-12"), LocalDate.parse("2026-10-18"));

        assertThat(pontos).hasSize(7);
        assertThat(pontos.get(0).chave()).isEqualTo("2026-10-12");
        assertThat(pontos.get(0).rotulo()).isEqualTo("12/10");
        assertThat(pontos.get(1).pedidos()).isZero();
        assertThat(pontos.get(2).faturamento()).isEqualByComparingTo("80.00");
        assertThat(pontos.get(2).pedidos()).isEqualTo(2);
    }

    @Test
    void porMesCobreTodosOsMesesDoIntervalo() {
        List<Ponto> pontos = AgregadorVendas.porMes(vendas, LocalDate.parse("2026-07-01"), LocalDate.parse("2026-12-31"));

        assertThat(pontos).extracting(Ponto::chave)
                .containsExactly("2026-07", "2026-08", "2026-09", "2026-10", "2026-11", "2026-12");
        assertThat(pontos.get(3).rotulo()).isEqualTo("out/2026");
        assertThat(pontos.get(3).faturamento()).isEqualByComparingTo("140.00");
    }

    @Test
    void picoPorHoraEPorDiaDaSemana() {
        List<Ponto> porHora = AgregadorVendas.porHora(vendas);
        List<Ponto> porDia = AgregadorVendas.porDiaDaSemana(vendas);

        assertThat(porHora).hasSize(24);
        assertThat(AgregadorVendas.pico(porHora).rotulo()).isEqualTo("12h");
        assertThat(porDia).hasSize(7);
        assertThat(porDia.getFirst().rotulo()).isEqualTo("Segunda-feira");
        Ponto diaPico = AgregadorVendas.pico(porDia);
        assertThat(diaPico.chave()).isEqualTo("3");
        assertThat(diaPico.rotulo()).isEqualTo("Quarta-feira");
        assertThat(diaPico.faturamento()).isEqualByComparingTo("80.00");
    }

    @Test
    void empateNoPicoFicaComOPrimeiro() {
        List<Ponto> pontos = List.of(
                new Ponto("a", "A", 1, new BigDecimal("10.00")),
                new Ponto("b", "B", 1, new BigDecimal("10.00")));

        assertThat(AgregadorVendas.pico(pontos).chave()).isEqualTo("a");
    }

    @Test
    void desempenhoPorVendedorComParticipacao() {
        List<DesempenhoVendedor> vendedores = AgregadorVendas.porVendedor(vendas);

        assertThat(vendedores).extracting(DesempenhoVendedor::nome).containsExactly("Carlos", "Fernanda");
        // Carlos: 30 + 40 = 70 de 140 = 50%; Fernanda: 20 + 50 = 70 -> empate desfeito pelo nome
        assertThat(vendedores.getFirst().participacao()).isEqualByComparingTo("50.0");
        assertThat(vendedores.getFirst().ticketMedio()).isEqualByComparingTo("35.00");
    }

    @Test
    void formasDePagamentoAparecemTodas() {
        List<TotalFormaPagamento> formas = AgregadorVendas.porFormaPagamento(vendas);

        assertThat(formas).hasSize(FormaPagamento.values().length);
        TotalFormaPagamento pix = formas.stream().filter(f -> f.formaPagamento() == FormaPagamento.PIX).findFirst().orElseThrow();
        TotalFormaPagamento debito = formas.stream().filter(f -> f.formaPagamento() == FormaPagamento.DEBITO).findFirst().orElseThrow();
        assertThat(pix.valor()).isEqualByComparingTo("60.00");
        assertThat(pix.percentual()).isEqualByComparingTo("42.9");
        assertThat(debito.pedidos()).isZero();
    }

    @Test
    void variacaoEmRelacaoAoPeriodoAnterior() {
        assertThat(AgregadorVendas.variacao(new BigDecimal("140"), new BigDecimal("45"))).isEqualByComparingTo("211.1");
        assertThat(AgregadorVendas.variacao(new BigDecimal("30"), new BigDecimal("60"))).isEqualByComparingTo("-50.0");
        assertThat(AgregadorVendas.variacao(new BigDecimal("30"), BigDecimal.ZERO)).isNull();
    }
}
