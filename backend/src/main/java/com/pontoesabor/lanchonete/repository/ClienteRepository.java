package com.pontoesabor.lanchonete.repository;

import com.pontoesabor.lanchonete.model.Cliente;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ClienteRepository extends JpaRepository<Cliente, Long>, JpaSpecificationExecutor<Cliente> {

    boolean existsByCpf(String cpf);

    boolean existsByCpfAndIdNot(String cpf, Long id);

    /** Filtros da listagem de clientes. Parâmetros nulos são ignorados. */
    final class Filtros {

        private Filtros() {
        }

        /** Procura o termo no nome, CPF, telefone ou e-mail (como a busca da tela de Cadastros). */
        public static Specification<Cliente> buscaGeral(String termo) {
            return (root, query, cb) -> {
                if (termo == null) {
                    return null;
                }
                String padrao = "%" + termo.toLowerCase() + "%";
                String digitos = termo.replaceAll("\\D", "");
                var nomeOuEmail = cb.or(
                        cb.like(cb.lower(root.get("nome")), padrao),
                        cb.like(cb.lower(root.get("email")), padrao));
                if (digitos.isEmpty()) {
                    return nomeOuEmail;
                }
                String padraoDigitos = "%" + digitos + "%";
                return cb.or(
                        nomeOuEmail,
                        cb.like(root.get("cpf"), padraoDigitos),
                        cb.like(root.get("telefone"), padraoDigitos));
            };
        }

        public static Specification<Cliente> comAtivo(Boolean ativo) {
            return (root, query, cb) -> ativo == null ? null : cb.equal(root.get("ativo"), ativo);
        }
    }
}
