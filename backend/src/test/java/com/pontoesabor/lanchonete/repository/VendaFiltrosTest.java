package com.pontoesabor.lanchonete.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.pontoesabor.lanchonete.model.Categoria;
import com.pontoesabor.lanchonete.model.Cliente;
import com.pontoesabor.lanchonete.model.FormaPagamento;
import com.pontoesabor.lanchonete.model.Perfil;
import com.pontoesabor.lanchonete.model.Produto;
import com.pontoesabor.lanchonete.model.StatusVenda;
import com.pontoesabor.lanchonete.model.Usuario;
import com.pontoesabor.lanchonete.model.Venda;
import com.pontoesabor.lanchonete.repository.VendaRepository.Filtros;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.data.jpa.domain.Specification;

/**
 * Filtros da tela Pedidos (caixa-branca): cada decisão dos {@link Filtros}, com parâmetro nulo e preenchido.
 * Cenário: três pedidos em dias diferentes, de dois vendedores, um com cliente e um cancelado.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class VendaFiltrosTest {

    @Autowired
    private VendaRepository vendaRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private CategoriaRepository categoriaRepository;

    @Autowired
    private ProdutoRepository produtoRepository;

    private Usuario ana;
    private Usuario bruno;
    private Cliente camila;
    private Venda pedidoDia10;
    private Venda pedidoDia12;
    private Venda pedidoDia14;

    @BeforeEach
    void setUp() {
        ana = usuarioRepository.save(new Usuario("Ana Souza", "ana", "hash", Perfil.VENDEDOR));
        bruno = usuarioRepository.save(new Usuario("Bruno Lima", "bruno", "hash", Perfil.VENDEDOR));
        camila = clienteRepository.save(new Cliente("Camila Ferreira"));
        Categoria categoria = categoriaRepository.findByAtivoTrueOrderByNome().getFirst();
        Produto produto = produtoRepository.save(new Produto("FLT001", "Suco", categoria, new BigDecimal("8.00"), 100, 1));

        pedidoDia10 = venda(ana, camila, FormaPagamento.PIX, LocalDate.of(2026, 10, 10), produto, false);
        pedidoDia12 = venda(bruno, null, FormaPagamento.DINHEIRO, LocalDate.of(2026, 10, 12), produto, false);
        pedidoDia14 = venda(ana, null, FormaPagamento.PIX, LocalDate.of(2026, 10, 14), produto, true);
    }

    private Venda venda(Usuario vendedor, Cliente cliente, FormaPagamento forma, LocalDate dia, Produto produto, boolean cancelar) {
        LocalDateTime momento = dia.atTime(12, 0);
        Venda venda = new Venda(vendedor, cliente, forma, momento);
        venda.adicionarItem(produto, 1);
        venda.confirmar(StatusVenda.EM_PREPARO, forma == FormaPagamento.DINHEIRO ? new BigDecimal("10.00") : null,
                true, true, momento);
        if (cancelar) {
            venda.cancelar("Teste", momento.plusMinutes(5));
        }
        return vendaRepository.saveAndFlush(venda);
    }

    private List<Long> ids(Specification<Venda> filtro) {
        return vendaRepository.findAll(filtro).stream().map(Venda::getId).sorted().toList();
    }

    @Test
    void filtrosNulosNaoRestringem() {
        Specification<Venda> todos = Specification.allOf(List.of(Filtros.buscaGeral(null), Filtros.comStatus(null),
                Filtros.doVendedor(null), Filtros.doCliente(null), Filtros.comFormaPagamento(null),
                Filtros.noPeriodo(null, null)));

        assertThat(ids(todos)).contains(pedidoDia10.getId(), pedidoDia12.getId(), pedidoDia14.getId());
    }

    @Test
    void buscaPeloNomeDoClienteOuDoVendedorSemDiferenciarMaiusculas() {
        assertThat(ids(Filtros.buscaGeral("CAMILA"))).containsExactly(pedidoDia10.getId());
        assertThat(ids(Filtros.buscaGeral("bruno"))).containsExactly(pedidoDia12.getId());
    }

    @Test
    void buscaPeloNumeroDoPedidoComOuSemMascara() {
        assertThat(ids(Filtros.buscaGeral(pedidoDia12.getNumero()))).containsExactly(pedidoDia12.getId());
        assertThat(ids(Filtros.buscaGeral(String.valueOf(pedidoDia14.getId())))).containsExactly(pedidoDia14.getId());
    }

    @Test
    void numeroComMaisDe18DigitosBuscaSoPorNome() {
        // 19 dígitos estouram o Long: o filtro não tenta converter e procura só nos nomes (nenhum casa)
        assertThat(ids(Filtros.buscaGeral("1234567890123456789"))).isEmpty();
    }

    @Test
    void filtraPorStatusVendedorClienteEFormaDePagamento() {
        assertThat(ids(Filtros.comStatus(StatusVenda.CANCELADA))).containsExactly(pedidoDia14.getId());
        assertThat(ids(Filtros.doVendedor(ana.getId()))).containsExactly(pedidoDia10.getId(), pedidoDia14.getId());
        assertThat(ids(Filtros.doCliente(camila.getId()))).containsExactly(pedidoDia10.getId());
        assertThat(ids(Filtros.comFormaPagamento(FormaPagamento.DINHEIRO))).containsExactly(pedidoDia12.getId());
    }

    @Test
    void periodoIncluiODiaInteiroDoInicioEDoFim() {
        LocalDate dia12 = LocalDate.of(2026, 10, 12);
        assertThat(ids(Filtros.noPeriodo(dia12, dia12))).containsExactly(pedidoDia12.getId());
        assertThat(ids(Filtros.noPeriodo(LocalDate.of(2026, 10, 11), LocalDate.of(2026, 10, 13)))).containsExactly(pedidoDia12.getId());
    }

    @Test
    void periodoSoComInicioOuSoComFim() {
        assertThat(ids(Filtros.noPeriodo(LocalDate.of(2026, 10, 12), null))).containsExactly(pedidoDia12.getId(), pedidoDia14.getId());
        assertThat(ids(Filtros.noPeriodo(null, LocalDate.of(2026, 10, 12)))).containsExactly(pedidoDia10.getId(), pedidoDia12.getId());
    }
}
