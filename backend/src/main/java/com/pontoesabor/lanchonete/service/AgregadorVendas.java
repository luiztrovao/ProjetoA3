package com.pontoesabor.lanchonete.service;

import com.pontoesabor.lanchonete.dto.RelatorioDtos.DesempenhoVendedor;
import com.pontoesabor.lanchonete.dto.RelatorioDtos.Indicadores;
import com.pontoesabor.lanchonete.dto.RelatorioDtos.Ponto;
import com.pontoesabor.lanchonete.dto.RelatorioDtos.TotalFormaPagamento;
import com.pontoesabor.lanchonete.dto.RelatorioDtos.VendaResumida;
import com.pontoesabor.lanchonete.model.FormaPagamento;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;

/**
 * Cálculos dos dashboards e relatórios sobre uma lista de vendas válidas. Classe pura (sem banco),
 * para que cada regra possa ser testada isoladamente.
 */
final class AgregadorVendas {

    private static final Locale PT_BR = Locale.of("pt", "BR");
    private static final DateTimeFormatter DIA_MES = DateTimeFormatter.ofPattern("dd/MM");

    private AgregadorVendas() {
    }

    static Indicadores indicadores(List<VendaResumida> vendas) {
        BigDecimal faturamento = somar(vendas);
        return new Indicadores(faturamento, vendas.size(), dividir(faturamento, vendas.size()));
    }

    /** Um ponto por dia entre {@code inicio} e {@code fim} (inclusive), mesmo nos dias sem venda. */
    static List<Ponto> porDia(List<VendaResumida> vendas, LocalDate inicio, LocalDate fim) {
        Map<LocalDate, List<VendaResumida>> grupos = agrupar(vendas, venda -> venda.dataHora().toLocalDate());
        List<Ponto> pontos = new ArrayList<>();
        for (LocalDate dia = inicio; !dia.isAfter(fim); dia = dia.plusDays(1)) {
            pontos.add(ponto(dia.toString(), dia.format(DIA_MES), grupos.getOrDefault(dia, List.of())));
        }
        return pontos;
    }

    /** Um ponto por mês entre o mês de {@code inicio} e o de {@code fim}. */
    static List<Ponto> porMes(List<VendaResumida> vendas, LocalDate inicio, LocalDate fim) {
        Map<YearMonth, List<VendaResumida>> grupos = agrupar(vendas, venda -> YearMonth.from(venda.dataHora()));
        List<Ponto> pontos = new ArrayList<>();
        for (YearMonth mes = YearMonth.from(inicio); !mes.isAfter(YearMonth.from(fim)); mes = mes.plusMonths(1)) {
            String rotulo = mes.getMonth().getDisplayName(TextStyle.SHORT, PT_BR).replace(".", "") + "/" + mes.getYear();
            pontos.add(ponto(mes.toString(), rotulo, grupos.getOrDefault(mes, List.of())));
        }
        return pontos;
    }

    /** 24 pontos, de 0h a 23h: em que horário a lanchonete mais vende. */
    static List<Ponto> porHora(List<VendaResumida> vendas) {
        Map<Integer, List<VendaResumida>> grupos = agrupar(vendas, venda -> venda.dataHora().getHour());
        List<Ponto> pontos = new ArrayList<>();
        for (int hora = 0; hora < 24; hora++) {
            pontos.add(ponto(String.valueOf(hora), "%02dh".formatted(hora), grupos.getOrDefault(hora, List.of())));
        }
        return pontos;
    }

    /** 7 pontos, de segunda (1) a domingo (7): em que dia da semana a lanchonete mais vende. */
    static List<Ponto> porDiaDaSemana(List<VendaResumida> vendas) {
        Map<DayOfWeek, List<VendaResumida>> grupos = agrupar(vendas, venda -> venda.dataHora().getDayOfWeek());
        List<Ponto> pontos = new ArrayList<>();
        for (DayOfWeek dia : DayOfWeek.values()) {
            String nome = dia.getDisplayName(TextStyle.FULL, PT_BR);
            String rotulo = Character.toUpperCase(nome.charAt(0)) + nome.substring(1);
            pontos.add(ponto(String.valueOf(dia.getValue()), rotulo, grupos.getOrDefault(dia, List.of())));
        }
        return pontos;
    }

