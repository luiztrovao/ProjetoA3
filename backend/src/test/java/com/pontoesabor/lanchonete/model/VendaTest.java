package com.pontoesabor.lanchonete.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.pontoesabor.lanchonete.exception.RegraNegocioException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Regras do fluxo de venda do protótipo: montagem, confirmação, conclusão e cancelamento. */
class VendaTest {

    private static final LocalDateTime AGORA = LocalDateTime.of(2026, 10, 26, 12, 30);

    private Produto xBurger;
    private Produto refrigerante;
    private Usuario vendedor;

    @BeforeEach
    void setUp() {
        xBurger = new Produto("XBG001", "X-Burger", new Categoria("Hambúrgueres"), new BigDecimal("18.00"), 10, 2);
        refrigerante = new Produto("REF001", "Refrigerante Lata", new Categoria("Bebidas"), new BigDecimal("6.00"), 3, 5);
        vendedor = new Usuario("Fernanda", "fernanda", "hash", Perfil.VENDEDOR);
    }

    private Venda venda(FormaPagamento forma) {
        return new Venda(vendedor, null, forma, AGORA);
    }

    private Venda confirmada(StatusVenda statusInicial) {
        Venda venda = venda(FormaPagamento.PIX);
        venda.adicionarItem(xBurger, 2);
        venda.confirmar(statusInicial, null, true, true, AGORA);
        return venda;
    }

    // ===== Montagem =====

    @Test
    void calculaSubtotalDescontoETotal() {
        Venda venda = venda(FormaPagamento.PIX);
        venda.adicionarItem(xBurger, 2);
        venda.adicionarItem(refrigerante, 2);
        venda.aplicarDesconto(new BigDecimal("2.00"));

        // 2 x 18 + 2 x 6 = 48; 48 - 2 = 46
        assertThat(venda.getSubtotal()).isEqualByComparingTo("48.00");
        assertThat(venda.getTotal()).isEqualByComparingTo("46.00");
        assertThat(venda.getStatus()).isNull();
    }

    @Test
    void itemGuardaOPrecoDoMomentoDaVenda() {
        Venda venda = venda(FormaPagamento.PIX);
        venda.adicionarItem(xBurger, 1);

        xBurger.setPreco(new BigDecimal("30.00"));

        assertThat(venda.getItens().getFirst().getPrecoUnitario()).isEqualByComparingTo("18.00");
    }

    @Test
    void naoAceitaProdutoInativo() {
        xBurger.setAtivo(false);

        assertThatThrownBy(() -> venda(FormaPagamento.PIX).adicionarItem(xBurger, 1))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("inativo");
    }

    @Test
    void descontoNaoPodePassarDoSubtotalNemSerNegativo() {
        Venda venda = venda(FormaPagamento.PIX);
        venda.adicionarItem(refrigerante, 1);

        assertThatThrownBy(() -> venda.aplicarDesconto(new BigDecimal("7.00"))).isInstanceOf(RegraNegocioException.class);
        assertThatThrownBy(() -> venda.aplicarDesconto(new BigDecimal("-1.00"))).isInstanceOf(RegraNegocioException.class);
    }

    // ===== Confirmação =====

    @Test
    void confirmarAplicaStatusInicialEBaixaEstoque() {
        Venda venda = confirmada(StatusVenda.EM_PREPARO);

        assertThat(venda.getStatus()).isEqualTo(StatusVenda.EM_PREPARO);
        assertThat(venda.getConfirmadaEm()).isEqualTo(AGORA);
        assertThat(xBurger.getEstoqueAtual()).isEqualTo(8);
        assertThat(venda.getItens().getFirst().getQuantidadeBaixadaEstoque()).isEqualTo(2);
    }

    @Test
    void statusInicialPodeSerConcluida() {
        assertThat(confirmada(StatusVenda.CONCLUIDA).getStatus()).isEqualTo(StatusVenda.CONCLUIDA);
    }

    @Test
    void confirmarSemItensLancaExcecao() {
        assertThatThrownBy(() -> venda(FormaPagamento.PIX).confirmar(StatusVenda.EM_PREPARO, null, true, true, AGORA))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessage("O pedido não possui produtos para confirmar.");
    }

    @Test
    void bloqueiaVendaSemEstoqueQuandoConfigurado() {
        Venda venda = venda(FormaPagamento.PIX);
        venda.adicionarItem(refrigerante, 5);

        assertThatThrownBy(() -> venda.confirmar(StatusVenda.EM_PREPARO, null, true, true, AGORA))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("Estoque insuficiente");
        assertThat(refrigerante.getEstoqueAtual()).isEqualTo(3);
    }

    @Test
    void semBloqueioVendeEBaixaSoOQueHaNoEstoque() {
        Venda venda = venda(FormaPagamento.PIX);
        venda.adicionarItem(refrigerante, 5);

        venda.confirmar(StatusVenda.EM_PREPARO, null, true, false, AGORA);

        assertThat(refrigerante.getEstoqueAtual()).isZero();
        assertThat(venda.getItens().getFirst().getQuantidadeBaixadaEstoque()).isEqualTo(3);
    }

