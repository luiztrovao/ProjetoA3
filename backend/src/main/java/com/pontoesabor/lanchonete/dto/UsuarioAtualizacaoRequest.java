package com.pontoesabor.lanchonete.dto;

import com.pontoesabor.lanchonete.model.Perfil;
import com.pontoesabor.lanchonete.util.Textos;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.br.CPF;

/** Dados para editar um usuário. A senha é opcional: se vier vazia, a atual é mantida. */
public record UsuarioAtualizacaoRequest(
        @NotBlank(message = "O nome é obrigatório.")
        @Size(max = 120, message = "O nome deve ter no máximo 120 caracteres.")
        String nome,

        @NotBlank(message = "O nome de usuário é obrigatório.")
        @Pattern(regexp = UsuarioCriacaoRequest.PADRAO_USERNAME, message = UsuarioCriacaoRequest.MENSAGEM_USERNAME)
        String username,

        @Email(message = "E-mail inválido.")
        @Size(max = 150, message = "O e-mail deve ter no máximo 150 caracteres.")
        String email,

        @CPF(message = "CPF inválido.")
        String cpf,

        @Pattern(regexp = "\\d{10,11}", message = "O telefone deve ter DDD + número (10 ou 11 dígitos).")
        String telefone,

        @Size(min = 6, max = 72, message = "A senha deve ter entre 6 e 72 caracteres.")
        String senha,

        @NotNull(message = "O perfil é obrigatório.")
        Perfil perfil) {

    public UsuarioAtualizacaoRequest {
        nome = Textos.limpar(nome);
        username = Textos.minusculas(username);
        email = Textos.email(email);
        cpf = Textos.somenteDigitos(cpf);
        telefone = Textos.somenteDigitos(telefone);
        senha = (senha == null || senha.isBlank()) ? null : senha;
    }
}
