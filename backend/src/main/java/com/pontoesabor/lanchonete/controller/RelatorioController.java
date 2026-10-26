package com.pontoesabor.lanchonete.controller;

import com.pontoesabor.lanchonete.dto.RelatorioDtos.DashboardResponse;
import com.pontoesabor.lanchonete.dto.RelatorioDtos.PeriodoPico;
import com.pontoesabor.lanchonete.dto.RelatorioDtos.PicosResponse;
import com.pontoesabor.lanchonete.dto.RelatorioDtos.RelatorioResponse;
import com.pontoesabor.lanchonete.security.Permissoes;
import com.pontoesabor.lanchonete.service.RelatorioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Dashboard gerencial e Relatórios: Dono e Gerente (tabela de permissões do protótipo). */
@RestController
@Tag(name = "Dashboard e Relatórios")
@PreAuthorize(Permissoes.GESTAO)
public class RelatorioController {

    private final RelatorioService relatorioService;

    public RelatorioController(RelatorioService relatorioService) {
        this.relatorioService = relatorioService;
    }

    @GetMapping("/api/dashboard")
    @Operation(summary = "Indicadores de hoje, evolução dos últimos 7 dias e pedidos recentes")
    public DashboardResponse dashboard() {
        return relatorioService.dashboard();
    }

    @GetMapping("/api/relatorios/resumo")
    @Operation(summary = "Relatório de um período: indicadores, evolução, mais vendidos, vendedores e pagamentos")
    public RelatorioResponse relatorio(
            @Parameter(description = "Data inicial (aaaa-mm-dd); padrão: 6 dias antes da final")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @Parameter(description = "Data final (aaaa-mm-dd); padrão: hoje")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim,
            @RequestParam(required = false) Long vendedorId) {
        return relatorioService.relatorio(inicio, fim, vendedorId);
    }

    @GetMapping("/api/relatorios/picos")
    @Operation(summary = "Picos de venda na semana, no mês, no semestre ou no ano (com comparação ao período anterior)")
    public PicosResponse picos(
            @RequestParam PeriodoPico periodo,
            @Parameter(description = "Qualquer data dentro do período desejado (aaaa-mm-dd); padrão: hoje")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate referencia,
            @RequestParam(required = false) Long vendedorId) {
        return relatorioService.picos(periodo, referencia, vendedorId);
    }
}
