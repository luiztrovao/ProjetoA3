package com.pontoesabor.lanchonete.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.pontoesabor.lanchonete.exception.RegraNegocioException;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class ProdutoTest {

    private Produto produtoComEstoque(int atual, int minimo) {
        return new Produto("PRD-TESTE", "X-Burguer", new Categoria("Lanches"), new BigDecimal("25.90"), atual, minimo);
    }

    @Test
    void baixarEstoqueDiminuiQuantidade() {
        Produto produto = produtoComEstoque(10, 2);

        produto.baixarEstoque(3);

        assertThat(produto.getEstoqueAtual()).isEqualTo(7);
    }

    @Test
    void baixarEstoqueAlemDoDisponivelLancaExcecao() {
        Produto produto = produtoComEstoque(2, 1);

        assertThatThrownBy(() -> produto.baixarEstoque(3))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("Estoque insuficiente");
        assertThat(produto.getEstoqueAtual()).isEqualTo(2);
    }

    @Test
    void quantidadeZeroOuNegativaLancaExcecao() {
        Produto produto = produtoComEstoque(10, 2);

        assertThatThrownBy(() -> produto.baixarEstoque(0)).isInstanceOf(RegraNegocioException.class);
        assertThatThrownBy(() -> produto.devolverEstoque(-1)).isInstanceOf(RegraNegocioException.class);
    }

    @Test
    void devolverEstoqueAumentaQuantidade() {
        Produto produto = produtoComEstoque(5, 2);

        produto.devolverEstoque(2);

        assertThat(produto.getEstoqueAtual()).isEqualTo(7);
    }

    @Test
    void estoqueBaixoQuandoIgualOuAbaixoDoMinimo() {
        assertThat(produtoComEstoque(3, 3).isEstoqueBaixo()).isTrue();
        assertThat(produtoComEstoque(2, 3).isEstoqueBaixo()).isTrue();
        assertThat(produtoComEstoque(4, 3).isEstoqueBaixo()).isFalse();
    }
}
