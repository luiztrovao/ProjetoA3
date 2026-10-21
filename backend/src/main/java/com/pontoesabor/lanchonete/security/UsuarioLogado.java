package com.pontoesabor.lanchonete.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/** Acesso ao usuário da requisição atual. O "name" da autenticação é o id do usuário. */
public final class UsuarioLogado {

    private UsuarioLogado() {
    }

    public static Long id() {
        Authentication autenticacao = SecurityContextHolder.getContext().getAuthentication();
        if (autenticacao == null || !autenticacao.isAuthenticated()) {
            throw new IllegalStateException("Nenhum usuário autenticado nesta requisição.");
        }
        return Long.valueOf(autenticacao.getName());
    }
}
