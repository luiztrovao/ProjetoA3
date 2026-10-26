package com.pontoesabor.lanchonete.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.pontoesabor.lanchonete.exception.RegraNegocioException;
import java.util.EnumSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ConfiguracaoTest {

    private final Configuracao configuracao = new Configuracao("Ponto & Sabor");

    @Test
    void padraoAceitaTodasAsFormasDePagamento() {
        assertThat(configuracao.getFormasPagamentoAceitas()).containsExactlyInAnyOrder(FormaPagamento.values());
        assertThat(configuracao.isControleEstoque()).isTrue();
        assertThat(configuracao.getStatusInicialVenda()).isEqualTo(StatusVenda.EM_PREPARO);
    }

    @Test
    void atualizaFormasDePagamento() {
        configuracao.atualizarPreferencias(true, true, 3, true, false, StatusVenda.CONCLUIDA,
                EnumSet.of(FormaPagamento.PIX, FormaPagamento.DINHEIRO));

        assertThat(configuracao.getFormasPagamentoAceitas())
                .containsExactlyInAnyOrder(FormaPagamento.PIX, FormaPagamento.DINHEIRO);
        assertThat(configuracao.getAlertaEstoqueBaixo()).isEqualTo(3);
        assertThat(configuracao.isPermitirVendaSemCliente()).isFalse();
        assertThat(configuracao.getStatusInicialVenda()).isEqualTo(StatusVenda.CONCLUIDA);
    }

    @Test
    void desligarControleDeEstoqueDesligaOBloqueio() {
        configuracao.atualizarPreferencias(false, true, 5, true, true, StatusVenda.EM_PREPARO,
                EnumSet.allOf(FormaPagamento.class));

        assertThat(configuracao.isControleEstoque()).isFalse();
        assertThat(configuracao.isBloquearSemEstoque()).isFalse();
    }

    @Test
    void exigeAoMenosUmaFormaDePagamento() {
        assertThatThrownBy(() -> configuracao.atualizarPreferencias(true, true, 5, true, true,
                StatusVenda.EM_PREPARO, Set.of()))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("forma de pagamento");
    }

    @Test
    void statusInicialNaoPodeSerCancelada() {
        assertThatThrownBy(() -> configuracao.atualizarPreferencias(true, true, 5, true, true,
                StatusVenda.CANCELADA, EnumSet.allOf(FormaPagamento.class)))
                .isInstanceOf(RegraNegocioException.class);
    }

    @Test
    void alertaDeEstoqueNaoPodeSerNegativo() {
        assertThatThrownBy(() -> configuracao.atualizarPreferencias(true, true, -1, true, true,
                StatusVenda.EM_PREPARO, EnumSet.allOf(FormaPagamento.class)))
                .isInstanceOf(RegraNegocioException.class);
    }
}
