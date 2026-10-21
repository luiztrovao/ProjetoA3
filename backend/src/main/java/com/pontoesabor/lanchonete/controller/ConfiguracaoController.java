package com.pontoesabor.lanchonete.controller;

import com.pontoesabor.lanchonete.dto.ConfiguracaoDtos.ConfiguracaoResponse;
import com.pontoesabor.lanchonete.dto.ConfiguracaoDtos.EmpresaDto;
import com.pontoesabor.lanchonete.dto.ConfiguracaoDtos.PreferenciasVendaDto;
import com.pontoesabor.lanchonete.security.Permissoes;
import com.pontoesabor.lanchonete.service.ConfiguracaoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/configuracoes")
@Tag(name = "Configurações")
public class ConfiguracaoController {

    private final ConfiguracaoService configuracaoService;

    public ConfiguracaoController(ConfiguracaoService configuracaoService) {
        this.configuracaoService = configuracaoService;
    }

    @GetMapping
    @Operation(summary = "Consulta os dados da lanchonete e as preferências de vendas")
    public ConfiguracaoResponse consultar() {
        return configuracaoService.consultar();
    }

    @PutMapping("/empresa")
    @Operation(summary = "Atualiza os dados da lanchonete")
    @PreAuthorize(Permissoes.DONO)
    public ConfiguracaoResponse atualizarEmpresa(@Valid @RequestBody EmpresaDto dados) {
        return configuracaoService.atualizarEmpresa(dados);
    }

    @PutMapping("/vendas")
    @Operation(summary = "Atualiza as preferências de vendas")
    @PreAuthorize(Permissoes.DONO)
    public ConfiguracaoResponse atualizarPreferenciasVenda(@Valid @RequestBody PreferenciasVendaDto dados) {
        return configuracaoService.atualizarPreferenciasVenda(dados);
    }
}
