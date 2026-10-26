package com.pontoesabor.lanchonete.dto;

import com.pontoesabor.lanchonete.dto.VendaDtos.VendaResponse;
import com.pontoesabor.lanchonete.model.FormaPagamento;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** DTOs do Dashboard, dos Relatórios e dos picos de venda. Só vendas não canceladas entram nos números. */
public final class RelatorioDtos {

    private RelatorioDtos() {
    }

    // ===== Dados lidos do banco =====

    /** Projeção enxuta de uma venda válida, usada nas agregações. */
    public record VendaResumida(
            Long id, LocalDateTime dataHora, BigDecimal total, FormaPagamento formaPagamento, Long vendedorId,
            String vendedorNome) {
    }

    /** Quantidade e faturamento de um produto no período (ranking de mais vendidos). */
    public record ProdutoVendido(Long produtoId, String codigo, String nome, Long quantidade, BigDecimal faturamento) {
    }

    // ===== Blocos das respostas =====

    /** Faturamento, quantidade de pedidos e ticket médio. */
    public record Indicadores(BigDecimal faturamento, long pedidos, BigDecimal ticketMedio) {
    }

    /**
     * Um ponto de um gráfico.
     *
     * @param chave identificador estável: "2026-10-26" (dia), "2026-10" (mês), "14" (hora) ou "1" a "7" (dia da semana, segunda = 1)
     * @param rotulo texto para exibir (ex.: "26/10", "out/2026", "14h", "Segunda-feira")
     */
    public record Ponto(String chave, String rotulo, long pedidos, BigDecimal faturamento) {
    }

    public enum Granularidade {
        HORA,
        DIA,
        MES
    }

    /** Períodos dos dashboards estratégicos de picos de venda. */
    public enum PeriodoPico {
        SEMANA,
        MES,
        SEMESTRE,
        ANO
    }

    public record DesempenhoVendedor(
            Long vendedorId, String nome, long vendas, BigDecimal faturamento, BigDecimal ticketMedio,
            BigDecimal participacao) {
    }

    public record TotalFormaPagamento(FormaPagamento formaPagamento, long pedidos, BigDecimal valor, BigDecimal percentual) {
    }

    public record ResumoProdutos(long cadastrados, long ativos, long estoqueBaixo) {
    }

    // ===== Respostas =====

    /** GET /api/dashboard: a tela inicial de Dono e Gerente. */
    public record DashboardResponse(
            Indicadores hoje,
            ResumoProdutos produtos,
            List<Ponto> ultimos7Dias,
            Indicadores resumo7Dias,
            List<VendaResponse> pedidosRecentes) {
    }

    /** GET /api/relatorios/resumo: a tela Relatórios para um período livre. */
    public record RelatorioResponse(
            LocalDate inicio,
            LocalDate fim,
            Long vendedorId,
            Indicadores indicadores,
            long produtosVendidos,
            Granularidade granularidade,
            List<Ponto> evolucao,
            List<ProdutoVendido> maisVendidos,
            List<DesempenhoVendedor> vendedores,
            List<TotalFormaPagamento> formasPagamento) {
    }

    /**
     * GET /api/relatorios/picos: maior pico de vendas na semana, no mês, no semestre ou no ano.
     *
     * @param variacaoFaturamento variação percentual em relação ao período anterior (nula se o anterior não teve vendas)
     */
    public record PicosResponse(
            PeriodoPico periodo,
            LocalDate inicio,
            LocalDate fim,
            Indicadores indicadores,
            Indicadores periodoAnterior,
            BigDecimal variacaoFaturamento,
            Granularidade granularidade,
            List<Ponto> serie,
            Ponto pico,
            List<Ponto> porDiaDaSemana,
            Ponto diaDaSemanaPico,
            List<Ponto> porHora,
            Ponto horaPico) {
    }
}
