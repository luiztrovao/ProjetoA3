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

    private static UsuarioCriacaoRequest criacao(String username, String email, String cpf) {
        return new UsuarioCriacaoRequest("Fernanda Souza", username, email, cpf, "(81) 97766-3300", "senha1", Perfil.VENDEDOR);
    }

    private static UsuarioAtualizacaoRequest atualizacao(String nome, String senha, Perfil perfil) {
        return new UsuarioAtualizacaoRequest(nome, "matheus", null, null, null, senha, perfil);
    }

    @Test
    void criarGuardaSenhaCriptografadaENormalizaCampos() {
        when(passwordEncoder.encode("senha1")).thenReturn("$2a$hash");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(invocacao -> invocacao.getArgument(0));

        UsuarioResponse resposta = usuarioService.criar(criacao(" Fernanda ", "Fernanda@PontoeSabor.com", "529.982.247-25"));

        ArgumentCaptor<Usuario> salvo = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(salvo.capture());
        assertThat(salvo.getValue().getSenhaHash()).isEqualTo("$2a$hash");
        assertThat(resposta.username()).isEqualTo("fernanda");
        assertThat(resposta.email()).isEqualTo("fernanda@pontoesabor.com");
        assertThat(resposta.cpf()).isEqualTo("52998224725");
        assertThat(resposta.telefone()).isEqualTo("81977663300");
    }

    @Test
    void criarSemEmailECpfNaoConsultaDuplicidadeDeles() {
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(invocacao -> invocacao.getArgument(0));

        usuarioService.criar(criacao("fernanda", null, null));

        verify(usuarioRepository, never()).existsByEmail(any());
        verify(usuarioRepository, never()).existsByCpf(any());
    }

    @Test
    void criarComUsuarioRepetidoLancaExcecao() {
        when(usuarioRepository.existsByUsername("fernanda")).thenReturn(true);

        assertThatThrownBy(() -> usuarioService.criar(criacao("fernanda", null, null)))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessage("Já existe um funcionário com o mesmo CPF, usuário ou e-mail.");
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void criarComCpfRepetidoLancaExcecao() {
        when(usuarioRepository.existsByCpf("52998224725")).thenReturn(true);

        assertThatThrownBy(() -> usuarioService.criar(criacao("fernanda", null, "52998224725")))
                .isInstanceOf(RegraNegocioException.class);
    }

    @Test
    void atualizarSemSenhaMantemSenhaAtual() {
        Usuario usuario = new Usuario("Matheus", "matheus", "hashAntigo", Perfil.VENDEDOR);
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(usuarioRepository.saveAndFlush(usuario)).thenReturn(usuario);

        usuarioService.atualizar(1L, atualizacao("Matheus Almeida", "  ", Perfil.VENDEDOR));

        assertThat(usuario.getSenhaHash()).isEqualTo("hashAntigo");
        assertThat(usuario.getNome()).isEqualTo("Matheus Almeida");
        verify(passwordEncoder, never()).encode(any());
    }

    @Test
    void naoPermiteInativarUltimoDono() {
        Usuario dono = new Usuario("Matheus", "matheus", "hash", Perfil.DONO);
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(dono));
        when(usuarioRepository.countByPerfilAndAtivoTrue(Perfil.DONO)).thenReturn(1L);

        assertThatThrownBy(() -> usuarioService.inativar(1L))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("Dono");
        assertThat(dono.isAtivo()).isTrue();
    }

    @Test
    void permiteInativarDonoQuandoExisteOutro() {
        Usuario dono = new Usuario("Matheus", "matheus", "hash", Perfil.DONO);
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(dono));
        when(usuarioRepository.countByPerfilAndAtivoTrue(Perfil.DONO)).thenReturn(2L);

        usuarioService.inativar(1L);

        assertThat(dono.isAtivo()).isFalse();
    }

    @Test
    void naoPermiteRebaixarUltimoDono() {
        Usuario dono = new Usuario("Matheus", "matheus", "hash", Perfil.DONO);
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(dono));
        when(usuarioRepository.countByPerfilAndAtivoTrue(Perfil.DONO)).thenReturn(1L);

        assertThatThrownBy(() -> usuarioService.atualizar(1L, atualizacao("Matheus", null, Perfil.GERENTE)))
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
