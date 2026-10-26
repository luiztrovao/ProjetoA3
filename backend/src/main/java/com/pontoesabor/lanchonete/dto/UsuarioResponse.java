package com.pontoesabor.lanchonete.dto;

import com.pontoesabor.lanchonete.model.Perfil;
import com.pontoesabor.lanchonete.model.Usuario;
import java.time.LocalDateTime;

/** Dados do usuário devolvidos pela API. A senha nunca é exposta. */
public record UsuarioResponse(
        Long id,
        String nome,
        String username,
        String email,
        String cpf,
        String telefone,
        Perfil perfil,
        boolean ativo,
        LocalDateTime ultimoAcesso,
        LocalDateTime criadoEm,
        LocalDateTime atualizadoEm) {

    public static UsuarioResponse de(Usuario usuario) {
        return new UsuarioResponse(
                usuario.getId(),
                usuario.getNome(),
                usuario.getUsername(),
                usuario.getEmail(),
                usuario.getCpf(),
                usuario.getTelefone(),
                usuario.getPerfil(),
                usuario.isAtivo(),
                usuario.getUltimoAcesso(),
                usuario.getCriadoEm(),
                usuario.getAtualizadoEm());
    }
}
