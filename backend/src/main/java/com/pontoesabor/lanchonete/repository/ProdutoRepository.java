package com.pontoesabor.lanchonete.repository;

import com.pontoesabor.lanchonete.model.Produto;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ProdutoRepository extends JpaRepository<Produto, Long>, JpaSpecificationExecutor<Produto> {

    List<Produto> findByAtivoTrueOrderByNome();

    /** Filtros da listagem de produtos. Parâmetros nulos são ignorados. */
    final class Filtros {

        private Filtros() {
        }

        public static Specification<Produto> nomeContem(String termo) {
            return (root, query, cb) -> termo == null
                    ? null
                    : cb.like(cb.lower(root.get("nome")), "%" + termo.toLowerCase() + "%");
        }

        public static Specification<Produto> daCategoria(Long categoriaId) {
            return (root, query, cb) -> categoriaId == null
                    ? null
                    : cb.equal(root.get("categoria").get("id"), categoriaId);
        }

        public static Specification<Produto> comAtivo(Boolean ativo) {
            return (root, query, cb) -> ativo == null ? null : cb.equal(root.get("ativo"), ativo);
        }
    }
}
