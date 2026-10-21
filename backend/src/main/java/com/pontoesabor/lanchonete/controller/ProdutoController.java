package com.pontoesabor.lanchonete.controller;

import com.pontoesabor.lanchonete.dto.PaginaResponse;
import com.pontoesabor.lanchonete.dto.ProdutoRequest;
import com.pontoesabor.lanchonete.dto.ProdutoResponse;
import com.pontoesabor.lanchonete.security.Permissoes;
import com.pontoesabor.lanchonete.service.ProdutoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/produtos")
@Tag(name = "Produtos")
public class ProdutoController {

    private final ProdutoService produtoService;

    public ProdutoController(ProdutoService produtoService) {
        this.produtoService = produtoService;
    }

    @GetMapping
    @Operation(summary = "Lista produtos com filtros e paginação")
    public PaginaResponse<ProdutoResponse> listar(
            @Parameter(description = "Parte do nome") @RequestParam(required = false) String termo,
            @RequestParam(required = false) Long categoriaId,
            @Parameter(description = "true = ativos, false = inativos, vazio = todos")
            @RequestParam(required = false) Boolean ativo,
            @ParameterObject @PageableDefault(size = 10, sort = "nome") Pageable pageable) {
        return produtoService.listar(termo, categoriaId, ativo, pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Busca um produto pelo id")
    public ProdutoResponse buscar(@PathVariable Long id) {
        return produtoService.buscar(id);
    }

    @PostMapping
    @Operation(summary = "Cadastra um produto")
    @PreAuthorize(Permissoes.GESTAO)
    public ResponseEntity<ProdutoResponse> criar(@Valid @RequestBody ProdutoRequest request) {
        ProdutoResponse criado = produtoService.criar(request);
        return ResponseEntity.created(URI.create("/api/produtos/" + criado.id())).body(criado);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualiza um produto")
    @PreAuthorize(Permissoes.GESTAO)
    public ProdutoResponse atualizar(@PathVariable Long id, @Valid @RequestBody ProdutoRequest request) {
        return produtoService.atualizar(id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Inativa um produto (exclusão lógica)")
    @PreAuthorize(Permissoes.GESTAO)
    public ResponseEntity<Void> inativar(@PathVariable Long id) {
        produtoService.inativar(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/ativar")
    @Operation(summary = "Reativa um produto inativo")
    @PreAuthorize(Permissoes.GESTAO)
    public ResponseEntity<Void> ativar(@PathVariable Long id) {
        produtoService.ativar(id);
        return ResponseEntity.noContent().build();
    }
}
