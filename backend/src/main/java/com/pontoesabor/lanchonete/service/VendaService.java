package com.pontoesabor.lanchonete.service;

import static com.pontoesabor.lanchonete.repository.VendaRepository.Filtros.buscaGeral;
import static com.pontoesabor.lanchonete.repository.VendaRepository.Filtros.comFormaPagamento;
import static com.pontoesabor.lanchonete.repository.VendaRepository.Filtros.comStatus;
import static com.pontoesabor.lanchonete.repository.VendaRepository.Filtros.doCliente;
import static com.pontoesabor.lanchonete.repository.VendaRepository.Filtros.doVendedor;
import static com.pontoesabor.lanchonete.repository.VendaRepository.Filtros.noPeriodo;

import com.pontoesabor.lanchonete.dto.PaginaResponse;
import com.pontoesabor.lanchonete.dto.VendaDtos.ItemRequest;
import com.pontoesabor.lanchonete.dto.VendaDtos.VendaRequest;
import com.pontoesabor.lanchonete.dto.VendaDtos.VendaResponse;
import com.pontoesabor.lanchonete.exception.RecursoNaoEncontradoException;
import com.pontoesabor.lanchonete.exception.RegraNegocioException;
import com.pontoesabor.lanchonete.model.Cliente;
import com.pontoesabor.lanchonete.model.Configuracao;
import com.pontoesabor.lanchonete.model.FormaPagamento;
import com.pontoesabor.lanchonete.model.Perfil;
import com.pontoesabor.lanchonete.model.Produto;
import com.pontoesabor.lanchonete.model.StatusVenda;
import com.pontoesabor.lanchonete.model.Usuario;
import com.pontoesabor.lanchonete.model.Venda;
import com.pontoesabor.lanchonete.repository.ClienteRepository;
import com.pontoesabor.lanchonete.repository.ProdutoRepository;
import com.pontoesabor.lanchonete.repository.UsuarioRepository;
import com.pontoesabor.lanchonete.repository.VendaRepository;
import com.pontoesabor.lanchonete.security.RespostasSeguranca;
import com.pontoesabor.lanchonete.util.Textos;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Telas Vendas (PDV) e Pedidos, com as regras do protótipo e das configurações. */
@Service
@Transactional(readOnly = true)
public class VendaService {

    private final VendaRepository vendaRepository;
    private final ProdutoRepository produtoRepository;
    private final ClienteRepository clienteRepository;
    private final UsuarioRepository usuarioRepository;
    private final ConfiguracaoService configuracaoService;
    private final Clock relogio;

    public VendaService(
            VendaRepository vendaRepository,
            ProdutoRepository produtoRepository,
            ClienteRepository clienteRepository,
            UsuarioRepository usuarioRepository,
            ConfiguracaoService configuracaoService,
            Clock relogio) {
        this.vendaRepository = vendaRepository;
        this.produtoRepository = produtoRepository;
        this.clienteRepository = clienteRepository;
        this.usuarioRepository = usuarioRepository;
        this.configuracaoService = configuracaoService;
        this.relogio = relogio;
    }

    /** Filtros da tela Pedidos. O Vendedor só enxerga os próprios pedidos. */
    public record Filtro(
            String termo,
            StatusVenda status,
            Long vendedorId,
            Long clienteId,
            FormaPagamento formaPagamento,
            LocalDate inicio,
            LocalDate fim) {
    }

    public PaginaResponse<VendaResponse> listar(Filtro filtro, Pageable pageable, Long idUsuarioLogado) {
        Usuario logado = buscarUsuario(idUsuarioLogado);
        Long vendedorId = logado.getPerfil() == Perfil.VENDEDOR ? logado.getId() : filtro.vendedorId();
        Specification<Venda> especificacao = Specification.allOf(List.of(
                buscaGeral(Textos.limpar(filtro.termo())),
                comStatus(filtro.status()),
                doVendedor(vendedorId),
                doCliente(filtro.clienteId()),
                comFormaPagamento(filtro.formaPagamento()),
                noPeriodo(filtro.inicio(), filtro.fim())));
        return PaginaResponse.de(vendaRepository.findAll(especificacao, pageable).map(VendaResponse::de));
    }

