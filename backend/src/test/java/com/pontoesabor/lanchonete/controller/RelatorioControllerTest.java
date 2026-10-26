package com.pontoesabor.lanchonete.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * Dashboard, Relatórios e picos de venda com vendas em datas conhecidas.
 * Semana de 12 a 18/10/2026 (segunda a domingo):
 * <pre>
 *   seg 12/10 10h  Fernanda  1 x X-Burger (18) + 1 x Refri (6)  = 24  PIX
 *   qua 14/10 12h  Fernanda  2 x X-Burger                       = 36  DINHEIRO
 *   qua 14/10 12h  Carlos    3 x Refri                          = 18  CRÉDITO
 *   sáb 17/10 19h  Carlos    2 x X-Burger + 2 x Refri           = 48  PIX
 *   qua 14/10 15h  Carlos    5 x X-Burger (CANCELADA, não conta)
 *   ter 06/10 20h  Fernanda  1 x X-Burger (semana anterior)     = 18  PIX
 * </pre>
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser(username = "1", roles = "GERENTE")
class RelatorioControllerTest {

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

    private Usuario fernanda;
    private Usuario carlos;
    private Produto xBurger;
    private Produto refri;

    @BeforeEach
    void setUp() {
        fernanda = usuarioRepository.save(new Usuario("Fernanda Souza", "fernanda", "hash", Perfil.VENDEDOR));
        carlos = usuarioRepository.save(new Usuario("Carlos Lima", "carlos", "hash", Perfil.GERENTE));
        Categoria categoria = categoriaRepository.findByAtivoTrueOrderByNome().getFirst();
        xBurger = produtoRepository.save(new Produto("XBG001", "X-Burger", categoria, new BigDecimal("18.00"), 1000, 5));
        refri = produtoRepository.save(new Produto("REF001", "Refrigerante", categoria, new BigDecimal("6.00"), 1000, 5));

        venda("2026-10-12T10:15", fernanda, FormaPagamento.PIX, 1, 1, false);
        venda("2026-10-14T12:05", fernanda, FormaPagamento.DINHEIRO, 2, 0, false);
        venda("2026-10-14T12:40", carlos, FormaPagamento.CREDITO, 0, 3, false);
        venda("2026-10-17T19:30", carlos, FormaPagamento.PIX, 2, 2, false);
        venda("2026-10-14T15:00", carlos, FormaPagamento.PIX, 5, 0, true);
        venda("2026-10-06T20:00", fernanda, FormaPagamento.PIX, 1, 0, false);
    }

    private Venda venda(String dataHora, Usuario vendedor, FormaPagamento forma, int burgers, int refris, boolean cancelar) {
        LocalDateTime momento = LocalDateTime.parse(dataHora);
        Venda venda = new Venda(vendedor, null, forma, momento);
        if (burgers > 0) {
            venda.adicionarItem(xBurger, burgers);
        }
        if (refris > 0) {
            venda.adicionarItem(refri, refris);
        }
        venda.confirmar(StatusVenda.CONCLUIDA, venda.getTotal(), true, true, momento);
        if (cancelar) {
            venda.cancelar("Teste", momento);
        }
        return vendaRepository.save(venda);
    }

    // ===== Picos de venda =====

