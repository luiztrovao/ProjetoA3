package com.pontoesabor.lanchonete.dto;

import com.pontoesabor.lanchonete.util.Textos;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.br.CPF;

/**
 * Dados para cadastrar ou editar um cliente.
 * CPF e telefone podem vir formatados ("123.456.789-09", "(81) 99999-1234"): só os dígitos são guardados.
 */
public record ClienteRequest(
        @NotBlank(message = "O nome é obrigatório.")
        @Size(max = 120, message = "O nome deve ter no máximo 120 caracteres.")
        String nome,

        @CPF(message = "CPF inválido.")
        String cpf,

        @Pattern(regexp = "\\d{10,11}", message = "O telefone deve ter DDD + número (10 ou 11 dígitos).")
        String telefone,

        @Email(message = "E-mail inválido.")
        @Size(max = 150, message = "O e-mail deve ter no máximo 150 caracteres.")
        String email) {

    public ClienteRequest {
        nome = Textos.limpar(nome);
        cpf = Textos.somenteDigitos(cpf);
        telefone = Textos.somenteDigitos(telefone);
        email = Textos.email(email);
    }
}