    public VendaResponse buscar(Long id, Long idUsuarioLogado) {
        Venda venda = buscarEntidade(id);
        exigirAcessoAoPedido(venda, buscarUsuario(idUsuarioLogado));
        return VendaResponse.de(venda);
    }

    /**
     * Confirma uma venda montada no PDV (tela "Revisar pedido"): aplica as preferências de vendas
     * (cliente obrigatório, formas de pagamento, controle de estoque e status inicial).
     */
    @Transactional
    public VendaResponse confirmar(VendaRequest request, Long idUsuarioLogado) {
        Usuario vendedor = buscarUsuario(idUsuarioLogado);
        Configuracao configuracao = configuracaoService.obter();
        if (vendedor.getPerfil() == Perfil.VENDEDOR && request.desconto().signum() > 0) {
            throw new AccessDeniedException(RespostasSeguranca.MENSAGEM_SEM_PERMISSAO);
        }
        if (!configuracao.getFormasPagamentoAceitas().contains(request.formaPagamento())) {
            throw new RegraNegocioException("Esta forma de pagamento foi desabilitada nas configurações.");
        }
        Cliente cliente = buscarCliente(request.clienteId(), configuracao);

        LocalDateTime agora = LocalDateTime.now(relogio);
        Venda venda = new Venda(vendedor, cliente, request.formaPagamento(), agora);
        agruparPorProduto(request.itens()).forEach((produtoId, quantidade) ->
                venda.adicionarItem(buscarProduto(produtoId), quantidade));
        venda.aplicarDesconto(request.desconto());
        venda.setObservacao(request.observacao());
        venda.confirmar(configuracao.getStatusInicialVenda(), request.valorRecebido(),
                configuracao.isControleEstoque(), configuracao.isBloquearSemEstoque(), agora);
        return VendaResponse.de(vendaRepository.saveAndFlush(venda));
    }

    @Transactional
    public VendaResponse concluir(Long id) {
        Venda venda = buscarEntidade(id);
        venda.concluir();
        return VendaResponse.de(venda);
    }

    @Transactional
    public VendaResponse cancelar(Long id, String motivo) {
        Venda venda = buscarEntidade(id);
        venda.cancelar(motivo, LocalDateTime.now(relogio));
        return VendaResponse.de(venda);
    }

    /** O mesmo produto pode vir em mais de uma linha: as quantidades são somadas, mantendo a ordem. */
    private static Map<Long, Integer> agruparPorProduto(List<ItemRequest> itens) {
        Map<Long, Integer> quantidades = new LinkedHashMap<>();
        itens.forEach(item -> quantidades.merge(item.produtoId(), item.quantidade(), Integer::sum));
        return quantidades;
    }

    private Cliente buscarCliente(Long clienteId, Configuracao configuracao) {
        if (clienteId == null) {
            if (!configuracao.isPermitirVendaSemCliente()) {
                throw new RegraNegocioException("Selecione um cliente para revisar este pedido.");
            }
            return null;
        }
        Cliente cliente = clienteRepository.findById(clienteId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Cliente", clienteId));
        if (!cliente.isAtivo()) {
            throw new RegraNegocioException("O cliente \"" + cliente.getNome() + "\" está inativo.");
        }
        return cliente;
    }

    private void exigirAcessoAoPedido(Venda venda, Usuario usuario) {
        boolean vendedorDeOutroPedido = usuario.getPerfil() == Perfil.VENDEDOR
                && !venda.getUsuario().getId().equals(usuario.getId());
        if (vendedorDeOutroPedido) {
            throw new AccessDeniedException(RespostasSeguranca.MENSAGEM_SEM_PERMISSAO);
        }
    }

    private Produto buscarProduto(Long id) {
        return produtoRepository.findById(id).orElseThrow(() -> new RecursoNaoEncontradoException("Produto", id));
    }

    private Usuario buscarUsuario(Long id) {
        return usuarioRepository.findById(id).orElseThrow(() -> new RecursoNaoEncontradoException("Usuário", id));
    }

    private Venda buscarEntidade(Long id) {
        return vendaRepository.findById(id).orElseThrow(() -> new RecursoNaoEncontradoException("Pedido", id));
    }
}
