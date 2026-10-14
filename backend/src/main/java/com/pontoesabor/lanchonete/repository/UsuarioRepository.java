package com.pontoesabor.lanchonete.repository;

import com.pontoesabor.lanchonete.model.Perfil;
import com.pontoesabor.lanchonete.model.Usuario;
import java.util.Optional;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface UsuarioRepository extends JpaRepository<Usuario, Long>, JpaSpecificationExecutor<Usuario> {

    Optional<Usuario> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByEmailAndIdNot(String email, Long id);

    long countByPerfilAndAtivoTrue(Perfil perfil);

    /** Filtros da listagem de usuários. Parâmetros nulos são ignorados. */
    final class Filtros {

        private Filtros() {
        }

        public static Specification<Usuario> nomeOuEmailContem(String termo) {
            return (root, query, cb) -> {
                if (termo == null) {
                    return null;
                }
                String padrao = "%" + termo.toLowerCase() + "%";
                return cb.or(cb.like(cb.lower(root.get("nome")), padrao), cb.like(root.get("email"), padrao));
            };
        }

        public static Specification<Usuario> comPerfil(Perfil perfil) {
            return (root, query, cb) -> perfil == null ? null : cb.equal(root.get("perfil"), perfil);
        }

        public static Specification<Usuario> comAtivo(Boolean ativo) {
            return (root, query, cb) -> ativo == null ? null : cb.equal(root.get("ativo"), ativo);
        }
    }
}
