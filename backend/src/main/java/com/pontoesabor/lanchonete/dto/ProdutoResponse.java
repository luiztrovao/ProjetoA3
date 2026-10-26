package com.pontoesabor.lanchonete.dto;

import com.pontoesabor.lanchonete.model.Imagem;
import com.pontoesabor.lanchonete.model.Produto;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ProdutoResponse(
        Long id,
        String codigo,
        String nome,
        String descricao,
        CategoriaResponse categoria,
        BigDecimal preco,
        int estoqueAtual,
        int estoqueMinimo,
        boolean estoqueBaixo,
        String imagemUrl,
        boolean ativo,
        LocalDateTime criadoEm,
        LocalDateTime atualizadoEm) {

    public static ProdutoResponse de(Produto produto) {
        return new ProdutoResponse(
                produto.getId(),
                produto.getCodigo(),
                produto.getNome(),
                produto.getDescricao(),
                CategoriaResponse.de(produto.getCategoria()),
                produto.getPreco(),
                produto.getEstoqueAtual(),
                produto.getEstoqueMinimo(),
                produto.isEstoqueBaixo(),
                Imagem.url(produto.getImagemId()),
                produto.isAtivo(),
                produto.getCriadoEm(),
                produto.getAtualizadoEm());
    }
}
