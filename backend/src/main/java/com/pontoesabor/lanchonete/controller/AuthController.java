package com.pontoesabor.lanchonete.controller;

import com.pontoesabor.lanchonete.dto.AuthDtos.AlterarSenhaRequest;
import com.pontoesabor.lanchonete.dto.AuthDtos.ContaRequest;
import com.pontoesabor.lanchonete.dto.AuthDtos.LoginRequest;
import com.pontoesabor.lanchonete.dto.AuthDtos.LoginResponse;
import com.pontoesabor.lanchonete.dto.UsuarioResponse;
import com.pontoesabor.lanchonete.security.UsuarioLogado;
import com.pontoesabor.lanchonete.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Login e Minha Conta")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/api/auth/login")
    @Operation(summary = "Entra com usuário ou e-mail e senha; devolve o token JWT")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @GetMapping("/api/conta")
    @Operation(summary = "Dados do usuário logado")
    public UsuarioResponse minhaConta() {
        return authService.minhaConta(UsuarioLogado.id());
    }

    @PutMapping("/api/conta")
    @Operation(summary = "Atualiza os dados do usuário logado (nome, usuário, e-mail e telefone)")
    public UsuarioResponse atualizarConta(@Valid @RequestBody ContaRequest request) {
        return authService.atualizarConta(UsuarioLogado.id(), request);
    }

    @PutMapping("/api/conta/senha")
    @Operation(summary = "Altera a senha do usuário logado")
    public ResponseEntity<Void> alterarSenha(@Valid @RequestBody AlterarSenhaRequest request) {
        authService.alterarSenha(UsuarioLogado.id(), request);
        return ResponseEntity.noContent().build();
    }
}
