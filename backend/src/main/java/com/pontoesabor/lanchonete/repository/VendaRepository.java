package com.pontoesabor.lanchonete.repository;

import com.pontoesabor.lanchonete.model.FormaPagamento;
import com.pontoesabor.lanchonete.model.StatusVenda;
import com.pontoesabor.lanchonete.model.Venda;
import jakarta.persistence.criteria.JoinType;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface VendaRepository extends JpaRepository<Venda, Long>, JpaSpecificationExecutor<Venda> {

    List<Venda> findByStatusAndDataHoraBetween(StatusVenda status, LocalDateTime inicio, LocalDateTime fim);

    /** Filtros da tela Pedidos. Parâmetros nulos são ignorados. */
    final class Filtros {

        private Filtros() {
        }

        /** Número do pedido (ex.: "123" ou "#PV-000123"), nome do cliente ou nome do vendedor. */
        public static Specification<Venda> buscaGeral(String termo) {
            return (root, query, cb) -> {
                if (termo == null) {
                    return null;
                }
                String padrao = "%" + termo.toLowerCase() + "%";
                var cliente = root.join("cliente", JoinType.LEFT);
                var vendedor = root.join("usuario", JoinType.LEFT);
                var nomes = cb.or(
                        cb.like(cb.lower(cliente.get("nome")), padrao),
                        cb.like(cb.lower(vendedor.get("nome")), padrao));
                String digitos = termo.replaceAll("\\D", "");
                if (digitos.isEmpty() || digitos.length() > 18) {
                    return nomes;
                }
                return cb.or(nomes, cb.equal(root.get("id"), Long.valueOf(digitos)));
            };
        }

        public static Specification<Venda> comStatus(StatusVenda status) {
            return (root, query, cb) -> status == null ? null : cb.equal(root.get("status"), status);
        }

        public static Specification<Venda> doVendedor(Long usuarioId) {
            return (root, query, cb) -> usuarioId == null ? null : cb.equal(root.get("usuario").get("id"), usuarioId);
        }

        public static Specification<Venda> doCliente(Long clienteId) {
            return (root, query, cb) -> clienteId == null ? null : cb.equal(root.get("cliente").get("id"), clienteId);
        }

        public static Specification<Venda> comFormaPagamento(FormaPagamento forma) {
            return (root, query, cb) -> forma == null ? null : cb.equal(root.get("formaPagamento"), forma);
        }

        /** Vendas cujo dia (data_hora) está entre {@code inicio} e {@code fim}, inclusive. */
        public static Specification<Venda> noPeriodo(LocalDate inicio, LocalDate fim) {
            return (root, query, cb) -> {
                var dataHora = root.<LocalDateTime>get("dataHora");
                if (inicio != null && fim != null) {
                    return cb.and(cb.greaterThanOrEqualTo(dataHora, inicio.atStartOfDay()),
                            cb.lessThan(dataHora, fim.plusDays(1).atStartOfDay()));
                }
                if (inicio != null) {
                    return cb.greaterThanOrEqualTo(dataHora, inicio.atStartOfDay());
                }
                return fim == null ? null : cb.lessThan(dataHora, fim.plusDays(1).atStartOfDay());
            };
        }
    }
}
