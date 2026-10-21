package com.pontoesabor.lanchonete.service;

import com.pontoesabor.lanchonete.dto.AuthDtos.AlterarSenhaRequest;
import com.pontoesabor.lanchonete.dto.AuthDtos.ContaRequest;
import com.pontoesabor.lanchonete.dto.AuthDtos.LoginRequest;
import com.pontoesabor.lanchonete.dto.AuthDtos.LoginResponse;
import com.pontoesabor.lanchonete.dto.UsuarioResponse;
import com.pontoesabor.lanchonete.exception.CredenciaisInvalidasException;
import com.pontoesabor.lanchonete.exception.RecursoNaoEncontradoException;
import com.pontoesabor.lanchonete.exception.RegraNegocioException;
import com.pontoesabor.lanchonete.model.Usuario;
import com.pontoesabor.lanchonete.repository.UsuarioRepository;
import com.pontoesabor.lanchonete.security.TokenService;
import com.pontoesabor.lanchonete.security.TokenService.TokenGerado;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Login e operações do próprio usuário (tela "Minha Conta"). */
@Service
@Transactional(readOnly = true)
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final Clock relogio;

    public AuthService(
            UsuarioRepository usuarioRepository,
            PasswordEncoder passwordEncoder,
            TokenService tokenService,
            Clock relogio) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.relogio = relogio;
    }

    /**
     * Autentica pelo nome de usuário ou e-mail. A mensagem de erro é sempre a mesma,
     * para não revelar se o usuário existe.
     */
    @Transactional
    public LoginResponse login(LoginRequest request) {
        Usuario usuario = buscarPorLogin(request.login())
                .filter(Usuario::isAtivo)
                .filter(encontrado -> passwordEncoder.matches(request.senha(), encontrado.getSenhaHash()))
                .orElseThrow(CredenciaisInvalidasException::new);
        usuario.registrarAcesso(LocalDateTime.now(relogio));
        TokenGerado token = tokenService.gerar(usuario);
        return new LoginResponse(token.token(), token.expiraEm(), UsuarioResponse.de(usuario));
    }

    public UsuarioResponse minhaConta(Long idUsuario) {
        return UsuarioResponse.de(buscar(idUsuario));
    }

    @Transactional
    public UsuarioResponse atualizarConta(Long idUsuario, ContaRequest request) {
        Usuario usuario = buscar(idUsuario);
        boolean duplicado = usuarioRepository.existsByUsernameAndIdNot(request.username(), idUsuario)
                || (request.email() != null && usuarioRepository.existsByEmailAndIdNot(request.email(), idUsuario));
        if (duplicado) {
            throw new RegraNegocioException("Já existe um funcionário com o mesmo usuário ou e-mail.");
        }
        usuario.setNome(request.nome());
        usuario.setUsername(request.username());
        usuario.setEmail(request.email());
        usuario.setTelefone(request.telefone());
        return UsuarioResponse.de(usuarioRepository.saveAndFlush(usuario));
    }

    @Transactional
    public void alterarSenha(Long idUsuario, AlterarSenhaRequest request) {
        Usuario usuario = buscar(idUsuario);
        if (!passwordEncoder.matches(request.senhaAtual(), usuario.getSenhaHash())) {
            throw new RegraNegocioException("A senha atual está incorreta.");
        }
        if (!request.novaSenha().equals(request.confirmacaoSenha())) {
            throw new RegraNegocioException("A confirmação da nova senha não confere.");
        }
        usuario.setSenhaHash(passwordEncoder.encode(request.novaSenha()));
    }

    private Optional<Usuario> buscarPorLogin(String login) {
        if (login == null) {
            return Optional.empty();
        }
        return login.contains("@") ? usuarioRepository.findByEmail(login) : usuarioRepository.findByUsername(login);
    }

    private Usuario buscar(Long idUsuario) {
        return usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário", idUsuario));
    }
}
