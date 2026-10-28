package com.pontoesabor.lanchonete.controller;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.pontoesabor.lanchonete.model.Categoria;
import com.pontoesabor.lanchonete.model.FormaPagamento;
import com.pontoesabor.lanchonete.model.Perfil;
import com.pontoesabor.lanchonete.model.Produto;
import com.pontoesabor.lanchonete.model.StatusVenda;
import com.pontoesabor.lanchonete.model.Usuario;
import com.pontoesabor.lanchonete.model.Venda;
import com.pontoesabor.lanchonete.repository.CategoriaRepository;
import com.pontoesabor.lanchonete.repository.ProdutoRepository;
import com.pontoesabor.lanchonete.repository.UsuarioRepository;
import com.pontoesabor.lanchonete.repository.VendaRepository;
import com.pontoesabor.lanchonete.security.TokenService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

/**
 * Sino de notificações. Cenário: um produto com estoque baixo; um pedido da Fernanda em preparo há 40 min
 * (atrasado, o limite padrão é 20); outro há 5 min (no prazo); um pedido da Fernanda cancelado há 1 hora
 * e outro cancelado há 10 dias (fora da janela de 7 dias).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class NotificacaoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private ProdutoRepository produtoRepository;

    @Autowired
    private CategoriaRepository categoriaRepository;

    @Autowired
    private VendaRepository vendaRepository;

    @Autowired
    private TokenService tokenService;

    private String tokenGerente;
    private String tokenFernanda;
    private String tokenRafael;

    @BeforeEach
    void setUp() {
        Usuario gerente = usuarioRepository.save(new Usuario("Carlos", "carlos", "hash", Perfil.GERENTE));
        Usuario fernanda = usuarioRepository.save(new Usuario("Fernanda", "fernanda", "hash", Perfil.VENDEDOR));
        Usuario rafael = usuarioRepository.save(new Usuario("Rafael", "rafael", "hash", Perfil.VENDEDOR));
        tokenGerente = tokenService.gerar(gerente).token();
        tokenFernanda = tokenService.gerar(fernanda).token();
        tokenRafael = tokenService.gerar(rafael).token();

        Categoria categoria = categoriaRepository.findByAtivoTrueOrderByNome().getFirst();
        produtoRepository.save(new Produto("BAIXO1", "Refrigerante", categoria, new BigDecimal("6.00"), 2, 5));
        Produto lanche = produtoRepository.save(new Produto("OK0001", "X-Burger", categoria, new BigDecimal("18.00"), 500, 5));

        LocalDateTime agora = LocalDateTime.now();
        venda(fernanda, lanche, agora.minusMinutes(40), null);
        venda(fernanda, lanche, agora.minusMinutes(5), null);
        venda(fernanda, lanche, agora.minusHours(2), agora.minusHours(1));
        venda(fernanda, lanche, agora.minusDays(10), agora.minusDays(10));
    }

    private void venda(Usuario vendedor, Produto produto, LocalDateTime momento, LocalDateTime canceladaEm) {
        Venda venda = new Venda(vendedor, null, FormaPagamento.PIX, momento);
        venda.adicionarItem(produto, 1);
        venda.confirmar(StatusVenda.EM_PREPARO, null, true, true, momento);
        if (canceladaEm != null) {
            venda.cancelar("Cliente desistiu", canceladaEm);
        }
        vendaRepository.save(venda);
    }

    private MockHttpServletRequestBuilder como(String token, MockHttpServletRequestBuilder requisicao) {
        return requisicao.header(HttpHeaders.AUTHORIZATION, "Bearer " + token).contentType(MediaType.APPLICATION_JSON);
    }

    @Test
    void gerenteVeEstoqueBaixoEPedidoAtrasado() throws Exception {
        mockMvc.perform(como(tokenGerente, get("/api/notificacoes")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.naoLidas").value(2))
                .andExpect(jsonPath("$.itens", hasSize(2)))
                .andExpect(jsonPath("$.itens[?(@.tipo == 'ESTOQUE_BAIXO')].mensagem")
                        .value(org.hamcrest.Matchers.hasItem("Refrigerante está com 2 un. (mínimo 5).")))
                .andExpect(jsonPath("$.itens[?(@.tipo == 'PEDIDO_ATRASADO')].mensagem")
                        .value(org.hamcrest.Matchers.hasItem(containsString("em preparo há 40 min"))))
                .andExpect(jsonPath("$.itens[?(@.tipo == 'PEDIDO_CANCELADO')]", hasSize(0)));
    }

    @Test
    void vendedorVeSoOsProprioPedidosAtrasadosECancelados() throws Exception {
        mockMvc.perform(como(tokenFernanda, get("/api/notificacoes")))
                .andExpect(jsonPath("$.naoLidas").value(2))
                .andExpect(jsonPath("$.itens[?(@.tipo == 'PEDIDO_ATRASADO')]", hasSize(1)))
                .andExpect(jsonPath("$.itens[?(@.tipo == 'PEDIDO_CANCELADO')].mensagem")
                        .value(org.hamcrest.Matchers.hasItem(containsString("foi cancelado: Cliente desistiu"))))
                .andExpect(jsonPath("$.itens[?(@.tipo == 'ESTOQUE_BAIXO')]", hasSize(0)));

        mockMvc.perform(como(tokenRafael, get("/api/notificacoes")))
                .andExpect(jsonPath("$.naoLidas").value(0))
                .andExpect(jsonPath("$.itens", hasSize(0)));
    }

    @Test
    void marcarTodasComoLidasZeraOContador() throws Exception {
        mockMvc.perform(como(tokenGerente, post("/api/notificacoes/lidas"))).andExpect(status().isNoContent());

        mockMvc.perform(como(tokenGerente, get("/api/notificacoes")))
                .andExpect(jsonPath("$.naoLidas").value(0))
                .andExpect(jsonPath("$.itens", hasSize(2)))
                .andExpect(jsonPath("$.itens[*].lida", everyItem(org.hamcrest.Matchers.is(true))));
    }

    @Test
    void tempoDeAtrasoVemDasPreferenciasDeVendas() throws Exception {
        String preferencias = """
                {"controleEstoque": true, "bloquearSemEstoque": true, "alertaEstoqueBaixo": 5,
                 "confirmarCancelamento": true, "permitirVendaSemCliente": true,
                 "statusInicialVenda": "EM_PREPARO", "formasPagamento": ["PIX"], "minutosAlertaPreparo": %d}
                """;
        Usuario dono = usuarioRepository.save(new Usuario("Matheus", "matheus", "hash", Perfil.DONO));
        String tokenDono = tokenService.gerar(dono).token();

        mockMvc.perform(como(tokenDono, put("/api/configuracoes/vendas")).content(preferencias.formatted(60)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.preferenciasVenda.minutosAlertaPreparo").value(60));
        mockMvc.perform(como(tokenGerente, get("/api/notificacoes")))
                .andExpect(jsonPath("$.itens[?(@.tipo == 'PEDIDO_ATRASADO')]", hasSize(0)));

        mockMvc.perform(como(tokenDono, put("/api/configuracoes/vendas")).content(preferencias.formatted(2)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void semLoginDevolve401() throws Exception {
        mockMvc.perform(get("/api/notificacoes")).andExpect(status().isUnauthorized());
    }
}
