package com.pontoesabor.lanchonete.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.pontoesabor.lanchonete.dto.AuthDtos.ContaRequest;
import com.pontoesabor.lanchonete.dto.AuthDtos.LoginRequest;
import com.pontoesabor.lanchonete.exception.CredenciaisInvalidasException;
import com.pontoesabor.lanchonete.exception.RecursoNaoEncontradoException;
import com.pontoesabor.lanchonete.exception.RegraNegocioException;
import com.pontoesabor.lanchonete.model.Perfil;
import com.pontoesabor.lanchonete.model.Usuario;
import com.pontoesabor.lanchonete.repository.UsuarioRepository;
import com.pontoesabor.lanchonete.security.TokenService;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

/** Caminhos do login e da Minha Conta que os testes da API não exercitam. */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private TokenService tokenService;

    @InjectMocks
    private AuthService authService;

    @Test
    void loginSemUsuarioNaoConsultaOBanco() {
        assertThatThrownBy(() -> authService.login(new LoginRequest(null, "senha1")))
                .isInstanceOf(CredenciaisInvalidasException.class);
        verifyNoInteractions(usuarioRepository, tokenService);
    }

    @Test
    void loginComArrobaBuscaPorEmail() {
        when(usuarioRepository.findByEmail("matheus@pontoesabor.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(new LoginRequest("matheus@pontoesabor.com", "senha1")))
                .isInstanceOf(CredenciaisInvalidasException.class);
        verify(usuarioRepository, never()).findByUsername(any());
    }

    @Test
    void minhaContaDeUsuarioRemovidoLancaExcecao() {
        when(usuarioRepository.findById(7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.minhaConta(7L))
                .isInstanceOf(RecursoNaoEncontradoException.class)
                .hasMessageContaining("7");
    }

    @Test
    void atualizarContaComUsuarioDeOutroFuncionarioLancaExcecao() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(new Usuario("Matheus", "matheus", "hash", Perfil.DONO)));
        when(usuarioRepository.existsByUsernameAndIdNot("carlos", 1L)).thenReturn(true);

        assertThatThrownBy(() -> authService.atualizarConta(1L, new ContaRequest("Matheus", "carlos", null, null)))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessage("Já existe um funcionário com o mesmo usuário ou e-mail.");
        verify(usuarioRepository, never()).saveAndFlush(any());
    }

    @Test
    void atualizarContaComEmailDeOutroFuncionarioLancaExcecao() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(new Usuario("Matheus", "matheus", "hash", Perfil.DONO)));
        when(usuarioRepository.existsByEmailAndIdNot("carlos@pontoesabor.com", 1L)).thenReturn(true);

        assertThatThrownBy(() -> authService.atualizarConta(
                        1L, new ContaRequest("Matheus", "matheus", "Carlos@PontoeSabor.com", null)))
                .isInstanceOf(RegraNegocioException.class);
        verify(usuarioRepository, never()).saveAndFlush(any());
    }

    @Test
    void atualizarContaSemEmailNaoConsultaDuplicidadeDeEmail() {
        Usuario usuario = new Usuario("Matheus", "matheus", "hash", Perfil.DONO);
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(usuarioRepository.saveAndFlush(usuario)).thenReturn(usuario);

        authService.atualizarConta(1L, new ContaRequest("Matheus Almeida", "matheus", null, "(81) 99999-0000"));

        assertThat(usuario.getNome()).isEqualTo("Matheus Almeida");
        assertThat(usuario.getTelefone()).isEqualTo("81999990000");
        verify(usuarioRepository, never()).existsByEmailAndIdNot(any(), any());
    }
}
