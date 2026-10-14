package com.pontoesabor.lanchonete.dto;

import com.pontoesabor.lanchonete.model.Categoria;

public record CategoriaResponse(Long id, String nome) {

    public static CategoriaResponse de(Categoria categoria) {
        return new CategoriaResponse(categoria.getId(), categoria.getNome());
    }
}
