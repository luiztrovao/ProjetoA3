package com.pontoesabor.lanchonete.dto;

import com.pontoesabor.lanchonete.model.Perfil;
import com.pontoesabor.lanchonete.util.Textos;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Dados para editar um usuário. A senha é opcional: se vier vazia, a atual é mantida. */
public record UsuarioAtualizacaoRequest(
        @NotBlank(message = "O nome é obrigatório.")
        @Size(max = 120, message = "O nome deve ter no máximo 120 caracteres.")
        String nome,

        @NotBlank(message = "O e-mail é obrigatório.")
        @Email(message = "E-mail inválido.")
        @Size(max = 150, message = "O e-mail deve ter no máximo 150 caracteres.")
        String email,

        @Size(min = 8, max = 72, message = "A senha deve ter entre 8 e 72 caracteres.")
        String senha,

        @NotNull(message = "O perfil é obrigatório.")
        Perfil perfil) {

    public UsuarioAtualizacaoRequest {
        nome = Textos.limpar(nome);
        email = Textos.email(email);
        senha = (senha == null || senha.isBlank()) ? null : senha;
    }
}
