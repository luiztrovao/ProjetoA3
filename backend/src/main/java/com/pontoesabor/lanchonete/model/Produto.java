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
import java.time.LocalDateTime;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "produto")
public class Produto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Código interno do produto (ex.: XBG001), único e em maiúsculas. */
    @Column(nullable = false, length = 20, unique = true)
    private String codigo;

    @Column(nullable = false, length = 120)
    private String nome;

    @Column(length = 500)
    private String descricao;

    /** Foto do produto (tabela imagem); só o id, para não carregar a imagem nas listagens. */
    @Column(name = "imagem_id")
    private Long imagemId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "categoria_id", nullable = false)
    private Categoria categoria;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal preco;

    @Column(name = "estoque_atual", nullable = false)
    private int estoqueAtual;

    @Column(name = "estoque_minimo", nullable = false)
    private int estoqueMinimo;

    @Column(nullable = false)
    private boolean ativo = true;

    @CreationTimestamp
    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    @UpdateTimestamp
    @Column(name = "atualizado_em", nullable = false)
    private LocalDateTime atualizadoEm;

    protected Produto() {
    }

    public Produto(String codigo, String nome, Categoria categoria, BigDecimal preco, int estoqueAtual, int estoqueMinimo) {
        this.codigo = codigo;
        this.nome = nome;
        this.categoria = categoria;
        this.preco = preco;
        this.estoqueAtual = estoqueAtual;
        this.estoqueMinimo = estoqueMinimo;
    }

    /** Estoque igual ou abaixo do mínimo: o produto aparece em destaque na tela. */
    public boolean isEstoqueBaixo() {
        return estoqueAtual <= estoqueMinimo;
    }

    /** Retira do estoque ao concluir uma venda. */
    public void baixarEstoque(int quantidade) {
        validarQuantidade(quantidade);
        if (quantidade > estoqueAtual) {
            throw new RegraNegocioException(
                    "Estoque insuficiente para \"" + nome + "\": disponível " + estoqueAtual
                            + ", solicitado " + quantidade + ".");
        }
        estoqueAtual -= quantidade;
    }

    /** Devolve ao estoque quando uma venda concluída é cancelada. */
    public void devolverEstoque(int quantidade) {
        validarQuantidade(quantidade);
        estoqueAtual += quantidade;
    }

    private static void validarQuantidade(int quantidade) {
        if (quantidade <= 0) {
            throw new RegraNegocioException("A quantidade deve ser maior que zero.");
        }
    }

    public Long getId() {
        return id;
    }

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public Long getImagemId() {
        return imagemId;
    }

    public void setImagemId(Long imagemId) {
        this.imagemId = imagemId;
    }

    public Categoria getCategoria() {
        return categoria;
    }

    public void setCategoria(Categoria categoria) {
        this.categoria = categoria;
    }

    public BigDecimal getPreco() {
        return preco;
    }

    public void setPreco(BigDecimal preco) {
        this.preco = preco;
    }

    public int getEstoqueAtual() {
        return estoqueAtual;
    }

    public void setEstoqueAtual(int estoqueAtual) {
        this.estoqueAtual = estoqueAtual;
    }

    public int getEstoqueMinimo() {
        return estoqueMinimo;
    }

    public void setEstoqueMinimo(int estoqueMinimo) {
        this.estoqueMinimo = estoqueMinimo;
    }

    public boolean isAtivo() {
        return ativo;
    }

    public void setAtivo(boolean ativo) {
        this.ativo = ativo;
    }

    public LocalDateTime getCriadoEm() {
        return criadoEm;
    }

    public LocalDateTime getAtualizadoEm() {
        return atualizadoEm;
    }
}
