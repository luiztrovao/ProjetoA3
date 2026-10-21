package com.pontoesabor.lanchonete.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.pontoesabor.lanchonete.model.Categoria;
import com.pontoesabor.lanchonete.model.FormaPagamento;
import com.pontoesabor.lanchonete.model.Perfil;
import com.pontoesabor.lanchonete.model.Produto;
import com.pontoesabor.lanchonete.model.StatusVenda;
import com.pontoesabor.lanchonete.model.Usuario;
import com.pontoesabor.lanchonete.model.Venda;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;

/** Testa a persistência no banco criado pelas migrations do Flyway. */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class VendaRepositoryTest {

    @Autowired
    private VendaRepository vendaRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private CategoriaRepository categoriaRepository;

    @Autowired
    private ProdutoRepository produtoRepository;

    @Test
    void migrationsCriamCategoriasDoPrototipo() {
        assertThat(categoriaRepository.findByAtivoTrueOrderByNome())
                .extracting(Categoria::getNome)
                .containsExactly("Acompanhamentos", "Bebidas", "Combos", "Hambúrgueres", "Sobremesas");
    }

    @Test
    void salvaVendaComItensEBuscaPorPeriodo() {
        Usuario vendedor = usuarioRepository.save(
                new Usuario("Ana", "ana", "hash", Perfil.VENDEDOR));
        Categoria lanches = categoriaRepository.findByAtivoTrueOrderByNome().stream()
                .filter(c -> c.getNome().equals("Hambúrgueres"))
                .findFirst()
                .orElseThrow();
        Produto xBurguer = produtoRepository.save(
                new Produto("PRD-TESTE", "X-Burguer", lanches, new BigDecimal("25.90"), 10, 2));

        Venda venda = new Venda(vendedor, null, FormaPagamento.DINHEIRO, LocalDateTime.of(2026, 10, 14, 12, 0));
        venda.adicionarItem(xBurguer, 2);
        venda.concluir();
        vendaRepository.saveAndFlush(venda);

        List<Venda> encontradas = vendaRepository.findByStatusAndDataHoraBetween(
                StatusVenda.CONCLUIDA,
                LocalDateTime.of(2026, 10, 14, 0, 0),
                LocalDateTime.of(2026, 10, 14, 23, 59));

        assertThat(encontradas).hasSize(1);
        assertThat(encontradas.getFirst().getTotal()).isEqualByComparingTo("51.80");
        assertThat(encontradas.getFirst().getItens()).hasSize(1);
        assertThat(produtoRepository.findById(xBurguer.getId()).orElseThrow().getEstoqueAtual()).isEqualTo(8);
    }
}
