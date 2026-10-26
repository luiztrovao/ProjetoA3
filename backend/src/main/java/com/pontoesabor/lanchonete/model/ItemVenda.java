package com.pontoesabor.lanchonete.model;

import com.pontoesabor.lanchonete.exception.RegraNegocioException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;

@Entity
@Table(name = "item_venda")
public class ItemVenda {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "venda_id", nullable = false)
    private Venda venda;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "produto_id", nullable = false)
    private Produto produto;

    @Column(nullable = false)
    private int quantidade;

    /** Cópia do preço do produto no momento da venda. */
    @Column(name = "preco_unitario", nullable = false, precision = 10, scale = 2)
    private BigDecimal precoUnitario;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal subtotal;

    /**
     * Quanto deste item saiu do estoque na confirmação. Pode ser menor que a quantidade quando a venda
     * sem estoque é permitida; no cancelamento, só esse valor volta ao estoque.
     */
    @Column(name = "quantidade_baixada_estoque", nullable = false)
    private int quantidadeBaixadaEstoque;

    protected ItemVenda() {
    }

    ItemVenda(Venda venda, Produto produto, int quantidade) {
        if (quantidade <= 0) {
            throw new RegraNegocioException("A quantidade do item deve ser maior que zero.");
        }
        this.venda = venda;
        this.produto = produto;
        this.quantidade = quantidade;
        this.precoUnitario = produto.getPreco();
        this.subtotal = precoUnitario.multiply(BigDecimal.valueOf(quantidade));
    }

    public Long getId() {
        return id;
    }

    public Venda getVenda() {
        return venda;
    }

    public Produto getProduto() {
        return produto;
    }

    public int getQuantidade() {
        return quantidade;
    }

    public BigDecimal getPrecoUnitario() {
        return precoUnitario;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public int getQuantidadeBaixadaEstoque() {
        return quantidadeBaixadaEstoque;
    }

    /** Retira o item do estoque; com {@code bloquearSemEstoque} desligado, retira só o que houver. */
    void baixarEstoque(boolean bloquearSemEstoque) {
        if (bloquearSemEstoque) {
            produto.baixarEstoque(quantidade);
            quantidadeBaixadaEstoque = quantidade;
        } else {
            quantidadeBaixadaEstoque = produto.baixarAte(quantidade);
        }
    }

    /** Devolve ao estoque o que foi retirado na confirmação. */
    void devolverEstoque() {
        if (quantidadeBaixadaEstoque > 0) {
            produto.devolverEstoque(quantidadeBaixadaEstoque);
            quantidadeBaixadaEstoque = 0;
        }
    }
}
