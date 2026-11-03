package com.pontoesabor.lanchonete.dto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.pontoesabor.lanchonete.dto.ConfiguracaoDtos.PreferenciasVendaDto;
import com.pontoesabor.lanchonete.dto.VendaDtos.ItemRequest;
import com.pontoesabor.lanchonete.dto.VendaDtos.VendaRequest;
import com.pontoesabor.lanchonete.model.Cliente;
import com.pontoesabor.lanchonete.model.Contato;
import com.pontoesabor.lanchonete.model.FormaPagamento;
import com.pontoesabor.lanchonete.model.Imagem;
import com.pontoesabor.lanchonete.model.StatusVenda;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;

/**
 * Regressão das correções da análise estática (SpotBugs EI_EXPOSE_REP/EI_EXPOSE_REP2): quem cria ou lê
 * um objeto não consegue alterar o estado guardado dentro dele.
 */
class CopiasDefensivasTest {

    @Test
    void imagemGuardaEDevolveCopiaDoArquivo() {
        byte[] enviado = {1, 2, 3};
        Imagem imagem = new Imagem(enviado, "image/png");

        enviado[0] = 9;
        imagem.getConteudo()[1] = 9;

        assertThat(imagem.getConteudo()).containsExactly(1, 2, 3);
    }

    @Test
    void requisicaoDeVendaCopiaOsItens() {
        List<ItemRequest> itens = new ArrayList<>(List.of(new ItemRequest(1L, 2)));
        VendaRequest venda = new VendaRequest(null, itens, null, FormaPagamento.PIX, null, null);

        itens.add(new ItemRequest(2L, 1));

        assertThat(venda.itens()).hasSize(1);
        assertThatThrownBy(() -> venda.itens().clear()).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void requisicaoSemItensChegaNulaAValidacao() {
        assertThat(new VendaRequest(null, null, null, FormaPagamento.PIX, null, null).itens()).isNull();
    }

    @Test
    void preferenciasCopiamAsFormasDePagamento() {
        Set<FormaPagamento> formas = EnumSet.of(FormaPagamento.PIX);
        PreferenciasVendaDto preferencias = preferencias(formas);

        formas.add(FormaPagamento.DINHEIRO);

        assertThat(preferencias.formasPagamento()).containsExactly(FormaPagamento.PIX);
        assertThat(preferencias(null).formasPagamento()).isNull();
    }

    @Test
    void paginaDevolveListaQueNaoPodeSerAlterada() {
        PaginaResponse<String> pagina = PaginaResponse.de(new PageImpl<>(new ArrayList<>(List.of("a", "b"))));

        assertThatThrownBy(() -> pagina.conteudo().add("c")).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void clienteSemContatoDevolveContatoVazio() {
        assertThat(new Cliente("Ana").getContato()).isEqualTo(Contato.VAZIO);
    }

    private static PreferenciasVendaDto preferencias(Set<FormaPagamento> formas) {
        return new PreferenciasVendaDto(true, true, 5, true, true, StatusVenda.EM_PREPARO, formas, 20);
    }
}
