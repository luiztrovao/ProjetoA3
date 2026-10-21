package com.pontoesabor.lanchonete.exception;

/** Login com usuário/e-mail ou senha incorretos, ou usuário inativo. Vira HTTP 401. */
public class CredenciaisInvalidasException extends RuntimeException {

    public CredenciaisInvalidasException() {
        super("Usuário ou senha inválidos.");
    }
}
