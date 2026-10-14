package com.pontoesabor.lanchonete.dto;

import com.pontoesabor.lanchonete.util.Textos;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/** Dados para cadastrar ou editar um produto. */
public record ProdutoRequest(
        @NotBlank(message = "O nome é obrigatório.")
        @Size(max = 120, message = "O nome deve ter no máximo 120 caracteres.")
        String nome,

        @Size(max = 500, message = "A descrição deve ter no máximo 500 caracteres.")
        String descricao,

        @NotNull(message = "A categoria é obrigatória.")
        Long categoriaId,

        @NotNull(message = "O preço é obrigatório.")
        @DecimalMin(value = "0.01", message = "O preço deve ser maior que zero.")
        @Digits(integer = 8, fraction = 2, message = "O preço deve ter no máximo 2 casas decimais.")
        BigDecimal preco,

        @NotNull(message = "O estoque atual é obrigatório.")
        @PositiveOrZero(message = "O estoque atual não pode ser negativo.")
        Integer estoqueAtual,

        @NotNull(message = "O estoque mínimo é obrigatório.")
        @PositiveOrZero(message = "O estoque mínimo não pode ser negativo.")
        Integer estoqueMinimo) {

    public ProdutoRequest {
        nome = Textos.limpar(nome);
        descricao = Textos.limpar(descricao);
    }
}
