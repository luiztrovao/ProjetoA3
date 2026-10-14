package com.pontoesabor.lanchonete.exception;

import java.time.Instant;
import java.util.List;

/** Formato padrão de erro devolvido pela API. */
public record ErroResponse(
        Instant timestamp,
        int status,
        String erro,
        String mensagem,
        String caminho,
        List<CampoInvalido> campos) {

    public record CampoInvalido(String campo, String mensagem) {
    }
}
