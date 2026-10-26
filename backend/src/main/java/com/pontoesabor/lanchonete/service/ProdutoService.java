package com.pontoesabor.lanchonete.service;

import static com.pontoesabor.lanchonete.repository.ProdutoRepository.Filtros.comAtivo;
import static com.pontoesabor.lanchonete.repository.ProdutoRepository.Filtros.daCategoria;
import static com.pontoesabor.lanchonete.repository.ProdutoRepository.Filtros.nomeOuCodigoContem;

import com.pontoesabor.lanchonete.dto.PaginaResponse;
import com.pontoesabor.lanchonete.dto.ProdutoRequest;
import com.pontoesabor.lanchonete.dto.ProdutoResponse;
import com.pontoesabor.lanchonete.exception.RecursoNaoEncontradoException;
import com.pontoesabor.lanchonete.exception.RegraNegocioException;
import com.pontoesabor.lanchonete.model.Produto;
import com.pontoesabor.lanchonete.repository.ProdutoRepository;
import com.pontoesabor.lanchonete.util.Textos;
import java.util.List;
import java.util.Objects;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ProdutoService {

    private final ProdutoRepository produtoRepository;
    private final CategoriaService categoriaService;
    private final ConfiguracaoService configuracaoService;

    public ProdutoService(
            ProdutoRepository produtoRepository,
            CategoriaService categoriaService,
            ConfiguracaoService configuracaoService) {
        this.produtoRepository = produtoRepository;
        this.categoriaService = categoriaService;
        this.configuracaoService = configuracaoService;
    }

    public PaginaResponse<ProdutoResponse> listar(String termo, Long categoriaId, Boolean ativo, Pageable pageable) {
        Specification<Produto> filtro = Specification.allOf(List.of(
                nomeOuCodigoContem(Textos.limpar(termo)), daCategoria(categoriaId), comAtivo(ativo)));
        return PaginaResponse.de(produtoRepository.findAll(filtro, pageable).map(ProdutoResponse::de));
    }

    public ProdutoResponse buscar(Long id) {
        return ProdutoResponse.de(buscarEntidade(id));
    }

    @Transactional
    public ProdutoResponse criar(ProdutoRequest request) {
        if (produtoRepository.existsByCodigo(request.codigo())) {
            throw new RegraNegocioException("Já existe um produto cadastrado com este código.");
        }
        int estoqueMinimo = request.estoqueMinimo() != null
                ? request.estoqueMinimo()
                : configuracaoService.obter().getAlertaEstoqueBaixo();
        Produto produto = new Produto(
                request.codigo(),
                request.nome(),
                categoriaService.buscarAtiva(request.categoriaId()),
                request.preco(),
                request.estoqueAtual() != null ? request.estoqueAtual() : 0,
                estoqueMinimo);
        produto.setDescricao(request.descricao());
        produto.setImagemUrl(request.imagemUrl());
        return ProdutoResponse.de(produtoRepository.save(produto));
    }

    /** Campos de estoque não informados mantêm o valor atual. */
    @Transactional
    public ProdutoResponse atualizar(Long id, ProdutoRequest request) {
        Produto produto = buscarEntidade(id);
        if (produtoRepository.existsByCodigoAndIdNot(request.codigo(), id)) {
            throw new RegraNegocioException("Já existe um produto cadastrado com este código.");
        }
        if (!Objects.equals(produto.getCategoria().getId(), request.categoriaId())) {
            produto.setCategoria(categoriaService.buscarAtiva(request.categoriaId()));
        }
        produto.setCodigo(request.codigo());
        produto.setNome(request.nome());
        produto.setDescricao(request.descricao());
        produto.setPreco(request.preco());
        produto.setImagemUrl(request.imagemUrl());
        if (request.estoqueAtual() != null) {
            produto.setEstoqueAtual(request.estoqueAtual());
        }
        if (request.estoqueMinimo() != null) {
            produto.setEstoqueMinimo(request.estoqueMinimo());
        }
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
