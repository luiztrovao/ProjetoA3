package com.pontoesabor.lanchonete.service;

import static com.pontoesabor.lanchonete.repository.ProdutoRepository.Filtros.comAtivo;
import static com.pontoesabor.lanchonete.repository.ProdutoRepository.Filtros.daCategoria;
import static com.pontoesabor.lanchonete.repository.ProdutoRepository.Filtros.nomeContem;

import com.pontoesabor.lanchonete.dto.PaginaResponse;
import com.pontoesabor.lanchonete.dto.ProdutoRequest;
import com.pontoesabor.lanchonete.dto.ProdutoResponse;
import com.pontoesabor.lanchonete.exception.RecursoNaoEncontradoException;
import com.pontoesabor.lanchonete.model.Produto;
import com.pontoesabor.lanchonete.repository.ProdutoRepository;
import com.pontoesabor.lanchonete.util.Textos;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ProdutoService {

    private final ProdutoRepository produtoRepository;
    private final CategoriaService categoriaService;

    public ProdutoService(ProdutoRepository produtoRepository, CategoriaService categoriaService) {
        this.produtoRepository = produtoRepository;
        this.categoriaService = categoriaService;
    }

    public PaginaResponse<ProdutoResponse> listar(String termo, Long categoriaId, Boolean ativo, Pageable pageable) {
        Specification<Produto> filtro = Specification.allOf(List.of(
                nomeContem(Textos.limpar(termo)), daCategoria(categoriaId), comAtivo(ativo)));
        return PaginaResponse.de(produtoRepository.findAll(filtro, pageable).map(ProdutoResponse::de));
    }

    public ProdutoResponse buscar(Long id) {
        return ProdutoResponse.de(buscarEntidade(id));
    }

    @Transactional
    public ProdutoResponse criar(ProdutoRequest request) {
        Produto produto = new Produto(
                request.nome(),
                categoriaService.buscarAtiva(request.categoriaId()),
                request.preco(),
                request.estoqueAtual(),
                request.estoqueMinimo());
        produto.setDescricao(request.descricao());
        return ProdutoResponse.de(produtoRepository.save(produto));
    }

    @Transactional
    public ProdutoResponse atualizar(Long id, ProdutoRequest request) {
        Produto produto = buscarEntidade(id);
        if (!produto.getCategoria().getId().equals(request.categoriaId())) {
            produto.setCategoria(categoriaService.buscarAtiva(request.categoriaId()));
        }
        produto.setNome(request.nome());
        produto.setDescricao(request.descricao());
        produto.setPreco(request.preco());
        produto.setEstoqueAtual(request.estoqueAtual());
        produto.setEstoqueMinimo(request.estoqueMinimo());
        return ProdutoResponse.de(produtoRepository.saveAndFlush(produto));
    }

    /** Exclusão lógica: o produto some das vendas novas, mas continua no histórico. */
    @Transactional
    public void inativar(Long id) {
        buscarEntidade(id).setAtivo(false);
    }

    @Transactional
    public void ativar(Long id) {
        buscarEntidade(id).setAtivo(true);
    }

    private Produto buscarEntidade(Long id) {
        return produtoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Produto", id));
    }
}
