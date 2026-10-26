package com.pontoesabor.lanchonete.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Imagem (PNG ou JPG) guardada no banco: logo da lanchonete ou foto de produto.
 * Cada envio cria uma imagem nova; a anterior é apagada. Assim a URL muda a cada troca
 * e o navegador pode guardar a imagem em cache sem risco de mostrar uma versão antiga.
 */
@Entity
@Table(name = "imagem")
public class Imagem {

    /** Endereço público de uma imagem, usado nas respostas da API. */
    public static String url(Long id) {
        return id == null ? null : "/api/imagens/" + id;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JdbcTypeCode(SqlTypes.VARBINARY)
    @Column(nullable = false)
    private byte[] conteudo;

    @Column(name = "tipo_conteudo", nullable = false, length = 30)
    private String tipoConteudo;

    @Column(nullable = false)
    private int tamanho;

    @CreationTimestamp
    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    protected Imagem() {
    }

    public Imagem(byte[] conteudo, String tipoConteudo) {
        this.conteudo = conteudo;
        this.tipoConteudo = tipoConteudo;
        this.tamanho = conteudo.length;
    }

    public Long getId() {
        return id;
    }

    public byte[] getConteudo() {
        return conteudo;
    }

    public String getTipoConteudo() {
        return tipoConteudo;
    }

    public int getTamanho() {
        return tamanho;
    }

    public LocalDateTime getCriadoEm() {
        return criadoEm;
    }
}
