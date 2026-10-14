package com.pontoesabor.lanchonete.dto;

import com.pontoesabor.lanchonete.model.Perfil;
import com.pontoesabor.lanchonete.model.Usuario;
import java.time.LocalDateTime;

/** Dados do usuário devolvidos pela API. A senha nunca é exposta. */
public record UsuarioResponse(
        Long id,
        String nome,
        String email,
        Perfil perfil,
        boolean ativo,
        LocalDateTime criadoEm,
        LocalDateTime atualizadoEm) {

    public static UsuarioResponse de(Usuario usuario) {
        return new UsuarioResponse(
                usuario.getId(),
                usuario.getNome(),
                usuario.getEmail(),
                usuario.getPerfil(),
                usuario.isAtivo(),
                usuario.getCriadoEm(),
                usuario.getAtualizadoEm());
    }
}
