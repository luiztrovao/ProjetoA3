package com.pontoesabor.lanchonete.util;

/** Funções de normalização de texto usadas nos DTOs de entrada. */
public final class Textos {

    private Textos() {
    }

    /** Remove espaços das pontas; texto vazio vira {@code null}. */
    public static String limpar(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        return valor.strip();
    }

    /** Mantém só os dígitos (ex.: "123.456.789-09" vira "12345678909"); vazio vira {@code null}. */
    public static String somenteDigitos(String valor) {
        if (valor == null) {
            return null;
        }
        String digitos = valor.replaceAll("\\D", "");
        return digitos.isEmpty() ? null : digitos;
    }

    /** E-mail sem espaços e em minúsculas; vazio vira {@code null}. */
    public static String email(String valor) {
        return minusculas(valor);
    }

    /** Texto sem espaços nas pontas e em minúsculas (ex.: nome de usuário); vazio vira {@code null}. */
    public static String minusculas(String valor) {
        String limpo = limpar(valor);
        return limpo == null ? null : limpo.toLowerCase();
    }

    /** Texto sem espaços nas pontas e em maiúsculas (ex.: UF, código de produto); vazio vira {@code null}. */
    public static String maiusculas(String valor) {
        String limpo = limpar(valor);
        return limpo == null ? null : limpo.toUpperCase();
    }
}
