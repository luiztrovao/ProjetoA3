package com.pontoesabor.lanchonete.exception;

/** Lançada quando uma operação viola uma regra de negócio (ex.: estoque insuficiente). */
public class RegraNegocioException extends RuntimeException {

    public RegraNegocioException(String mensagem) {
        super(mensagem);
    }
}
