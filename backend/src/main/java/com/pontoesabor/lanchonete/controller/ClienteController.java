package com.pontoesabor.lanchonete.controller;

import com.pontoesabor.lanchonete.dto.ClienteRequest;
import com.pontoesabor.lanchonete.dto.ClienteResponse;
import com.pontoesabor.lanchonete.dto.PaginaResponse;
import com.pontoesabor.lanchonete.service.ClienteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
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
@RequestMapping("/api/clientes")
@Tag(name = "Clientes")
public class ClienteController {

    private final ClienteService clienteService;

    public ClienteController(ClienteService clienteService) {
        this.clienteService = clienteService;
    }

    @GetMapping
    @Operation(summary = "Lista clientes com busca e paginação")
    public PaginaResponse<ClienteResponse> listar(
            @Parameter(description = "Busca por nome, CPF, telefone ou e-mail")
            @RequestParam(required = false) String termo,
            @Parameter(description = "true = ativos, false = inativos, vazio = todos")
            @RequestParam(required = false) Boolean ativo,
            @ParameterObject @PageableDefault(size = 10, sort = "nome") Pageable pageable) {
        return clienteService.listar(termo, ativo, pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Busca um cliente pelo id")
    public ClienteResponse buscar(@PathVariable Long id) {
        return clienteService.buscar(id);
    }

    @PostMapping
    @Operation(summary = "Cadastra um cliente")
    public ResponseEntity<ClienteResponse> criar(@Valid @RequestBody ClienteRequest request) {
        ClienteResponse criado = clienteService.criar(request);
        return ResponseEntity.created(URI.create("/api/clientes/" + criado.id())).body(criado);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualiza um cliente")
    public ClienteResponse atualizar(@PathVariable Long id, @Valid @RequestBody ClienteRequest request) {
        return clienteService.atualizar(id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Inativa um cliente (exclusão lógica)")
    public ResponseEntity<Void> inativar(@PathVariable Long id) {
        clienteService.inativar(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/ativar")
    @Operation(summary = "Reativa um cliente inativo")
    public ResponseEntity<Void> ativar(@PathVariable Long id) {
        clienteService.ativar(id);
        return ResponseEntity.noContent().build();
    }
}
