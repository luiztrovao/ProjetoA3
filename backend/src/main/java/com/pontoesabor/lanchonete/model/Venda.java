package com.pontoesabor.lanchonete.model;

import com.pontoesabor.lanchonete.exception.RegraNegocioException;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Venda/pedido, no fluxo do protótipo:
 * <ol>
 *   <li><b>Montagem</b>: itens e desconto são definidos (ainda não gravada);</li>
 *   <li><b>Confirmação</b>: forma de pagamento escolhida, estoque baixado e status inicial das configurações;</li>
 *   <li><b>Em preparo → Concluída</b>;</li>
 *   <li><b>Cancelada</b> (com motivo), devolvendo ao estoque o que foi retirado.</li>
 * </ol>
 */
@Entity
@Table(name = "venda")
public class Venda {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Opcional: venda de balcão sem cliente cadastrado. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id")
    private Cliente cliente;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(name = "data_hora", nullable = false)
    private LocalDateTime dataHora;

    /** Nulo enquanto a venda está em montagem; preenchido na confirmação. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusVenda status;

    @Enumerated(EnumType.STRING)
    @Column(name = "forma_pagamento", nullable = false, length = 20)
    private FormaPagamento formaPagamento;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal subtotal = BigDecimal.ZERO;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal desconto = BigDecimal.ZERO;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal total = BigDecimal.ZERO;

    @Column(length = 500)
    private String observacao;

    /** Pagamento em dinheiro: valor entregue pelo cliente e troco. */
    @Column(name = "valor_recebido", precision = 10, scale = 2)
    private BigDecimal valorRecebido;

    @Column(precision = 10, scale = 2)
    private BigDecimal troco;

    @Column(name = "confirmada_em")
    private LocalDateTime confirmadaEm;

    @Column(name = "cancelada_em")
    private LocalDateTime canceladaEm;

    @Column(name = "motivo_cancelamento", length = 500)
    private String motivoCancelamento;

    @OneToMany(mappedBy = "venda", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ItemVenda> itens = new ArrayList<>();

    protected Venda() {
    }

    /** Inicia a montagem de uma venda. */
    public Venda(Usuario usuario, Cliente cliente, FormaPagamento formaPagamento, LocalDateTime dataHora) {
        this.usuario = usuario;
        this.cliente = cliente;
        this.formaPagamento = formaPagamento;
        this.dataHora = dataHora;
    }

    // ===== Montagem =====

    public void adicionarItem(Produto produto, int quantidade) {
        exigirEmMontagem();
        if (!produto.isAtivo()) {
            throw new RegraNegocioException("O produto \"" + produto.getNome() + "\" está inativo.");
        }
        itens.add(new ItemVenda(this, produto, quantidade));
        recalcularTotais();
    }

    public void aplicarDesconto(BigDecimal valor) {
        exigirEmMontagem();
        if (valor.signum() < 0) {
            throw new RegraNegocioException("O desconto não pode ser negativo.");
        }
        if (valor.compareTo(subtotal) > 0) {
            throw new RegraNegocioException("O desconto não pode ser maior que o subtotal.");
        }
        desconto = valor;
        recalcularTotais();
    }

    public void setObservacao(String observacao) {
        this.observacao = observacao;
    }

    // ===== Confirmação =====

    /**
     * Confirma a venda: valida o pagamento, baixa o estoque (se controlado) e aplica o status inicial.
     *
     * @param valorRecebido obrigatório e maior ou igual ao total quando a forma é dinheiro; ignorado nas demais
     */
    public void confirmar(
            StatusVenda statusInicial,
            BigDecimal valorRecebido,
            boolean controleEstoque,
            boolean bloquearSemEstoque,
            LocalDateTime momento) {
        exigirEmMontagem();
        if (itens.isEmpty()) {
            throw new RegraNegocioException("O pedido não possui produtos para confirmar.");
        }
        if (statusInicial == StatusVenda.CANCELADA) {
            throw new RegraNegocioException("O status inicial da venda deve ser Em preparo ou Concluída.");
        }
        registrarPagamento(valorRecebido);
        if (controleEstoque) {
            itens.forEach(item -> item.baixarEstoque(bloquearSemEstoque));
        }
        this.status = statusInicial;
        this.confirmadaEm = momento;
    }

    private void registrarPagamento(BigDecimal recebido) {
        if (formaPagamento != FormaPagamento.DINHEIRO) {
            valorRecebido = null;
            troco = null;
            return;
        }
        if (recebido == null || recebido.compareTo(total) < 0) {
            throw new RegraNegocioException("O valor recebido deve ser igual ou superior ao total do pedido.");
        }
        valorRecebido = recebido;
        troco = recebido.subtract(total);
    }

    // ===== Depois de confirmada =====

    /** Marca um pedido em preparo como concluído. */
    public void concluir() {
        if (status != StatusVenda.EM_PREPARO) {
            throw new RegraNegocioException("Só pedidos em preparo podem ser concluídos.");
        }
        status = StatusVenda.CONCLUIDA;
    }

    /** Cancela o pedido com motivo; os produtos retirados voltam ao estoque. */
    public void cancelar(String motivo, LocalDateTime momento) {
        if (status == null) {
            throw new RegraNegocioException("A venda ainda não foi confirmada.");
        }
        if (status == StatusVenda.CANCELADA) {
            throw new RegraNegocioException("O pedido já está cancelado.");
        }
        if (motivo == null || motivo.isBlank()) {
            throw new RegraNegocioException("Informe o motivo do cancelamento.");
        }
        itens.forEach(ItemVenda::devolverEstoque);
        status = StatusVenda.CANCELADA;
        motivoCancelamento = motivo.strip();
        canceladaEm = momento;
    }

    private void exigirEmMontagem() {
        if (status != null) {
            throw new RegraNegocioException("A venda já foi confirmada e não pode ser alterada.");
        }
    }

    private void recalcularTotais() {
        subtotal = itens.stream().map(ItemVenda::getSubtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        total = subtotal.subtract(desconto);
    }

    /** Número exibido nas telas, no formato do protótipo (ex.: #PV-000123). */
    public String getNumero() {
        return id == null ? null : "#PV-%06d".formatted(id);
    }

    public Long getId() {
        return id;
    }

    public Cliente getCliente() {
        return cliente;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public LocalDateTime getDataHora() {
        return dataHora;
    }

    public StatusVenda getStatus() {
        return status;
    }

    public FormaPagamento getFormaPagamento() {
        return formaPagamento;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public BigDecimal getDesconto() {
        return desconto;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public String getObservacao() {
        return observacao;
    }

    public BigDecimal getValorRecebido() {
        return valorRecebido;
    }

    public BigDecimal getTroco() {
        return troco;
    }

    public LocalDateTime getConfirmadaEm() {
        return confirmadaEm;
    }

    public LocalDateTime getCanceladaEm() {
        return canceladaEm;
    }

    public String getMotivoCancelamento() {
        return motivoCancelamento;
    }

    public List<ItemVenda> getItens() {
        return Collections.unmodifiableList(itens);
    }
}
