package com.pontoesabor.lanchonete.repository;

import com.pontoesabor.lanchonete.model.Produto;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

public interface ProdutoRepository extends JpaRepository<Produto, Long>, JpaSpecificationExecutor<Produto> {

    List<Produto> findByAtivoTrueOrderByNome();

    boolean existsByCodigo(String codigo);

    /** Produtos ativos com estoque igual ou abaixo do mínimo (notificação de estoque baixo). */
    @Query("select p from Produto p where p.ativo = true and p.estoqueAtual <= p.estoqueMinimo order by p.estoqueAtual, p.nome")
    List<Produto> comEstoqueBaixo();

    boolean existsByCodigoAndIdNot(String codigo, Long id);

    /** Filtros da listagem de produtos. Parâmetros nulos são ignorados. */
    final class Filtros {

        private Filtros() {
        }

        /** Procura o termo no nome ou no código do produto. */
        public static Specification<Produto> nomeOuCodigoContem(String termo) {
            return (root, query, cb) -> {
                if (termo == null) {
                    return null;
                }
                String padrao = "%" + termo.toLowerCase() + "%";
                return cb.or(cb.like(cb.lower(root.get("nome")), padrao), cb.like(cb.lower(root.get("codigo")), padrao));
            };
        }

        public static Specification<Produto> daCategoria(Long categoriaId) {
            return (root, query, cb) -> categoriaId == null
                    ? null
                    : cb.equal(root.get("categoria").get("id"), categoriaId);
        }

        /** {@code true}: só produtos com estoque igual ou abaixo do mínimo. */
        public static Specification<Produto> comEstoqueBaixo(Boolean estoqueBaixo) {
            return (root, query, cb) -> !Boolean.TRUE.equals(estoqueBaixo)
                    ? null
                    : cb.le(root.<Integer>get("estoqueAtual"), root.<Integer>get("estoqueMinimo"));
        }

        public static Specification<Produto> comAtivo(Boolean ativo) {
            return (root, query, cb) -> ativo == null ? null : cb.equal(root.get("ativo"), ativo);
        }
    }
}
