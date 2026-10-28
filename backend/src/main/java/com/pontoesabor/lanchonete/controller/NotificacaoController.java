package com.pontoesabor.lanchonete.controller;

import com.pontoesabor.lanchonete.dto.NotificacaoDtos.NotificacoesResponse;
import com.pontoesabor.lanchonete.security.UsuarioLogado;
import com.pontoesabor.lanchonete.service.NotificacaoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notificacoes")
@Tag(name = "Notificações")
public class NotificacaoController {

    private final NotificacaoService notificacaoService;

    public NotificacaoController(NotificacaoService notificacaoService) {
        this.notificacaoService = notificacaoService;
    }

    @GetMapping
    @Operation(summary = "Avisos do usuário logado: estoque baixo, pedidos atrasados e cancelados")
    public NotificacoesResponse listar() {
        return notificacaoService.listar(UsuarioLogado.id());
    }

    @PostMapping("/lidas")
    @Operation(summary = "Marca todos os avisos atuais como lidos")
    public ResponseEntity<Void> marcarTodasComoLidas() {
        notificacaoService.marcarTodasComoLidas(UsuarioLogado.id());
        return ResponseEntity.noContent().build();
    }
}
