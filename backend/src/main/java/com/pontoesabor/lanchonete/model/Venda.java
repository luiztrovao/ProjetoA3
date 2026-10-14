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

/** Venda/pedido. O status indica a etapa em que ela está. */
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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusVenda status = StatusVenda.EM_PREPARO;

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

    @OneToMany(mappedBy = "venda", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ItemVenda> itens = new ArrayList<>();

    protected Venda() {
    }

    public Venda(Usuario usuario, Cliente cliente, FormaPagamento formaPagamento, LocalDateTime dataHora) {
        this.usuario = usuario;
        this.cliente = cliente;
        this.formaPagamento = formaPagamento;
        this.dataHora = dataHora;
    }

    public void adicionarItem(Produto produto, int quantidade) {
        exigirEmPreparo();
        itens.add(new ItemVenda(this, produto, quantidade));
        recalcularTotais();
    }

    public void aplicarDesconto(BigDecimal valor) {
        exigirEmPreparo();
        if (valor.signum() < 0) {
            throw new RegraNegocioException("O desconto não pode ser negativo.");
        }
        if (valor.compareTo(subtotal) > 0) {
            throw new RegraNegocioException("O desconto não pode ser maior que o subtotal.");
        }
        desconto = valor;
        recalcularTotais();
    }

    /** Conclui a venda e baixa o estoque de cada produto. */
    public void concluir() {
        exigirEmPreparo();
        if (itens.isEmpty()) {
            throw new RegraNegocioException("Não é possível concluir uma venda sem itens.");
        }
        itens.forEach(item -> item.getProduto().baixarEstoque(item.getQuantidade()));
        status = StatusVenda.CONCLUIDA;
    }

    /** Cancela a venda; se ela já estava concluída, devolve os produtos ao estoque. */
    public void cancelar() {
        if (status == StatusVenda.CANCELADA) {
            throw new RegraNegocioException("A venda já está cancelada.");
        }
        if (status == StatusVenda.CONCLUIDA) {
            itens.forEach(item -> item.getProduto().devolverEstoque(item.getQuantidade()));
        }
        status = StatusVenda.CANCELADA;
    }

    private void exigirEmPreparo() {
        if (status != StatusVenda.EM_PREPARO) {
            throw new RegraNegocioException("A venda não pode ser alterada no status " + status + ".");
        }
    }

    private void recalcularTotais() {
        subtotal = itens.stream().map(ItemVenda::getSubtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        total = subtotal.subtract(desconto);
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

    public void setObservacao(String observacao) {
        this.observacao = observacao;
    }

    public List<ItemVenda> getItens() {
        return Collections.unmodifiableList(itens);
    }
}