    @Test
    void semControleDeEstoqueNaoMexeNoEstoque() {
        Venda venda = venda(FormaPagamento.PIX);
        venda.adicionarItem(refrigerante, 5);

        venda.confirmar(StatusVenda.EM_PREPARO, null, false, true, AGORA);

        assertThat(refrigerante.getEstoqueAtual()).isEqualTo(3);
    }

    @Test
    void dinheiroCalculaOTroco() {
        Venda venda = venda(FormaPagamento.DINHEIRO);
        venda.adicionarItem(xBurger, 1);

        venda.confirmar(StatusVenda.EM_PREPARO, new BigDecimal("20.00"), true, true, AGORA);

        assertThat(venda.getValorRecebido()).isEqualByComparingTo("20.00");
        assertThat(venda.getTroco()).isEqualByComparingTo("2.00");
    }

    @Test
    void dinheiroExigeValorRecebidoSuficiente() {
        Venda venda = venda(FormaPagamento.DINHEIRO);
        venda.adicionarItem(xBurger, 1);

        assertThatThrownBy(() -> venda.confirmar(StatusVenda.EM_PREPARO, new BigDecimal("10.00"), true, true, AGORA))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessage("O valor recebido deve ser igual ou superior ao total do pedido.");
        assertThat(xBurger.getEstoqueAtual()).isEqualTo(10);
    }

    @Test
    void outrasFormasNaoGuardamValorRecebido() {
        Venda venda = venda(FormaPagamento.CREDITO);
        venda.adicionarItem(xBurger, 1);

        venda.confirmar(StatusVenda.EM_PREPARO, new BigDecimal("50.00"), true, true, AGORA);

        assertThat(venda.getValorRecebido()).isNull();
        assertThat(venda.getTroco()).isNull();
    }

    @Test
    void vendaConfirmadaNaoPodeSerAlterada() {
        Venda venda = confirmada(StatusVenda.EM_PREPARO);

        assertThatThrownBy(() -> venda.adicionarItem(refrigerante, 1)).isInstanceOf(RegraNegocioException.class);
        assertThatThrownBy(() -> venda.confirmar(StatusVenda.EM_PREPARO, null, true, true, AGORA))
                .isInstanceOf(RegraNegocioException.class);
    }

    // ===== Conclusão e cancelamento =====

    @Test
    void concluirPedidoEmPreparo() {
        Venda venda = confirmada(StatusVenda.EM_PREPARO);

        venda.concluir();

        assertThat(venda.getStatus()).isEqualTo(StatusVenda.CONCLUIDA);
        assertThat(xBurger.getEstoqueAtual()).isEqualTo(8);
    }

    @Test
    void soPedidoEmPreparoPodeSerConcluido() {
        Venda venda = confirmada(StatusVenda.CONCLUIDA);

        assertThatThrownBy(venda::concluir)
                .isInstanceOf(RegraNegocioException.class)
                .hasMessage("Só pedidos em preparo podem ser concluídos.");
    }

    @Test
    void cancelarDevolveEstoqueEGuardaMotivo() {
        Venda venda = confirmada(StatusVenda.CONCLUIDA);

        venda.cancelar("  Cliente desistiu  ", AGORA.plusMinutes(5));

        assertThat(venda.getStatus()).isEqualTo(StatusVenda.CANCELADA);
        assertThat(venda.getMotivoCancelamento()).isEqualTo("Cliente desistiu");
        assertThat(venda.getCanceladaEm()).isEqualTo(AGORA.plusMinutes(5));
        assertThat(xBurger.getEstoqueAtual()).isEqualTo(10);
    }

    @Test
    void cancelarDevolveSoOQueFoiRetiradoDoEstoque() {
        Venda venda = venda(FormaPagamento.PIX);
        venda.adicionarItem(refrigerante, 5);
        venda.confirmar(StatusVenda.EM_PREPARO, null, true, false, AGORA);

        venda.cancelar("Erro no pedido", AGORA);

        assertThat(refrigerante.getEstoqueAtual()).isEqualTo(3);
    }

    @Test
    void cancelarExigeMotivo() {
        Venda venda = confirmada(StatusVenda.EM_PREPARO);

        assertThatThrownBy(() -> venda.cancelar("  ", AGORA))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessage("Informe o motivo do cancelamento.");
    }

    @Test
    void naoCancelaDuasVezesNemVendaNaoConfirmada() {
        Venda venda = confirmada(StatusVenda.EM_PREPARO);
        venda.cancelar("Motivo", AGORA);

        assertThatThrownBy(() -> venda.cancelar("Motivo", AGORA)).isInstanceOf(RegraNegocioException.class);
        assertThatThrownBy(() -> venda(FormaPagamento.PIX).cancelar("Motivo", AGORA))
                .isInstanceOf(RegraNegocioException.class);
    }

    @Test
    void numeroNoFormatoDoPrototipo() {
        assertThat(venda(FormaPagamento.PIX).getNumero()).isNull();
    }
}
