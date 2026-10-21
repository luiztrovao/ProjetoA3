package com.pontoesabor.lanchonete.dto;

import com.pontoesabor.lanchonete.util.Textos;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.br.CPF;

/**
 * Dados para cadastrar ou editar um cliente. Só o nome é obrigatório.
 * CPF, telefone e CEP podem vir formatados ("123.456.789-09", "(81) 99999-1234", "50030-230"):
 * só os dígitos são guardados.
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
        String email,

        @Pattern(regexp = "\\d{8}", message = "O CEP deve ter 8 dígitos.")
        String cep,

        @Size(max = 150, message = "O logradouro deve ter no máximo 150 caracteres.")
        String logradouro,

        @Size(max = 20, message = "O número deve ter no máximo 20 caracteres.")
        String numero,

        @Size(max = 100, message = "O complemento deve ter no máximo 100 caracteres.")
        String complemento,

        @Size(max = 80, message = "O bairro deve ter no máximo 80 caracteres.")
        String bairro,

        @Size(max = 80, message = "A cidade deve ter no máximo 80 caracteres.")
        String cidade,

        @Pattern(regexp = "[A-Z]{2}", message = "A UF deve ter 2 letras.")
        String uf,

        @Size(max = 500, message = "As observações devem ter no máximo 500 caracteres.")
        String observacoes) {

    public ClienteRequest {
        nome = Textos.limpar(nome);
        cpf = Textos.somenteDigitos(cpf);
        telefone = Textos.somenteDigitos(telefone);
        email = Textos.email(email);
        cep = Textos.somenteDigitos(cep);
        logradouro = Textos.limpar(logradouro);
        numero = Textos.limpar(numero);
        complemento = Textos.limpar(complemento);
        bairro = Textos.limpar(bairro);
        cidade = Textos.limpar(cidade);
        uf = Textos.maiusculas(uf);
        observacoes = Textos.limpar(observacoes);
    }

    /** Atalho para cadastros simples (usado nos testes). */
    public static ClienteRequest basico(String nome, String cpf, String telefone, String email) {
        return new ClienteRequest(nome, cpf, telefone, email, null, null, null, null, null, null, null, null);
    }
}
