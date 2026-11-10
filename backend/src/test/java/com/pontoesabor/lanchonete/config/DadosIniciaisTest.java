package com.pontoesabor.lanchonete.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.pontoesabor.lanchonete.model.Categoria;
import com.pontoesabor.lanchonete.model.FormaPagamento;
import com.pontoesabor.lanchonete.model.Perfil;
import com.pontoesabor.lanchonete.model.Produto;
import com.pontoesabor.lanchonete.model.StatusVenda;
import com.pontoesabor.lanchonete.model.Usuario;
import com.pontoesabor.lanchonete.model.Venda;
import com.pontoesabor.lanchonete.repository.CategoriaRepository;
import com.pontoesabor.lanchonete.repository.ClienteRepository;
import com.pontoesabor.lanchonete.repository.ImagemRepository;
import com.pontoesabor.lanchonete.repository.ProdutoRepository;
import com.pontoesabor.lanchonete.repository.UsuarioRepository;
import com.pontoesabor.lanchonete.repository.VendaRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

/** Nas subidas seguintes da API, as vendas de demonstração são completadas até o momento atual. */
@SpringBootTest
@Transactional
class DadosIniciaisTest {

    /** "Agora" dos testes: três dias depois da última venda. */
    private static final LocalDateTime AGORA = LocalDateTime.of(2026, 11, 10, 15, 0);

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private ProdutoRepository produtoRepository;

    @Autowired
    private CategoriaRepository categoriaRepository;

    @Autowired
    private ImagemRepository imagemRepository;

    @Autowired
    private VendaRepository vendaRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private GeradorVendasDemo geradorVendas;

    private DadosIniciais dadosIniciais() {
        Clock relogio = Clock.fixed(AGORA.toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
        return new DadosIniciais(usuarioRepository, clienteRepository, produtoRepository, categoriaRepository,
                imagemRepository, vendaRepository, passwordEncoder, geradorVendas, relogio,
                true, 18, "demo123", "", "");
    }

    @Test
    void completaAsVendasDesdeAUltimaEConcluiPedidosAntigos() {
        Categoria categoria = categoriaRepository.findByAtivoTrueOrderByNome().getFirst();
        Produto produto = produtoRepository.save(new Produto("XBG001", "X-Burger", categoria, new BigDecimal("18.00"), 5, 1));
        Usuario vendedor = usuarioRepository.save(new Usuario("Fernanda", "fernanda", "hash", Perfil.VENDEDOR));
        Usuario visitante = usuarioRepository.save(new Usuario("Visitante", "visitante", "hash", Perfil.VISITANTE));
        Usuario desligado = new Usuario("Rafael", "rafael", "hash", Perfil.VENDEDOR);
        desligado.setAtivo(false);
        usuarioRepository.save(desligado);
        LocalDateTime ultimaVenda = AGORA.minusDays(3).withHour(20);
        Venda emPreparo = new Venda(vendedor, null, FormaPagamento.PIX, ultimaVenda);
        emPreparo.adicionarItem(produto, 1);
        emPreparo.confirmar(StatusVenda.EM_PREPARO, null, false, false, ultimaVenda);
        Long idEmPreparo = vendaRepository.save(emPreparo).getId();

        dadosIniciais().complementarVendasDemonstracao();

        List<Venda> novas = vendaRepository.findAll().stream().filter(venda -> !venda.getId().equals(idEmPreparo)).toList();
        assertThat(novas).hasSizeGreaterThan(30).allSatisfy(venda -> {
            assertThat(venda.getDataHora()).isAfter(ultimaVenda).isBeforeOrEqualTo(AGORA);
            assertThat(venda.getUsuario().getId()).isNotIn(visitante.getId(), desligado.getId());
        });
        assertThat(novas).anyMatch(venda -> venda.getDataHora().toLocalDate().equals(AGORA.toLocalDate()));
        assertThat(vendaRepository.findById(idEmPreparo).orElseThrow().getStatus()).isEqualTo(StatusVenda.CONCLUIDA);
    }

    @Test
    void semVendasNoBancoNaoCompletaNada() {
        Categoria categoria = categoriaRepository.findByAtivoTrueOrderByNome().getFirst();
        produtoRepository.save(new Produto("XBG001", "X-Burger", categoria, new BigDecimal("18.00"), 5, 1));
        usuarioRepository.save(new Usuario("Matheus", "matheus", "hash", Perfil.DONO));

        dadosIniciais().complementarVendasDemonstracao();

        assertThat(vendaRepository.count()).isZero();
    }
}
