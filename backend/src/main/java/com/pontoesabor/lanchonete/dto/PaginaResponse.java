package com.pontoesabor.lanchonete.dto;

import java.util.List;
import org.springframework.data.domain.Page;

/** Resposta paginada usada pelas listagens (tabelas com paginação no frontend). */
public record PaginaResponse<T>(
        List<T> conteudo,
        int pagina,
        int tamanho,
        long totalElementos,
        int totalPaginas) {

    public static <T> PaginaResponse<T> de(Page<T> page) {
        return new PaginaResponse<>(
                page.getContent(), page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
    }
}
