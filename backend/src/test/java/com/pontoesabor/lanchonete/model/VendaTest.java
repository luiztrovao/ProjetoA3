package com.pontoesabor.lanchonete.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.pontoesabor.lanchonete.exception.RegraNegocioException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class VendaTest {

    private Produto xBurguer;
    private Produto refrigerante;
    private Venda venda;

    @BeforeEach
    void setUp() {
        Categoria lanches = new Categoria("Lanches");
        Categoria bebidas = new Categoria("Bebidas");
        xBurguer = new Produto("X-Burguer", lanches, new BigDecimal("25.90"), 10, 2);
        refrigerante = new Produto("Refrigerante lata", bebidas, new BigDecimal("6.50"), 20, 5);
        Usuario vendedor = new Usuario("Ana", "ana@pontoesabor.com", "hash", Perfil.VENDEDOR);
        venda = new Venda(vendedor, null, FormaPagamento.PIX, LocalDateTime.of(2026, 10, 14, 12, 30));
    }

    @Test
    void novaVendaComecaEmPreparoEZerada() {
        assertThat(venda.getStatus()).isEqualTo(StatusVenda.EM_PREPARO);
        assertThat(venda.getTotal()).isEqualByComparingTo("0");
        assertThat(venda.getCliente()).isNull();
    }

    @Test
    void adicionarItensCalculaSubtotalETotal() {
        venda.adicionarItem(xBurguer, 2);
        venda.adicionarItem(refrigerante, 3);

        // 2 x 25,90 + 3 x 6,50 = 51,80 + 19,50 = 71,30
        assertThat(venda.getSubtotal()).isEqualByComparingTo("71.30");
        assertThat(venda.getTotal()).isEqualByComparingTo("71.30");
        assertThat(venda.getItens()).hasSize(2);
    }

    @Test
    void itemGuardaPrecoDoMomentoDaVenda() {
        venda.adicionarItem(xBurguer, 1);

        xBurguer.setPreco(new BigDecimal("30.00"));

        assertThat(venda.getItens().getFirst().getPrecoUnitario()).isEqualByComparingTo("25.90");
        assertThat(venda.getTotal()).isEqualByComparingTo("25.90");
    }

    @Test
    void descontoReduzOTotal() {
        venda.adicionarItem(xBurguer, 2);

        venda.aplicarDesconto(new BigDecimal("1.80"));

        assertThat(venda.getTotal()).isEqualByComparingTo("50.00");
    }

    @Test
    void descontoMaiorQueSubtotalLancaExcecao() {
        venda.adicionarItem(refrigerante, 1);

        assertThatThrownBy(() -> venda.aplicarDesconto(new BigDecimal("7.00")))
                .isInstanceOf(RegraNegocioException.class);
    }

    @Test
    void descontoNegativoLancaExcecao() {
        venda.adicionarItem(refrigerante, 1);

        assertThatThrownBy(() -> venda.aplicarDesconto(new BigDecimal("-1.00")))
                .isInstanceOf(RegraNegocioException.class);
    }

    @Test
    void concluirBaixaEstoque() {
        venda.adicionarItem(xBurguer, 2);
        venda.adicionarItem(refrigerante, 5);

        venda.concluir();

        assertThat(venda.getStatus()).isEqualTo(StatusVenda.CONCLUIDA);
        assertThat(xBurguer.getEstoqueAtual()).isEqualTo(8);
        assertThat(refrigerante.getEstoqueAtual()).isEqualTo(15);
    }

    @Test
    void concluirSemItensLancaExcecao() {
        assertThatThrownBy(venda::concluir)
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("sem itens");
    }

    @Test
    void naoPermiteAlterarVendaConcluida() {
        venda.adicionarItem(xBurguer, 1);
        venda.concluir();

        assertThatThrownBy(() -> venda.adicionarItem(refrigerante, 1)).isInstanceOf(RegraNegocioException.class);
    }

    @Test
    void cancelarVendaConcluidaDevolveEstoque() {
        venda.adicionarItem(xBurguer, 4);
        venda.concluir();

        venda.cancelar();

        assertThat(venda.getStatus()).isEqualTo(StatusVenda.CANCELADA);
        assertThat(xBurguer.getEstoqueAtual()).isEqualTo(10);
    }

    @Test
    void cancelarVendaEmPreparoNaoMexeNoEstoque() {
        venda.adicionarItem(xBurguer, 4);

        venda.cancelar();

        assertThat(venda.getStatus()).isEqualTo(StatusVenda.CANCELADA);
        assertThat(xBurguer.getEstoqueAtual()).isEqualTo(10);
    }

    @Test
    void cancelarDuasVezesLancaExcecao() {
        venda.cancelar();

        assertThatThrownBy(venda::cancelar).isInstanceOf(RegraNegocioException.class);
    }
}
