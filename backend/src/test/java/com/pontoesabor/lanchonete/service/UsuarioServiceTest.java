package com.pontoesabor.lanchonete.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pontoesabor.lanchonete.dto.UsuarioAtualizacaoRequest;
import com.pontoesabor.lanchonete.dto.UsuarioCriacaoRequest;
import com.pontoesabor.lanchonete.dto.UsuarioResponse;
import com.pontoesabor.lanchonete.exception.RecursoNaoEncontradoException;
import com.pontoesabor.lanchonete.exception.RegraNegocioException;
import com.pontoesabor.lanchonete.model.Perfil;
import com.pontoesabor.lanchonete.model.Usuario;
import com.pontoesabor.lanchonete.repository.UsuarioRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UsuarioService usuarioService;

    @Test
    void criarGuardaSenhaCriptografada() {
        when(usuarioRepository.existsByEmail("ana@pontoesabor.com")).thenReturn(false);
        when(passwordEncoder.encode("senhaSegura1")).thenReturn("$2a$hash");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(invocacao -> invocacao.getArgument(0));

        UsuarioResponse resposta = usuarioService.criar(
                new UsuarioCriacaoRequest("Ana", " Ana@PontoeSabor.com ", "senhaSegura1", Perfil.VENDEDOR));

        ArgumentCaptor<Usuario> salvo = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(salvo.capture());
        assertThat(salvo.getValue().getSenhaHash()).isEqualTo("$2a$hash");
        assertThat(resposta.email()).isEqualTo("ana@pontoesabor.com");
    }

    @Test
    void criarComEmailRepetidoLancaExcecao() {
        when(usuarioRepository.existsByEmail("ana@pontoesabor.com")).thenReturn(true);

        assertThatThrownBy(() -> usuarioService.criar(
                new UsuarioCriacaoRequest("Ana", "ana@pontoesabor.com", "senhaSegura1", Perfil.VENDEDOR)))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("e-mail");
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void atualizarSemSenhaMantemSenhaAtual() {
        Usuario usuario = new Usuario("Ana", "ana@pontoesabor.com", "hashAntigo", Perfil.VENDEDOR);
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(usuarioRepository.saveAndFlush(usuario)).thenReturn(usuario);

        usuarioService.atualizar(1L,
                new UsuarioAtualizacaoRequest("Ana Souza", "ana@pontoesabor.com", "  ", Perfil.VENDEDOR));

        assertThat(usuario.getSenhaHash()).isEqualTo("hashAntigo");
        assertThat(usuario.getNome()).isEqualTo("Ana Souza");
        verify(passwordEncoder, never()).encode(any());
    }

    @Test
    void naoPermiteInativarUltimoDono() {
        Usuario dono = new Usuario("Matheus", "matheus@pontoesabor.com", "hash", Perfil.DONO);
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(dono));
        when(usuarioRepository.countByPerfilAndAtivoTrue(Perfil.DONO)).thenReturn(1L);

        assertThatThrownBy(() -> usuarioService.inativar(1L))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("Dono");
        assertThat(dono.isAtivo()).isTrue();
    }

    @Test
    void permiteInativarDonoQuandoExisteOutro() {
        Usuario dono = new Usuario("Matheus", "matheus@pontoesabor.com", "hash", Perfil.DONO);
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(dono));
        when(usuarioRepository.countByPerfilAndAtivoTrue(Perfil.DONO)).thenReturn(2L);

        usuarioService.inativar(1L);

        assertThat(dono.isAtivo()).isFalse();
    }

    @Test
    void naoPermiteRebaixarUltimoDono() {
        Usuario dono = new Usuario("Matheus", "matheus@pontoesabor.com", "hash", Perfil.DONO);
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(dono));
        when(usuarioRepository.countByPerfilAndAtivoTrue(Perfil.DONO)).thenReturn(1L);

        assertThatThrownBy(() -> usuarioService.atualizar(1L,
                new UsuarioAtualizacaoRequest("Matheus", "matheus@pontoesabor.com", null, Perfil.GERENTE)))
                .isInstanceOf(RegraNegocioException.class);
        assertThat(dono.getPerfil()).isEqualTo(Perfil.DONO);
    }

    @Test
    void buscarInexistenteLancaExcecao() {
        when(usuarioRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> usuarioService.buscar(99L))
                .isInstanceOf(RecursoNaoEncontradoException.class)
                .hasMessageContaining("99");
    }
}
