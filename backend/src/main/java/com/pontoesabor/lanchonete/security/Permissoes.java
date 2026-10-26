package com.pontoesabor.lanchonete.security;

/**
 * Expressões de permissão usadas em {@code @PreAuthorize}, conforme a tabela
 * "Usuários e Permissões" do protótipo (docs/modelagem/modelo-de-dados.md).
 */
public final class Permissoes {

    /** Dono e Gerente: editar clientes, gerenciar produtos, consultar funcionários. */
    public static final String GESTAO = "hasAnyRole('DONO', 'GERENTE')";

    /** Somente Dono: gerenciar funcionários e alterar configurações gerais. */
    public static final String DONO = "hasRole('DONO')";

    private Permissoes() {
    }
}