    /** O ponto de maior faturamento (empate: o de mais pedidos, depois o primeiro); nulo se não houve vendas. */
    static Ponto pico(List<Ponto> pontos) {
        return pontos.stream()
                .filter(ponto -> ponto.pedidos() > 0)
                .max(Comparator.comparing(Ponto::faturamento)
                        .thenComparingLong(Ponto::pedidos)
                        .thenComparing(Comparator.comparingInt(pontos::indexOf).reversed()))
                .orElse(null);
    }

    /** Desempenho por vendedor, do maior para o menor faturamento, com a participação no total. */
    static List<DesempenhoVendedor> porVendedor(List<VendaResumida> vendas) {
        BigDecimal totalGeral = somar(vendas);
        return agrupar(vendas, VendaResumida::vendedorId).values().stream()
                .map(doVendedor -> {
                    BigDecimal faturamento = somar(doVendedor);
                    VendaResumida qualquer = doVendedor.getFirst();
                    return new DesempenhoVendedor(qualquer.vendedorId(), qualquer.vendedorNome(), doVendedor.size(),
                            faturamento, dividir(faturamento, doVendedor.size()), percentual(faturamento, totalGeral));
                })
                .sorted(Comparator.comparing(DesempenhoVendedor::faturamento).reversed()
                        .thenComparing(DesempenhoVendedor::nome))
                .toList();
    }

    /** Total e percentual de cada forma de pagamento (todas aparecem, mesmo sem vendas). */
    static List<TotalFormaPagamento> porFormaPagamento(List<VendaResumida> vendas) {
        BigDecimal totalGeral = somar(vendas);
        Map<FormaPagamento, List<VendaResumida>> grupos = agrupar(vendas, VendaResumida::formaPagamento);
        List<TotalFormaPagamento> totais = new ArrayList<>();
        for (FormaPagamento forma : FormaPagamento.values()) {
            List<VendaResumida> daForma = grupos.getOrDefault(forma, List.of());
            BigDecimal valor = somar(daForma);
            totais.add(new TotalFormaPagamento(forma, daForma.size(), valor, percentual(valor, totalGeral)));
        }
        return totais;
    }

    /** Variação percentual de {@code atual} em relação a {@code anterior}; nula se não há base de comparação. */
    static BigDecimal variacao(BigDecimal atual, BigDecimal anterior) {
        if (anterior.signum() == 0) {
            return null;
        }
        return atual.subtract(anterior).multiply(BigDecimal.valueOf(100)).divide(anterior, 1, RoundingMode.HALF_UP);
    }

    private static Ponto ponto(String chave, String rotulo, List<VendaResumida> vendas) {
        return new Ponto(chave, rotulo, vendas.size(), somar(vendas));
    }

    private static <K> Map<K, List<VendaResumida>> agrupar(List<VendaResumida> vendas, Function<VendaResumida, K> chave) {
        Map<K, List<VendaResumida>> grupos = new LinkedHashMap<>();
        vendas.forEach(venda -> grupos.computeIfAbsent(chave.apply(venda), k -> new ArrayList<>()).add(venda));
        return grupos;
    }

    private static BigDecimal somar(List<VendaResumida> vendas) {
        return vendas.stream().map(VendaResumida::total).reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2, RoundingMode.HALF_UP);
    }

    private static BigDecimal dividir(BigDecimal valor, long quantidade) {
        return quantidade == 0
                ? BigDecimal.ZERO.setScale(2)
                : valor.divide(BigDecimal.valueOf(quantidade), 2, RoundingMode.HALF_UP);
    }

    private static BigDecimal percentual(BigDecimal parte, BigDecimal total) {
        return total.signum() == 0
                ? BigDecimal.ZERO.setScale(1)
                : parte.multiply(BigDecimal.valueOf(100)).divide(total, 1, RoundingMode.HALF_UP);
    }
}
