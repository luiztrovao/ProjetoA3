package com.pontoesabor.lanchonete.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.pontoesabor.lanchonete.model.Categoria;
import com.pontoesabor.lanchonete.model.Cliente;
import com.pontoesabor.lanchonete.model.Configuracao;
import com.pontoesabor.lanchonete.model.FormaPagamento;
import com.pontoesabor.lanchonete.model.Perfil;
import com.pontoesabor.lanchonete.model.Produto;
import com.pontoesabor.lanchonete.model.StatusVenda;
import com.pontoesabor.lanchonete.model.Usuario;
import com.pontoesabor.lanchonete.repository.CategoriaRepository;
import com.pontoesabor.lanchonete.repository.ClienteRepository;
import com.pontoesabor.lanchonete.repository.ConfiguracaoRepository;
import com.pontoesabor.lanchonete.repository.ProdutoRepository;
import com.pontoesabor.lanchonete.repository.UsuarioRepository;
import com.pontoesabor.lanchonete.security.TokenService;
import java.math.BigDecimal;
import java.util.EnumSet;
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

/** Telas Vendas e Pedidos de ponta a ponta, com tokens JWT reais de cada perfil. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class VendaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private ProdutoRepository produtoRepository;

    @Autowired
    private CategoriaRepository categoriaRepository;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private ConfiguracaoRepository configuracaoRepository;

    @Autowired
    private TokenService tokenService;

    private String tokenGerente;
    private String tokenVendedor;
    private String tokenOutroVendedor;
    private Produto xBurger;
    private Cliente cliente;

    @BeforeEach
    void setUp() {
        tokenGerente = token(new Usuario("Carlos Lima", "carlos", "hash", Perfil.GERENTE));
        tokenVendedor = token(new Usuario("Fernanda Souza", "fernanda", "hash", Perfil.VENDEDOR));
        tokenOutroVendedor = token(new Usuario("Rafael Mendes", "rafael", "hash", Perfil.VENDEDOR));
        Categoria hamburgueres = categoriaRepository.findByAtivoTrueOrderByNome().stream()
                .filter(c -> c.getNome().equals("Hambúrgueres")).findFirst().orElseThrow();
        xBurger = produtoRepository.save(new Produto("XBG001", "X-Burger", hamburgueres, new BigDecimal("18.00"), 10, 2));
        cliente = clienteRepository.save(new Cliente("João Silva"));
    }

    private String token(Usuario usuario) {
        return tokenService.gerar(usuarioRepository.save(usuario)).token();
    }

    private MockHttpServletRequestBuilder como(String token, MockHttpServletRequestBuilder requisicao) {
        return requisicao.header(HttpHeaders.AUTHORIZATION, "Bearer " + token).contentType(MediaType.APPLICATION_JSON);
    }

    private String venda(int quantidade, String formaPagamento, String extra) {
        return """
                {"itens": [{"produtoId": %d, "quantidade": %d}], "formaPagamento": "%s"%s}
                """.formatted(xBurger.getId(), quantidade, formaPagamento, extra);
    }

    private long confirmar(String token, String corpo) throws Exception {
        String resposta = mockMvc.perform(como(token, post("/api/vendas")).content(corpo))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return Long.parseLong(resposta.replaceAll("^\\{\"id\":(\\d+).*", "$1"));
    }

    private void alterarPreferencias(boolean semCliente, StatusVenda statusInicial, EnumSet<FormaPagamento> formas) {
        Configuracao configuracao = configuracaoRepository.findById(Configuracao.ID_UNICO).orElseThrow();
        configuracao.atualizarPreferencias(true, true, 5, true, semCliente, statusInicial, formas);
    }

    // ===== Confirmação =====

    @Test
    void vendedorConfirmaVendaEEstoqueBaixa() throws Exception {
        mockMvc.perform(como(tokenVendedor, post("/api/vendas")).content(venda(2, "PIX", ", \"clienteId\": " + cliente.getId())))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.numero").value(org.hamcrest.Matchers.startsWith("#PV-")))
                .andExpect(jsonPath("$.status").value("EM_PREPARO"))
                .andExpect(jsonPath("$.cliente.nome").value("João Silva"))
                .andExpect(jsonPath("$.vendedor.nome").value("Fernanda Souza"))
                .andExpect(jsonPath("$.itens[0].codigo").value("XBG001"))
                .andExpect(jsonPath("$.quantidadeItens").value(2))
                .andExpect(jsonPath("$.total").value(36.00))
                .andExpect(jsonPath("$.confirmadaEm").isNotEmpty());

        assertThat(xBurger.getEstoqueAtual()).isEqualTo(8);
    }

    @Test
    void dinheiroCalculaTroco() throws Exception {
        mockMvc.perform(como(tokenVendedor, post("/api/vendas")).content(venda(1, "DINHEIRO", ", \"valorRecebido\": 20")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.valorRecebido").value(20))
                .andExpect(jsonPath("$.troco").value(2.00));
    }

    @Test
    void recusaValorRecebidoMenorQueOTotal() throws Exception {
        mockMvc.perform(como(tokenVendedor, post("/api/vendas")).content(venda(1, "DINHEIRO", ", \"valorRecebido\": 10")))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.mensagem").value("O valor recebido deve ser igual ou superior ao total do pedido."));
    }

    @Test
    void vendedorNaoDaDescontoMasGerenteSim() throws Exception {
        mockMvc.perform(como(tokenVendedor, post("/api/vendas")).content(venda(2, "PIX", ", \"desconto\": 5")))
                .andExpect(status().isForbidden());

        mockMvc.perform(como(tokenGerente, post("/api/vendas")).content(venda(2, "PIX", ", \"desconto\": 5")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.desconto").value(5))
                .andExpect(jsonPath("$.total").value(31.00));
    }

    @Test
    void recusaPedidoSemItens() throws Exception {
        mockMvc.perform(como(tokenVendedor, post("/api/vendas")).content("""
                        {"itens": [], "formaPagamento": "PIX"}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos[*].campo", hasItem("itens")));
    }

    @Test
    void recusaEstoqueInsuficiente() throws Exception {
        mockMvc.perform(como(tokenVendedor, post("/api/vendas")).content(venda(11, "PIX", "")))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.mensagem").value(org.hamcrest.Matchers.containsString("Estoque insuficiente")));
    }

    @Test
    void respeitaAsPreferenciasDeVendas() throws Exception {
        alterarPreferencias(false, StatusVenda.CONCLUIDA, EnumSet.of(FormaPagamento.PIX));

        mockMvc.perform(como(tokenVendedor, post("/api/vendas")).content(venda(1, "DEBITO", ", \"clienteId\": " + cliente.getId())))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.mensagem").value("Esta forma de pagamento foi desabilitada nas configurações."));
        mockMvc.perform(como(tokenVendedor, post("/api/vendas")).content(venda(1, "PIX", "")))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.mensagem").value("Selecione um cliente para revisar este pedido."));
        mockMvc.perform(como(tokenVendedor, post("/api/vendas")).content(venda(1, "PIX", ", \"clienteId\": " + cliente.getId())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("CONCLUIDA"));
    }

    // ===== Pedidos =====

    @Test
    void vendedorVeSoOsProprios() throws Exception {
        long daFernanda = confirmar(tokenVendedor, venda(1, "PIX", ""));
        long doRafael = confirmar(tokenOutroVendedor, venda(1, "PIX", ""));

        mockMvc.perform(como(tokenVendedor, get("/api/vendas")))
                .andExpect(jsonPath("$.totalElementos").value(1))
                .andExpect(jsonPath("$.conteudo[0].id").value(daFernanda));
        mockMvc.perform(como(tokenVendedor, get("/api/vendas/" + doRafael))).andExpect(status().isForbidden());
        mockMvc.perform(como(tokenGerente, get("/api/vendas"))).andExpect(jsonPath("$.totalElementos").value(2));
    }

    @Test
    void filtraPorStatusENumero() throws Exception {
        long primeiro = confirmar(tokenVendedor, venda(1, "PIX", ""));
        long segundo = confirmar(tokenVendedor, venda(1, "PIX", ""));
        mockMvc.perform(como(tokenGerente, patch("/api/vendas/" + segundo + "/concluir")));

        mockMvc.perform(como(tokenGerente, get("/api/vendas").param("status", "CONCLUIDA")))
                .andExpect(jsonPath("$.totalElementos").value(1))
                .andExpect(jsonPath("$.conteudo[0].id").value(segundo));
        mockMvc.perform(como(tokenGerente, get("/api/vendas").param("termo", "#PV-%06d".formatted(primeiro))))
                .andExpect(jsonPath("$.conteudo[0].id").value(primeiro));
    }

    @Test
    void gerenteConcluiPedidoEVendedorNao() throws Exception {
        long id = confirmar(tokenVendedor, venda(1, "PIX", ""));

        mockMvc.perform(como(tokenVendedor, patch("/api/vendas/" + id + "/concluir"))).andExpect(status().isForbidden());
        mockMvc.perform(como(tokenGerente, patch("/api/vendas/" + id + "/concluir")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONCLUIDA"));
        mockMvc.perform(como(tokenGerente, patch("/api/vendas/" + id + "/concluir")))
                .andExpect(status().isUnprocessableContent());
    }

    @Test
    void cancelarExigeMotivoEDevolveEstoque() throws Exception {
        long id = confirmar(tokenVendedor, venda(3, "PIX", ""));
        assertThat(xBurger.getEstoqueAtual()).isEqualTo(7);

        mockMvc.perform(como(tokenGerente, patch("/api/vendas/" + id + "/cancelar")).content("{\"motivo\": \"\"}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(como(tokenVendedor, patch("/api/vendas/" + id + "/cancelar")).content("{\"motivo\": \"Desistiu\"}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(como(tokenGerente, patch("/api/vendas/" + id + "/cancelar")).content("{\"motivo\": \"Cliente desistiu\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELADA"))
                .andExpect(jsonPath("$.motivoCancelamento").value("Cliente desistiu"))
                .andExpect(jsonPath("$.canceladaEm").isNotEmpty());

        assertThat(xBurger.getEstoqueAtual()).isEqualTo(10);
    }

    @Test
    void pedidoInexistenteDevolve404() throws Exception {
        mockMvc.perform(como(tokenGerente, get("/api/vendas/999999"))).andExpect(status().isNotFound());
    }
}
