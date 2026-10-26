package com.pontoesabor.lanchonete.service;

import static com.pontoesabor.lanchonete.repository.UsuarioRepository.Filtros.buscaGeral;
import static com.pontoesabor.lanchonete.repository.UsuarioRepository.Filtros.comAtivo;
import static com.pontoesabor.lanchonete.repository.UsuarioRepository.Filtros.comPerfil;

import com.pontoesabor.lanchonete.dto.PaginaResponse;
import com.pontoesabor.lanchonete.dto.UsuarioAtualizacaoRequest;
import com.pontoesabor.lanchonete.dto.UsuarioCriacaoRequest;
import com.pontoesabor.lanchonete.dto.UsuarioResponse;
import com.pontoesabor.lanchonete.exception.RecursoNaoEncontradoException;
import com.pontoesabor.lanchonete.exception.RegraNegocioException;
import com.pontoesabor.lanchonete.model.Perfil;
import com.pontoesabor.lanchonete.model.Usuario;
import com.pontoesabor.lanchonete.repository.UsuarioRepository;
import com.pontoesabor.lanchonete.util.Textos;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class UsuarioService {

    private static final String MENSAGEM_DUPLICADO = "Já existe um funcionário com o mesmo CPF, usuário ou e-mail.";

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public PaginaResponse<UsuarioResponse> listar(String termo, Perfil perfil, Boolean ativo, Pageable pageable) {
        Specification<Usuario> filtro = Specification.allOf(List.of(
                buscaGeral(Textos.limpar(termo)), comPerfil(perfil), comAtivo(ativo)));
        return PaginaResponse.de(usuarioRepository.findAll(filtro, pageable).map(UsuarioResponse::de));
    }

    public UsuarioResponse buscar(Long id) {
        return UsuarioResponse.de(buscarEntidade(id));
    }

    @Transactional
    public UsuarioResponse criar(UsuarioCriacaoRequest request) {
        boolean duplicado = usuarioRepository.existsByUsername(request.username())
                || (request.email() != null && usuarioRepository.existsByEmail(request.email()))
                || (request.cpf() != null && usuarioRepository.existsByCpf(request.cpf()));
        if (duplicado) {
            throw new RegraNegocioException(MENSAGEM_DUPLICADO);
        }
        Usuario usuario = new Usuario(
                request.nome(), request.username(), passwordEncoder.encode(request.senha()), request.perfil());
        usuario.setEmail(request.email());
        usuario.setCpf(request.cpf());
        usuario.setTelefone(request.telefone());
        return UsuarioResponse.de(usuarioRepository.save(usuario));
    }

    @Transactional
    public UsuarioResponse atualizar(Long id, UsuarioAtualizacaoRequest request) {
        Usuario usuario = buscarEntidade(id);
        boolean duplicado = usuarioRepository.existsByUsernameAndIdNot(request.username(), id)
                || (request.email() != null && usuarioRepository.existsByEmailAndIdNot(request.email(), id))
                || (request.cpf() != null && usuarioRepository.existsByCpfAndIdNot(request.cpf(), id));
        if (duplicado) {
            throw new RegraNegocioException(MENSAGEM_DUPLICADO);
        }
        if (usuario.getPerfil() != request.perfil()) {
            garantirQueNaoEhUltimoDono(usuario);
        }
        usuario.setNome(request.nome());
        usuario.setUsername(request.username());
        usuario.setEmail(request.email());
        usuario.setCpf(request.cpf());
        usuario.setTelefone(request.telefone());
        usuario.setPerfil(request.perfil());
        if (request.senha() != null) {
            usuario.setSenhaHash(passwordEncoder.encode(request.senha()));
        }
        return UsuarioResponse.de(usuarioRepository.saveAndFlush(usuario));
    }

    /** Exclusão lógica: o usuário perde o acesso, mas continua ligado às vendas que registrou. */
    @Transactional
    public void inativar(Long id) {
        Usuario usuario = buscarEntidade(id);
        garantirQueNaoEhUltimoDono(usuario);
        usuario.setAtivo(false);
    }

    @Transactional
    public void ativar(Long id) {
        buscarEntidade(id).setAtivo(true);
    }

    /** O sistema precisa sempre de pelo menos um Dono ativo para gerenciar os usuários. */
    private void garantirQueNaoEhUltimoDono(Usuario usuario) {
        boolean ehDonoAtivo = usuario.getPerfil() == Perfil.DONO && usuario.isAtivo();
        if (ehDonoAtivo && usuarioRepository.countByPerfilAndAtivoTrue(Perfil.DONO) <= 1) {
            throw new RegraNegocioException("O sistema precisa de pelo menos um usuário Dono ativo.");
        }
    }

    private Usuario buscarEntidade(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário", id));
    }
}
