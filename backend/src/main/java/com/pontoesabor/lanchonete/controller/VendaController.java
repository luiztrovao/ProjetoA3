package com.pontoesabor.lanchonete.controller;

import com.pontoesabor.lanchonete.dto.PaginaResponse;
import com.pontoesabor.lanchonete.dto.VendaDtos.CancelamentoRequest;
import com.pontoesabor.lanchonete.dto.VendaDtos.VendaRequest;
import com.pontoesabor.lanchonete.dto.VendaDtos.VendaResponse;
import com.pontoesabor.lanchonete.model.FormaPagamento;
import com.pontoesabor.lanchonete.model.StatusVenda;
import com.pontoesabor.lanchonete.security.Permissoes;
import com.pontoesabor.lanchonete.security.UsuarioLogado;
import com.pontoesabor.lanchonete.service.VendaService;
import com.pontoesabor.lanchonete.service.VendaService.Filtro;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.time.LocalDate;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/vendas")
@Tag(name = "Vendas e Pedidos")
public class VendaController {

    private final VendaService vendaService;

    public VendaController(VendaService vendaService) {
        this.vendaService = vendaService;
    }

    @GetMapping
    @Operation(summary = "Lista pedidos com filtros (o Vendedor vê só os próprios)")
    public PaginaResponse<VendaResponse> listar(
            @Parameter(description = "Número do pedido, nome do cliente ou do vendedor")
            @RequestParam(required = false) String termo,
            @RequestParam(required = false) StatusVenda status,
            @RequestParam(required = false) Long vendedorId,
            @RequestParam(required = false) Long clienteId,
            @RequestParam(required = false) FormaPagamento formaPagamento,
            @Parameter(description = "Data inicial (aaaa-mm-dd)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @Parameter(description = "Data final (aaaa-mm-dd)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim,
            @ParameterObject @PageableDefault(size = 10, sort = "dataHora", direction = Sort.Direction.DESC)
            Pageable pageable) {
        Filtro filtro = new Filtro(termo, status, vendedorId, clienteId, formaPagamento, inicio, fim);
        return vendaService.listar(filtro, pageable, UsuarioLogado.id());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalhes de um pedido")
    public VendaResponse buscar(@PathVariable Long id) {
        return vendaService.buscar(id, UsuarioLogado.id());
    }

    @PostMapping
    @Operation(summary = "Confirma uma venda (estoque, pagamento e status inicial conforme as configurações)")
    public ResponseEntity<VendaResponse> confirmar(@Valid @RequestBody VendaRequest request) {
        VendaResponse venda = vendaService.confirmar(request, UsuarioLogado.id());
        return ResponseEntity.created(URI.create("/api/vendas/" + venda.id())).body(venda);
    }

    @PatchMapping("/{id}/concluir")
    @Operation(summary = "Marca um pedido em preparo como concluído")
    @PreAuthorize(Permissoes.GESTAO)
    public VendaResponse concluir(@PathVariable Long id) {
        return vendaService.concluir(id);
    }

    @PatchMapping("/{id}/cancelar")
    @Operation(summary = "Cancela um pedido (motivo obrigatório); os produtos voltam ao estoque")
    @PreAuthorize(Permissoes.GESTAO)
    public VendaResponse cancelar(@PathVariable Long id, @Valid @RequestBody CancelamentoRequest request) {
        return vendaService.cancelar(id, request.motivo());
    }
}
