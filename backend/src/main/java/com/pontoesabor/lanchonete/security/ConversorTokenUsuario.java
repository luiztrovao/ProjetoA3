package com.pontoesabor.lanchonete.security;

import com.pontoesabor.lanchonete.model.Usuario;
import com.pontoesabor.lanchonete.repository.UsuarioRepository;
import java.util.List;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

/**
 * Transforma um token válido no usuário autenticado da requisição.
 * O perfil é lido do banco (e não do token) para que uma mudança de perfil ou a inativação
 * do funcionário valham na hora, sem esperar o token expirar.
 */
@Component
public class ConversorTokenUsuario implements Converter<Jwt, AbstractAuthenticationToken> {

    private final UsuarioRepository usuarioRepository;

    public ConversorTokenUsuario(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        Usuario usuario = usuarioRepository.findById(Long.valueOf(jwt.getSubject()))
                .filter(Usuario::isAtivo)
                .orElseThrow(() -> new DisabledException("Usuário inativo ou inexistente."));
        var perfil = new SimpleGrantedAuthority("ROLE_" + usuario.getPerfil().name());
        return new JwtAuthenticationToken(jwt, List.of(perfil), String.valueOf(usuario.getId()));
    }
}