    @Test
    void picosDaSemana() throws Exception {
        mockMvc.perform(get("/api/relatorios/picos").param("periodo", "SEMANA").param("referencia", "2026-10-14"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.inicio").value("2026-10-12"))
                .andExpect(jsonPath("$.fim").value("2026-10-18"))
                .andExpect(jsonPath("$.indicadores.faturamento").value(126.00))
                .andExpect(jsonPath("$.indicadores.pedidos").value(4))
                .andExpect(jsonPath("$.indicadores.ticketMedio").value(31.50))
                .andExpect(jsonPath("$.periodoAnterior.faturamento").value(18.00))
                .andExpect(jsonPath("$.variacaoFaturamento").value(600.0))
                .andExpect(jsonPath("$.granularidade").value("DIA"))
                .andExpect(jsonPath("$.serie", hasSize(7)))
                .andExpect(jsonPath("$.pico.chave").value("2026-10-14"))
                .andExpect(jsonPath("$.pico.faturamento").value(54.00))
                .andExpect(jsonPath("$.diaDaSemanaPico.rotulo").value("Quarta-feira"))
                .andExpect(jsonPath("$.horaPico.rotulo").value("12h"))
                .andExpect(jsonPath("$.porHora", hasSize(24)));
    }

    @Test
    void picosDoAnoSaoPorMes() throws Exception {
        mockMvc.perform(get("/api/relatorios/picos").param("periodo", "ANO").param("referencia", "2026-03-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.granularidade").value("MES"))
                .andExpect(jsonPath("$.serie", hasSize(12)))
                .andExpect(jsonPath("$.pico.chave").value("2026-10"))
                .andExpect(jsonPath("$.indicadores.pedidos").value(5));
    }

    @Test
    void picosFiltradosPorVendedor() throws Exception {
        mockMvc.perform(get("/api/relatorios/picos").param("periodo", "SEMANA").param("referencia", "2026-10-14")
                        .param("vendedorId", String.valueOf(carlos.getId())))
                .andExpect(jsonPath("$.indicadores.faturamento").value(66.00))
                .andExpect(jsonPath("$.pico.chave").value("2026-10-17"));
    }

    @Test
    void periodoSemVendasNaoTemPico() throws Exception {
        mockMvc.perform(get("/api/relatorios/picos").param("periodo", "MES").param("referencia", "2025-01-15"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.indicadores.pedidos").value(0))
                .andExpect(jsonPath("$.pico").doesNotExist())
                .andExpect(jsonPath("$.variacaoFaturamento").doesNotExist());
    }

    // ===== Relatório de período livre =====

    @Test
    void relatorioDaSemana() throws Exception {
        mockMvc.perform(get("/api/relatorios/resumo").param("inicio", "2026-10-12").param("fim", "2026-10-18"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.indicadores.faturamento").value(126.00))
                .andExpect(jsonPath("$.produtosVendidos").value(11))
                .andExpect(jsonPath("$.granularidade").value("DIA"))
                .andExpect(jsonPath("$.evolucao", hasSize(7)))
                .andExpect(jsonPath("$.maisVendidos[0].nome").value("Refrigerante"))
                .andExpect(jsonPath("$.maisVendidos[0].quantidade").value(6))
                .andExpect(jsonPath("$.maisVendidos[1].nome").value("X-Burger"))
                .andExpect(jsonPath("$.maisVendidos[1].faturamento").value(90.00))
                .andExpect(jsonPath("$.vendedores[0].nome").value("Carlos Lima"))
                .andExpect(jsonPath("$.vendedores[0].participacao").value(52.4))
                .andExpect(jsonPath("$.formasPagamento[?(@.formaPagamento == 'PIX')].valor").value(72.00));
    }

    @Test
    void relatorioDeUmDiaEhPorHoraEDeMesesEhPorMes() throws Exception {
        mockMvc.perform(get("/api/relatorios/resumo").param("inicio", "2026-10-14").param("fim", "2026-10-14"))
                .andExpect(jsonPath("$.granularidade").value("HORA"))
                .andExpect(jsonPath("$.evolucao", hasSize(24)))
                .andExpect(jsonPath("$.evolucao[12].pedidos").value(2));
        mockMvc.perform(get("/api/relatorios/resumo").param("inicio", "2026-07-01").param("fim", "2026-12-31"))
                .andExpect(jsonPath("$.granularidade").value("MES"))
                .andExpect(jsonPath("$.evolucao", hasSize(6)));
    }

    @Test
    void relatorioPorVendedor() throws Exception {
        mockMvc.perform(get("/api/relatorios/resumo").param("inicio", "2026-10-12").param("fim", "2026-10-18")
                        .param("vendedorId", String.valueOf(fernanda.getId())))
                .andExpect(jsonPath("$.indicadores.faturamento").value(60.00))
                .andExpect(jsonPath("$.produtosVendidos").value(4))
                .andExpect(jsonPath("$.vendedores", hasSize(1)));
    }

    @Test
    void recusaPeriodoInvalido() throws Exception {
        mockMvc.perform(get("/api/relatorios/resumo").param("inicio", "2026-10-18").param("fim", "2026-10-12"))
                .andExpect(status().isUnprocessableContent());
        mockMvc.perform(get("/api/relatorios/resumo").param("inicio", "2020-01-01").param("fim", "2026-10-12"))
                .andExpect(status().isUnprocessableContent());
        mockMvc.perform(get("/api/relatorios/picos").param("periodo", "QUINZENA"))
                .andExpect(status().isBadRequest());
    }

    // ===== Dashboard =====

    @Test
    void dashboardMostraHojeEOsUltimos7Dias() throws Exception {
        venda(LocalDateTime.now().withSecond(0).withNano(0).toString(), fernanda, FormaPagamento.PIX, 1, 1, false);

        mockMvc.perform(get("/api/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hoje.pedidos").value(1))
                .andExpect(jsonPath("$.hoje.faturamento").value(24.00))
                .andExpect(jsonPath("$.ultimos7Dias", hasSize(7)))
                .andExpect(jsonPath("$.produtos.cadastrados").value(2))
                .andExpect(jsonPath("$.pedidosRecentes", hasSize(5)))
                .andExpect(jsonPath("$.pedidosRecentes[0].vendedor.nome").value("Fernanda Souza"));
    }

    @Test
    @WithMockUser(username = "1", roles = "VENDEDOR")
    void vendedorNaoAcessaDashboardNemRelatorios() throws Exception {
        mockMvc.perform(get("/api/dashboard")).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/relatorios/resumo")).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/relatorios/picos").param("periodo", "SEMANA")).andExpect(status().isForbidden());
    }
}
