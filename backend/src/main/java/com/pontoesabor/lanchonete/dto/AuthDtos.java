package com.pontoesabor.lanchonete.dto;

import com.pontoesabor.lanchonete.util.Textos;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;

/** DTOs de login e da tela "Minha Conta". */
public final class AuthDtos {

    private AuthDtos() {
    }

    /** Login com o nome de usuário ou o e-mail. */
    public record LoginRequest(
            @NotBlank(message = "Informe o usuário ou e-mail.")
            String login,

            @NotBlank(message = "Informe a senha.")
            String senha) {

        public LoginRequest {
            login = Textos.minusculas(login);
        }
    }

    /** Resposta do login: envie o token em {@code Authorization: Bearer <token>}. */
    public record LoginResponse(String token, String tipo, Instant expiraEm, UsuarioResponse usuario) {

        public LoginResponse(String token, Instant expiraEm, UsuarioResponse usuario) {
            this(token, "Bearer", expiraEm, usuario);
        }
    }

    /** Dados que o próprio usuário pode alterar. Perfil e status só o Dono altera, em Cadastros. */
    public record ContaRequest(
            @NotBlank(message = "O nome é obrigatório.")
            @Size(max = 120, message = "O nome deve ter no máximo 120 caracteres.")
            String nome,

            @NotBlank(message = "O nome de usuário é obrigatório.")
            @Pattern(regexp = "[a-z0-9._-]{3,50}",
                    message = "O nome de usuário deve ter de 3 a 50 caracteres: letras, números, ponto, hífen ou sublinhado.")
            String username,

            @Email(message = "E-mail inválido.")
            @Size(max = 150, message = "O e-mail deve ter no máximo 150 caracteres.")
            String email,

            @Pattern(regexp = "\\d{10,11}", message = "O telefone deve ter DDD + número (10 ou 11 dígitos).")
            String telefone) {

        public ContaRequest {
            nome = Textos.limpar(nome);
            username = Textos.minusculas(username);
            email = Textos.email(email);
            telefone = Textos.somenteDigitos(telefone);
        }
    }

    /** Troca de senha: exige a senha atual e a confirmação da nova. */
    public record AlterarSenhaRequest(
            @NotBlank(message = "Informe a senha atual.")
            String senhaAtual,

            @NotBlank(message = "Informe a nova senha.")
            @Size(min = 6, max = 72, message = "A nova senha deve ter entre 6 e 72 caracteres.")
            String novaSenha,

            @NotBlank(message = "Confirme a nova senha.")
            String confirmacaoSenha) {
    }
}
