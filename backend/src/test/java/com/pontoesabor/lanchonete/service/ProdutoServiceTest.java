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

    @InjectMocks
    private ProdutoService produtoService;

    private static ProdutoRequest request(Long categoriaId, int estoqueAtual, int estoqueMinimo) {
        return new ProdutoRequest("X-Burguer", "Pão, carne e queijo", categoriaId,
                new BigDecimal("25.90"), estoqueAtual, estoqueMinimo);
    }

    @Test
    void criarProdutoNaCategoriaInformada() {
        Categoria lanches = new Categoria("Lanches");
        when(categoriaService.buscarAtiva(1L)).thenReturn(lanches);
        when(produtoRepository.save(any(Produto.class))).thenAnswer(invocacao -> invocacao.getArgument(0));

        ProdutoResponse resposta = produtoService.criar(request(1L, 3, 5));

        assertThat(resposta.categoria().nome()).isEqualTo("Lanches");
        assertThat(resposta.descricao()).isEqualTo("Pão, carne e queijo");
        assertThat(resposta.estoqueBaixo()).isTrue();
    }

    @Test
    void criarEmCategoriaInativaLancaExcecao() {
        when(categoriaService.buscarAtiva(1L)).thenThrow(new RegraNegocioException("A categoria está inativa."));

        assertThatThrownBy(() -> produtoService.criar(request(1L, 10, 2)))
                .isInstanceOf(RegraNegocioException.class);
        verify(produtoRepository, never()).save(any());
    }

    @Test
    void atualizarInexistenteLancaExcecao() {
        when(produtoRepository.findById(42L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> produtoService.atualizar(42L, request(1L, 10, 2)))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    void inativarEAtivarProduto() {
        Produto produto = new Produto("X-Burguer", new Categoria("Lanches"), new BigDecimal("25.90"), 10, 2);
        when(produtoRepository.findById(1L)).thenReturn(Optional.of(produto));

        produtoService.inativar(1L);
        assertThat(produto.isAtivo()).isFalse();

        produtoService.ativar(1L);
        assertThat(produto.isAtivo()).isTrue();
    }
}
