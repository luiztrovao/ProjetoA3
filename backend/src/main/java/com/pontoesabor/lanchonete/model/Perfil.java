package com.pontoesabor.lanchonete.model;

/** Nível de acesso do usuário. As permissões estão em docs/modelagem/modelo-de-dados.md. */
public enum Perfil {
    DONO,
    GERENTE,
    VENDEDOR,
    /** Acesso só de leitura à tela Relatórios (ex.: alunos na apresentação do projeto). */
    VISITANTE
}
