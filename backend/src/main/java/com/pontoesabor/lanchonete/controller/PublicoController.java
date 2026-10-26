package com.pontoesabor.lanchonete.controller;

import com.pontoesabor.lanchonete.dto.ConfiguracaoDtos.IdentidadeResponse;
import com.pontoesabor.lanchonete.model.Imagem;
import com.pontoesabor.lanchonete.service.ConfiguracaoService;
import com.pontoesabor.lanchonete.service.ImagemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.Duration;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/** Rotas abertas, sem login: usadas pela tela de login e pelas tags {@code <img>}. */
@RestController
@Tag(name = "Público")
public class PublicoController {

    private final ConfiguracaoService configuracaoService;
    private final ImagemService imagemService;

    public PublicoController(ConfiguracaoService configuracaoService, ImagemService imagemService) {
        this.configuracaoService = configuracaoService;
        this.imagemService = imagemService;
    }

    @GetMapping("/api/publico/identidade")
    @Operation(summary = "Nome e logo da lanchonete (exibidos na tela de login)")
    public IdentidadeResponse identidade() {
        return configuracaoService.identidade();
    }

    /** Cada troca gera uma imagem nova (novo id), então o navegador pode guardá-la em cache por muito tempo. */
    @GetMapping("/api/imagens/{id}")
    @Operation(summary = "Conteúdo de uma imagem (logo ou foto de produto)")
    public ResponseEntity<byte[]> imagem(@PathVariable Long id) {
        Imagem imagem = imagemService.buscar(id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(imagem.getTipoConteudo()))
                .cacheControl(CacheControl.maxAge(Duration.ofDays(365)).cachePublic().immutable())
                .body(imagem.getConteudo());
    }
}
