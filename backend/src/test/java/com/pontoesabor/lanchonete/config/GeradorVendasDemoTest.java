package com.pontoesabor.lanchonete.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.pontoesabor.lanchonete.dto.RelatorioDtos.PeriodoPico;
import com.pontoesabor.lanchonete.dto.RelatorioDtos.PicosResponse;
import com.pontoesabor.lanchonete.model.Categoria;
import com.pontoesabor.lanchonete.model.Cliente;
import com.pontoesabor.lanchonete.model.Perfil;
import com.pontoesabor.lanchonete.model.Produto;
import com.pontoesabor.lanchonete.model.Usuario;
import com.pontoesabor.lanchonete.repository.CategoriaRepository;
import com.pontoesabor.lanchonete.repository.ClienteRepository;
import com.pontoesabor.lanchonete.repository.ProdutoRepository;
import com.pontoesabor.lanchonete.repository.UsuarioRepository;
import com.pontoesabor.lanchonete.repository.VendaRepository;
import com.pontoesabor.lanchonete.service.RelatorioService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

/** O histórico fictício da apresentação precisa ter os picos esperados e respeitar as regras do sistema. */
@SpringBootTest
@Transactional
class GeradorVendasDemoTest {

    @Autowired
    private GeradorVendasDemo gerador;

    @Autowired
    private RelatorioService relatorioService;

    @Autowired
    private VendaRepository vendaRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private ProdutoRepository produtoRepository;

    @Autowired
    private CategoriaRepository categoriaRepository;

    @Test
    void geraHistoricoComPicosNoFimDeSemanaENasRefeicoes() {
        Categoria categoria = categoriaRepository.findByAtivoTrueOrderByNome().getFirst();
        List<Produto> produtos = produtoRepository.saveAll(List.of(
                new Produto("T1", "Lanche", categoria, new BigDecimal("20.00"), 5, 1),
                new Produto("T2", "Bebida", categoria, new BigDecimal("6.00"), 5, 1)));
        List<Usuario> equipe = usuarioRepository.saveAll(List.of(
                new Usuario("Dona", "dona", "hash", Perfil.DONO),
                new Usuario("Vendedor", "vendedor", "hash", Perfil.VENDEDOR)));
        List<Cliente> clientes = clienteRepository.saveAll(List.of(new Cliente("Cliente A"), new Cliente("Cliente B")));
        // 4 semanas completas de segunda a domingo e "agora" no meio do último dia
        LocalDate inicio = LocalDate.of(2026, 9, 7);
        LocalDateTime agora = LocalDateTime.of(2026, 10, 4, 15, 0);

        int geradas = gerador.gerar(inicio, agora, produtos, new int[] {3, 2}, equipe, clientes);

        assertThat(geradas).isGreaterThan(400);
        assertThat(vendaRepository.count()).isEqualTo(geradas);
        assertThat(vendaRepository.findAll()).allSatisfy(venda -> {
            assertThat(venda.getDataHora()).isBetween(inicio.atStartOfDay(), agora);
            if (venda.getUsuario().getPerfil() == Perfil.VENDEDOR) {
                assertThat(venda.getDesconto()).isZero();
            }
        });
        // O histórico não consome o estoque atual
        assertThat(produtoRepository.findById(produtos.getFirst().getId()).orElseThrow().getEstoqueAtual()).isEqualTo(5);

        PicosResponse picos = relatorioService.picos(PeriodoPico.MES, LocalDate.of(2026, 9, 15), null);
        assertThat(picos.diaDaSemanaPico().rotulo()).isIn("Sexta-feira", "Sábado");
        assertThat(picos.horaPico().chave()).isIn("12", "19", "20");
    }

    @Test
    void sorteioRespeitaOsPesos() {
        Random aleatorio = new Random(1);
        int[] contagem = new int[2];
        for (int i = 0; i < 10_000; i++) {
            contagem[GeradorVendasDemo.sortear(new int[] {9, 1}, aleatorio)]++;
        }

        assertThat(contagem[0]).isBetween(8_700, 9_300);
    }
}
