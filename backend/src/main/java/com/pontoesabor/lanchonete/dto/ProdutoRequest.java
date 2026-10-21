package com.pontoesabor.lanchonete.dto;

import com.pontoesabor.lanchonete.util.Textos;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/**
 * Dados para cadastrar ou editar um produto. Obrigatórios: nome, código, categoria e preço.
 * Sem estoque informado, começa em 0; sem estoque mínimo, usa o "alerta de estoque baixo" das configurações.
 */
public record ProdutoRequest(
        @NotBlank(message = "O nome é obrigatório.")
        @Size(max = 120, message = "O nome deve ter no máximo 120 caracteres.")
        String nome,

        @NotBlank(message = "O código é obrigatório.")
        @Pattern(regexp = "[A-Z0-9-]{1,20}", message = "O código deve ter até 20 caracteres: letras, números ou hífen.")
        String codigo,

        @Size(max = 500, message = "A descrição deve ter no máximo 500 caracteres.")
        String descricao,

        @NotNull(message = "A categoria é obrigatória.")
        Long categoriaId,

        @NotNull(message = "O preço é obrigatório.")
        @DecimalMin(value = "0.01", message = "O preço deve ser maior que zero.")
        @Digits(integer = 8, fraction = 2, message = "O preço deve ter no máximo 2 casas decimais.")
        BigDecimal preco,

        @PositiveOrZero(message = "O estoque não pode ser negativo.")
        Integer estoqueAtual,

        @PositiveOrZero(message = "O estoque mínimo não pode ser negativo.")
        Integer estoqueMinimo,

        @Size(max = 500, message = "O endereço da imagem deve ter no máximo 500 caracteres.")
        String imagemUrl) {

    public ProdutoRequest {
        nome = Textos.limpar(nome);
        codigo = Textos.maiusculas(codigo);
        descricao = Textos.limpar(descricao);
        imagemUrl = Textos.limpar(imagemUrl);
    }
}
