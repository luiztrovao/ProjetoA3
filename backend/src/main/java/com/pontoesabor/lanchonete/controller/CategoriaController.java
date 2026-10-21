package com.pontoesabor.lanchonete.controller;

import com.pontoesabor.lanchonete.dto.CategoriaResponse;
import com.pontoesabor.lanchonete.service.CategoriaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/categorias")
@Tag(name = "Categorias")
public class CategoriaController {

    private final CategoriaService categoriaService;

    public CategoriaController(CategoriaService categoriaService) {
        this.categoriaService = categoriaService;
    }

    @GetMapping
    @Operation(summary = "Lista as categorias ativas")
    public List<CategoriaResponse> listar() {
        return categoriaService.listarAtivas();
    }
}
