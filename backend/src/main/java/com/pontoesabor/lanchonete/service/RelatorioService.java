package com.pontoesabor.lanchonete.service;

import com.pontoesabor.lanchonete.dto.RelatorioDtos.DashboardResponse;
import com.pontoesabor.lanchonete.dto.RelatorioDtos.Granularidade;
import com.pontoesabor.lanchonete.dto.RelatorioDtos.Indicadores;
import com.pontoesabor.lanchonete.dto.RelatorioDtos.PeriodoPico;
import com.pontoesabor.lanchonete.dto.RelatorioDtos.PicosResponse;
import com.pontoesabor.lanchonete.dto.RelatorioDtos.Ponto;
import com.pontoesabor.lanchonete.dto.RelatorioDtos.ProdutoVendido;
import com.pontoesabor.lanchonete.dto.RelatorioDtos.RelatorioResponse;
import com.pontoesabor.lanchonete.dto.RelatorioDtos.ResumoProdutos;
import com.pontoesabor.lanchonete.dto.RelatorioDtos.VendaResumida;
import com.pontoesabor.lanchonete.dto.VendaDtos.VendaResponse;
import com.pontoesabor.lanchonete.exception.RegraNegocioException;
import com.pontoesabor.lanchonete.model.StatusVenda;
import com.pontoesabor.lanchonete.repository.RelatorioRepository;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Dashboard, Relatórios e picos de venda. Só vendas não canceladas entram nos números. */
@Service
@Transactional(readOnly = true)
public class RelatorioService {

    /** Até este número de dias o gráfico de evolução é por dia; acima, por mês (como no protótipo). */
    static final int MAXIMO_DIAS_GRAFICO_DIARIO = 62;

    /** Maior período aceito num relatório. */
    static final int MAXIMO_DIAS_RELATORIO = 2 * 366;

    private static final int LIMITE_MAIS_VENDIDOS = 5;
    private static final int LIMITE_PEDIDOS_RECENTES = 5;

    private final RelatorioRepository relatorioRepository;
    private final Clock relogio;

    public RelatorioService(RelatorioRepository relatorioRepository, Clock relogio) {
        this.relatorioRepository = relatorioRepository;
        this.relogio = relogio;
    }

    public DashboardResponse dashboard() {
        LocalDate hoje = LocalDate.now(relogio);
        LocalDate inicioSemana = hoje.minusDays(6);
        List<VendaResumida> ultimos7Dias = vendas(inicioSemana, hoje, null);
        List<VendaResumida> deHoje = ultimos7Dias.stream()
                .filter(venda -> venda.dataHora().toLocalDate().equals(hoje))
                .toList();
        ResumoProdutos produtos = new ResumoProdutos(relatorioRepository.produtosCadastrados(),
                relatorioRepository.produtosAtivos(), relatorioRepository.produtosComEstoqueBaixo());
        List<VendaResponse> recentes = relatorioRepository.pedidosRecentes(PageRequest.of(0, LIMITE_PEDIDOS_RECENTES))
                .stream().map(VendaResponse::de).toList();
        return new DashboardResponse(
                AgregadorVendas.indicadores(deHoje),
                produtos,
                AgregadorVendas.porDia(ultimos7Dias, inicioSemana, hoje),
                AgregadorVendas.indicadores(ultimos7Dias),
                recentes);
    }

    /** Relatório de um período livre (tela Relatórios). Sem datas, usa os últimos 7 dias. */
    public RelatorioResponse relatorio(LocalDate inicio, LocalDate fim, Long vendedorId) {
        LocalDate ate = fim != null ? fim : LocalDate.now(relogio);
        LocalDate de = inicio != null ? inicio : ate.minusDays(6);
        validarPeriodo(de, ate);
        List<VendaResumida> vendas = vendas(de, ate, vendedorId);

        long dias = ChronoUnit.DAYS.between(de, ate) + 1;
        Granularidade granularidade = dias == 1 ? Granularidade.HORA
                : dias <= MAXIMO_DIAS_GRAFICO_DIARIO ? Granularidade.DIA : Granularidade.MES;
        List<Ponto> evolucao = switch (granularidade) {
            case HORA -> AgregadorVendas.porHora(vendas);
            case DIA -> AgregadorVendas.porDia(vendas, de, ate);
            case MES -> AgregadorVendas.porMes(vendas, de, ate);
        };

        return new RelatorioResponse(de, ate, vendedorId,
                AgregadorVendas.indicadores(vendas),
                quantidadeVendida(de, ate, vendedorId),
                granularidade,
                evolucao,
                maisVendidos(de, ate, vendedorId),
                AgregadorVendas.porVendedor(vendas),
                AgregadorVendas.porFormaPagamento(vendas));
    }

