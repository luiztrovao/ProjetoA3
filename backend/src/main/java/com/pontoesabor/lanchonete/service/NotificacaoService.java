package com.pontoesabor.lanchonete.service;

import com.pontoesabor.lanchonete.dto.NotificacaoDtos.Notificacao;
import com.pontoesabor.lanchonete.dto.NotificacaoDtos.NotificacoesResponse;
import com.pontoesabor.lanchonete.dto.NotificacaoDtos.TipoNotificacao;
import com.pontoesabor.lanchonete.exception.RecursoNaoEncontradoException;
import com.pontoesabor.lanchonete.model.Configuracao;
import com.pontoesabor.lanchonete.model.Perfil;
import com.pontoesabor.lanchonete.model.Produto;
import com.pontoesabor.lanchonete.model.StatusVenda;
import com.pontoesabor.lanchonete.model.Usuario;
import com.pontoesabor.lanchonete.model.Venda;
import com.pontoesabor.lanchonete.repository.ProdutoRepository;
import com.pontoesabor.lanchonete.repository.UsuarioRepository;
import com.pontoesabor.lanchonete.repository.VendaRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Avisos do sino, calculados na hora a partir do estoque e dos pedidos (não há uma tabela de mensagens
 * para manter sincronizada). O usuário só guarda o momento em que marcou tudo como lido.
 */
@Service
@Transactional(readOnly = true)
public class NotificacaoService {

    /** Quantos avisos aparecem na lista (os mais recentes). */
    static final int LIMITE = 30;

    /** Por quantos dias um cancelamento continua aparecendo para o Vendedor. */
    static final int DIAS_CANCELAMENTO = 7;

    private final UsuarioRepository usuarioRepository;
    private final ProdutoRepository produtoRepository;
    private final VendaRepository vendaRepository;
    private final ConfiguracaoService configuracaoService;
    private final Clock relogio;

    public NotificacaoService(
            UsuarioRepository usuarioRepository,
            ProdutoRepository produtoRepository,
            VendaRepository vendaRepository,
            ConfiguracaoService configuracaoService,
            Clock relogio) {
        this.usuarioRepository = usuarioRepository;
        this.produtoRepository = produtoRepository;
        this.vendaRepository = vendaRepository;
        this.configuracaoService = configuracaoService;
        this.relogio = relogio;
    }

    public NotificacoesResponse listar(Long idUsuario) {
        Usuario usuario = buscarUsuario(idUsuario);
        LocalDateTime agora = LocalDateTime.now(relogio);
        boolean gestao = usuario.getPerfil() != Perfil.VENDEDOR;
        List<Notificacao> avisos = new ArrayList<>();

        if (gestao) {
            produtoRepository.comEstoqueBaixo().forEach(produto -> avisos.add(estoqueBaixo(produto, usuario)));
        }
        Configuracao configuracao = configuracaoService.obter();
        LocalDateTime limiteAtraso = agora.minusMinutes(configuracao.getMinutosAlertaPreparo());
        List<Venda> atrasados = gestao
                ? vendaRepository.findTop20ByStatusAndDataHoraBeforeOrderByDataHoraAsc(StatusVenda.EM_PREPARO, limiteAtraso)
                : vendaRepository.findTop20ByStatusAndUsuarioIdAndDataHoraBeforeOrderByDataHoraAsc(
                        StatusVenda.EM_PREPARO, usuario.getId(), limiteAtraso);
        atrasados.forEach(venda -> avisos.add(pedidoAtrasado(venda, configuracao, agora, usuario)));
        if (!gestao) {
            vendaRepository.findTop20ByStatusAndUsuarioIdAndCanceladaEmAfterOrderByCanceladaEmDesc(
                            StatusVenda.CANCELADA, usuario.getId(), agora.minusDays(DIAS_CANCELAMENTO))
                    .forEach(venda -> avisos.add(pedidoCancelado(venda, usuario)));
        }

        long naoLidas = avisos.stream().filter(aviso -> !aviso.lida()).count();
        List<Notificacao> recentes = avisos.stream()
                .sorted(Comparator.comparing(Notificacao::data).reversed())
                .limit(LIMITE)
                .toList();
        return new NotificacoesResponse(naoLidas, recentes);
    }

    @Transactional
    public void marcarTodasComoLidas(Long idUsuario) {
        buscarUsuario(idUsuario).marcarNotificacoesComoLidas(LocalDateTime.now(relogio));
    }

    private static Notificacao estoqueBaixo(Produto produto, Usuario usuario) {
        String situacao = produto.getEstoqueAtual() == 0
                ? "está sem estoque"
                : "está com %d un. (mínimo %d)".formatted(produto.getEstoqueAtual(), produto.getEstoqueMinimo());
        LocalDateTime data = produto.getAtualizadoEm();
        return new Notificacao("estoque-" + produto.getId(), TipoNotificacao.ESTOQUE_BAIXO, "Estoque baixo",
                "%s %s.".formatted(produto.getNome(), situacao), data, lida(data, usuario),
                produto.getId(), produto.getCodigo());
    }

    private static Notificacao pedidoAtrasado(Venda venda, Configuracao configuracao, LocalDateTime agora, Usuario usuario) {
        long minutos = Duration.between(venda.getDataHora(), agora).toMinutes();
        LocalDateTime data = venda.getDataHora().plusMinutes(configuracao.getMinutosAlertaPreparo());
        return new Notificacao("atraso-" + venda.getId(), TipoNotificacao.PEDIDO_ATRASADO, "Pedido atrasado",
                "%s está em preparo há %s.".formatted(venda.getNumero(), tempo(minutos)), data, lida(data, usuario),
                venda.getId(), venda.getNumero());
    }

    private static Notificacao pedidoCancelado(Venda venda, Usuario usuario) {
        LocalDateTime data = venda.getCanceladaEm();
        return new Notificacao("cancelamento-" + venda.getId(), TipoNotificacao.PEDIDO_CANCELADO, "Pedido cancelado",
                "%s foi cancelado: %s".formatted(venda.getNumero(), venda.getMotivoCancelamento()), data,
                lida(data, usuario), venda.getId(), venda.getNumero());
    }

    /** Um aviso está lido se aconteceu até o momento em que o usuário marcou tudo como lido. */
    private static boolean lida(LocalDateTime data, Usuario usuario) {
        LocalDateTime lidasEm = usuario.getNotificacoesLidasEm();
        return lidasEm != null && data != null && !data.isAfter(lidasEm);
    }

    private static String tempo(long minutos) {
        return minutos < 60 ? minutos + " min" : "%d h %02d min".formatted(minutos / 60, minutos % 60);
    }

    private Usuario buscarUsuario(Long id) {
        return usuarioRepository.findById(id).orElseThrow(() -> new RecursoNaoEncontradoException("Usuário", id));
    }
}
