package com.pontoesabor.lanchonete.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/**
 * Telefone, e-mail e endereço, usados pelo cliente e pelos dados da lanchonete (as colunas ficam na
 * própria tabela de cada um). É imutável: para mudar, a entidade recebe um novo {@code Contato}.
 * Telefone e CEP guardam só os números.
 */
@Embeddable
public record Contato(
        @Column(length = 11) String telefone,
        @Column(length = 150) String email,
        @Column(length = 8) String cep,
        @Column(length = 150) String logradouro,
        @Column(length = 20) String numero,
        @Column(length = 80) String bairro,
        @Column(length = 80) String cidade,
        @Column(length = 2) String uf) {

    /** Sem nenhum dado; o Hibernate carrega como {@code null} quando todas as colunas estão vazias. */
    public static final Contato VAZIO = new Contato(null, null, null, null, null, null, null, null);
}