    /**
     * Picos de venda do período que contém {@code referencia} (padrão: hoje): semana (segunda a domingo),
     * mês, semestre (janeiro–junho ou julho–dezembro) ou ano. Compara com o período anterior de mesmo tipo.
     */
    public PicosResponse picos(PeriodoPico periodo, LocalDate referencia, Long vendedorId) {
        LocalDate data = referencia != null ? referencia : LocalDate.now(relogio);
        LocalDate inicio = inicioDoPeriodo(periodo, data);
        LocalDate fim = fimDoPeriodo(periodo, inicio);
        LocalDate inicioAnterior = inicioDoPeriodo(periodo, inicio.minusDays(1));
        LocalDate fimAnterior = fimDoPeriodo(periodo, inicioAnterior);

        List<VendaResumida> vendas = vendas(inicio, fim, vendedorId);
        Indicadores atual = AgregadorVendas.indicadores(vendas);
        Indicadores anterior = AgregadorVendas.indicadores(vendas(inicioAnterior, fimAnterior, vendedorId));

        boolean porMes = periodo == PeriodoPico.SEMESTRE || periodo == PeriodoPico.ANO;
        List<Ponto> serie = porMes
                ? AgregadorVendas.porMes(vendas, inicio, fim)
                : AgregadorVendas.porDia(vendas, inicio, fim);
        List<Ponto> porDiaDaSemana = AgregadorVendas.porDiaDaSemana(vendas);
        List<Ponto> porHora = AgregadorVendas.porHora(vendas);

        return new PicosResponse(periodo, inicio, fim, atual, anterior,
                AgregadorVendas.variacao(atual.faturamento(), anterior.faturamento()),
                porMes ? Granularidade.MES : Granularidade.DIA,
                serie, AgregadorVendas.pico(serie),
                porDiaDaSemana, AgregadorVendas.pico(porDiaDaSemana),
                porHora, AgregadorVendas.pico(porHora));
    }

    static LocalDate inicioDoPeriodo(PeriodoPico periodo, LocalDate data) {
        return switch (periodo) {
            case SEMANA -> data.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            case MES -> data.withDayOfMonth(1);
            case SEMESTRE -> LocalDate.of(data.getYear(), data.getMonthValue() <= 6 ? 1 : 7, 1);
            case ANO -> data.withDayOfYear(1);
        };
    }

    static LocalDate fimDoPeriodo(PeriodoPico periodo, LocalDate inicio) {
        return switch (periodo) {
            case SEMANA -> inicio.plusDays(6);
            case MES -> inicio.with(TemporalAdjusters.lastDayOfMonth());
            case SEMESTRE -> inicio.plusMonths(6).minusDays(1);
            case ANO -> inicio.with(TemporalAdjusters.lastDayOfYear());
        };
    }

    private static void validarPeriodo(LocalDate inicio, LocalDate fim) {
        if (inicio.isAfter(fim)) {
            throw new RegraNegocioException("A data inicial deve ser anterior ou igual à data final.");
        }
        if (ChronoUnit.DAYS.between(inicio, fim) + 1 > MAXIMO_DIAS_RELATORIO) {
            throw new RegraNegocioException("O período do relatório deve ter no máximo 2 anos.");
        }
    }

    // ===== Consultas (intervalo de dias inclusivo → [início do dia inicial, início do dia seguinte ao final)) =====

    private List<VendaResumida> vendas(LocalDate inicio, LocalDate fim, Long vendedorId) {
        var de = inicio.atStartOfDay();
        var ate = fim.plusDays(1).atStartOfDay();
        return vendedorId == null
                ? relatorioRepository.vendasValidas(de, ate, StatusVenda.CANCELADA)
                : relatorioRepository.vendasValidasDoVendedor(de, ate, StatusVenda.CANCELADA, vendedorId);
    }

    private List<ProdutoVendido> maisVendidos(LocalDate inicio, LocalDate fim, Long vendedorId) {
        var de = inicio.atStartOfDay();
        var ate = fim.plusDays(1).atStartOfDay();
        var limite = PageRequest.of(0, LIMITE_MAIS_VENDIDOS);
        return vendedorId == null
                ? relatorioRepository.produtosVendidos(de, ate, StatusVenda.CANCELADA, limite)
                : relatorioRepository.produtosVendidosDoVendedor(de, ate, StatusVenda.CANCELADA, vendedorId, limite);
    }

    private long quantidadeVendida(LocalDate inicio, LocalDate fim, Long vendedorId) {
        var de = inicio.atStartOfDay();
        var ate = fim.plusDays(1).atStartOfDay();
        return vendedorId == null
                ? relatorioRepository.quantidadeVendida(de, ate, StatusVenda.CANCELADA)
                : relatorioRepository.quantidadeVendidaDoVendedor(de, ate, StatusVenda.CANCELADA, vendedorId);
    }
}
