package com.pontoesabor.lanchonete.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pontoesabor.lanchonete.dto.ProdutoRequest;
import com.pontoesabor.lanchonete.dto.ProdutoResponse;
import com.pontoesabor.lanchonete.exception.RecursoNaoEncontradoException;
import com.pontoesabor.lanchonete.exception.RegraNegocioException;
import com.pontoesabor.lanchonete.model.Categoria;
import com.pontoesabor.lanchonete.model.Configuracao;
import com.pontoesabor.lanchonete.model.Produto;
import com.pontoesabor.lanchonete.repository.ProdutoRepository;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProdutoServiceTest {

    @Mock
    private ProdutoRepository produtoRepository;

    @Mock
    private CategoriaService categoriaService;

    @Mock
    private ConfiguracaoService configuracaoService;

    @InjectMocks
    private ProdutoService produtoService;

    private static ProdutoRequest request(Integer estoqueAtual, Integer estoqueMinimo) {
        return new ProdutoRequest("X-Burger", "xbg001", "Pão, carne e queijo", 1L,
                new BigDecimal("18.00"), estoqueAtual, estoqueMinimo);
    }

    @Test
    void criarProdutoComCodigoEmMaiusculas() {
        when(produtoRepository.existsByCodigo("XBG001")).thenReturn(false);
        when(categoriaService.buscarAtiva(1L)).thenReturn(new Categoria("Hambúrgueres"));
        when(produtoRepository.save(any(Produto.class))).thenAnswer(invocacao -> invocacao.getArgument(0));

        ProdutoResponse resposta = produtoService.criar(request(3, 5));

        assertThat(resposta.codigo()).isEqualTo("XBG001");
        assertThat(resposta.categoria().nome()).isEqualTo("Hambúrgueres");
        assertThat(resposta.estoqueBaixo()).isTrue();
    }

    @Test
    void semEstoqueMinimoUsaOAlertaDasConfiguracoes() {
        Configuracao configuracao = new Configuracao("Ponto & Sabor");
        when(produtoRepository.existsByCodigo("XBG001")).thenReturn(false);
        when(configuracaoService.obter()).thenReturn(configuracao);
        when(categoriaService.buscarAtiva(1L)).thenReturn(new Categoria("Hambúrgueres"));
        when(produtoRepository.save(any(Produto.class))).thenAnswer(invocacao -> invocacao.getArgument(0));

        ProdutoResponse resposta = produtoService.criar(request(null, null));

        assertThat(resposta.estoqueAtual()).isZero();
        assertThat(resposta.estoqueMinimo()).isEqualTo(configuracao.getAlertaEstoqueBaixo());
    }

    @Test
    void criarComCodigoRepetidoLancaExcecao() {
        when(produtoRepository.existsByCodigo("XBG001")).thenReturn(true);

        assertThatThrownBy(() -> produtoService.criar(request(10, 2)))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("código");
        verify(produtoRepository, never()).save(any());
    }

    @Test
    void criarEmCategoriaInativaLancaExcecao() {
        when(categoriaService.buscarAtiva(1L)).thenThrow(new RegraNegocioException("A categoria está inativa."));

        assertThatThrownBy(() -> produtoService.criar(request(10, 2)))
                .isInstanceOf(RegraNegocioException.class);
        verify(produtoRepository, never()).save(any());
    }

    @Test
    void atualizarSemEstoqueMantemValoresAtuais() {
        Categoria categoria = new Categoria("Hambúrgueres");
        Produto produto = new Produto("XBG001", "X-Burger", categoria, new BigDecimal("18.00"), 24, 6);
        when(produtoRepository.findById(1L)).thenReturn(Optional.of(produto));
        when(produtoRepository.saveAndFlush(produto)).thenReturn(produto);
        when(categoriaService.buscarAtiva(1L)).thenReturn(categoria);

        produtoService.atualizar(1L, request(null, null));

        assertThat(produto.getEstoqueAtual()).isEqualTo(24);
        assertThat(produto.getEstoqueMinimo()).isEqualTo(6);
    }

    @Test
    void atualizarInexistenteLancaExcecao() {
        when(produtoRepository.findById(42L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> produtoService.atualizar(42L, request(10, 2)))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    void inativarEAtivarProduto() {
        Produto produto = new Produto("XBG001", "X-Burger", new Categoria("Hambúrgueres"), new BigDecimal("18.00"), 10, 2);
        when(produtoRepository.findById(1L)).thenReturn(Optional.of(produto));

        produtoService.inativar(1L);
        assertThat(produto.isAtivo()).isFalse();

        produtoService.ativar(1L);
        assertThat(produto.isAtivo()).isTrue();
    }
}
