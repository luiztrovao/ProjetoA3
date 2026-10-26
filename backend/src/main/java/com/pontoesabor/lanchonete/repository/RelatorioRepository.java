package com.pontoesabor.lanchonete.repository;

import com.pontoesabor.lanchonete.dto.RelatorioDtos.ProdutoVendido;
import com.pontoesabor.lanchonete.dto.RelatorioDtos.VendaResumida;
import com.pontoesabor.lanchonete.model.StatusVenda;
import com.pontoesabor.lanchonete.model.Venda;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

/**
 * Consultas dos relatórios. Todas consideram só vendas <b>não canceladas</b> com {@code data_hora} no intervalo
 * {@code [inicio, fim)}. Há uma versão "DoVendedor" de cada uma para o filtro por vendedor.
 */
public interface RelatorioRepository extends Repository<Venda, Long> {

    String VENDAS_VALIDAS = """
            from Venda v join v.usuario u
            where v.status <> :cancelada and v.dataHora >= :inicio and v.dataHora < :fim""";

    String SELECT_RESUMIDA = """
            select new com.pontoesabor.lanchonete.dto.RelatorioDtos$VendaResumida(
                v.id, v.dataHora, v.total, v.formaPagamento, u.id, u.nome)
            """;

    String SELECT_PRODUTO = """
            select new com.pontoesabor.lanchonete.dto.RelatorioDtos$ProdutoVendido(
                p.id, p.codigo, p.nome, sum(i.quantidade), sum(i.subtotal))
            from ItemVenda i join i.venda v join v.usuario u join i.produto p
            where v.status <> :cancelada and v.dataHora >= :inicio and v.dataHora < :fim""";

    String AGRUPAR_PRODUTO = " group by p.id, p.codigo, p.nome order by sum(i.quantidade) desc, sum(i.subtotal) desc";

    @Query(SELECT_RESUMIDA + VENDAS_VALIDAS)
    List<VendaResumida> vendasValidas(
            @Param("inicio") LocalDateTime inicio,
            @Param("fim") LocalDateTime fim,
            @Param("cancelada") StatusVenda cancelada);

    @Query(SELECT_RESUMIDA + VENDAS_VALIDAS + " and u.id = :vendedorId")
    List<VendaResumida> vendasValidasDoVendedor(
            @Param("inicio") LocalDateTime inicio,
            @Param("fim") LocalDateTime fim,
            @Param("cancelada") StatusVenda cancelada,
            @Param("vendedorId") Long vendedorId);

    @Query(SELECT_PRODUTO + AGRUPAR_PRODUTO)
    List<ProdutoVendido> produtosVendidos(
            @Param("inicio") LocalDateTime inicio,
            @Param("fim") LocalDateTime fim,
            @Param("cancelada") StatusVenda cancelada,
            Pageable limite);

    @Query(SELECT_PRODUTO + " and u.id = :vendedorId" + AGRUPAR_PRODUTO)
    List<ProdutoVendido> produtosVendidosDoVendedor(
            @Param("inicio") LocalDateTime inicio,
            @Param("fim") LocalDateTime fim,
            @Param("cancelada") StatusVenda cancelada,
            @Param("vendedorId") Long vendedorId,
            Pageable limite);

    @Query("select coalesce(sum(i.quantidade), 0) from ItemVenda i join i.venda v join v.usuario u"
            + " where v.status <> :cancelada and v.dataHora >= :inicio and v.dataHora < :fim")
    long quantidadeVendida(
            @Param("inicio") LocalDateTime inicio,
            @Param("fim") LocalDateTime fim,
            @Param("cancelada") StatusVenda cancelada);

    @Query("select coalesce(sum(i.quantidade), 0) from ItemVenda i join i.venda v join v.usuario u"
            + " where v.status <> :cancelada and v.dataHora >= :inicio and v.dataHora < :fim and u.id = :vendedorId")
    long quantidadeVendidaDoVendedor(
            @Param("inicio") LocalDateTime inicio,
            @Param("fim") LocalDateTime fim,
            @Param("cancelada") StatusVenda cancelada,
            @Param("vendedorId") Long vendedorId);

    /** Últimos pedidos (inclusive cancelados), para o card "Pedidos recentes" do Dashboard. */
    @Query("select v from Venda v order by v.dataHora desc, v.id desc")
    List<Venda> pedidosRecentes(Pageable limite);

    @Query("select count(p) from Produto p")
    long produtosCadastrados();

    @Query("select count(p) from Produto p where p.ativo = true")
    long produtosAtivos();

    @Query("select count(p) from Produto p where p.ativo = true and p.estoqueAtual <= p.estoqueMinimo")
    long produtosComEstoqueBaixo();
}
