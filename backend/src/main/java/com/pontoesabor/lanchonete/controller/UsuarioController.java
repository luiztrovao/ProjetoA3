package com.pontoesabor.lanchonete.controller;

import com.pontoesabor.lanchonete.dto.PaginaResponse;
import com.pontoesabor.lanchonete.dto.UsuarioAtualizacaoRequest;
import com.pontoesabor.lanchonete.dto.UsuarioCriacaoRequest;
import com.pontoesabor.lanchonete.dto.UsuarioResponse;
import com.pontoesabor.lanchonete.model.Perfil;
import com.pontoesabor.lanchonete.service.UsuarioService;
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
@RequestMapping("/api/usuarios")
@Tag(name = "Usuários (funcionários)")
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping
    @Operation(summary = "Lista usuários com filtros e paginação")
    public PaginaResponse<UsuarioResponse> listar(
            @Parameter(description = "Busca por nome ou e-mail") @RequestParam(required = false) String termo,
            @RequestParam(required = false) Perfil perfil,
            @Parameter(description = "true = ativos, false = inativos, vazio = todos")
            @RequestParam(required = false) Boolean ativo,
            @ParameterObject @PageableDefault(size = 10, sort = "nome") Pageable pageable) {
        return usuarioService.listar(termo, perfil, ativo, pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Busca um usuário pelo id")
    public UsuarioResponse buscar(@PathVariable Long id) {
        return usuarioService.buscar(id);
    }

    @PostMapping
    @Operation(summary = "Cadastra um usuário")
    public ResponseEntity<UsuarioResponse> criar(@Valid @RequestBody UsuarioCriacaoRequest request) {
        UsuarioResponse criado = usuarioService.criar(request);
        return ResponseEntity.created(URI.create("/api/usuarios/" + criado.id())).body(criado);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualiza um usuário (senha opcional)")
    public UsuarioResponse atualizar(@PathVariable Long id, @Valid @RequestBody UsuarioAtualizacaoRequest request) {
        return usuarioService.atualizar(id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Inativa um usuário (exclusão lógica)")
    public ResponseEntity<Void> inativar(@PathVariable Long id) {
        usuarioService.inativar(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/ativar")
    @Operation(summary = "Reativa um usuário inativo")
    public ResponseEntity<Void> ativar(@PathVariable Long id) {
        usuarioService.ativar(id);
        return ResponseEntity.noContent().build();
    }
}
