package com.pontoesabor.lanchonete.service;

import static com.pontoesabor.lanchonete.repository.ClienteRepository.Filtros.buscaGeral;
import static com.pontoesabor.lanchonete.repository.ClienteRepository.Filtros.comAtivo;

import com.pontoesabor.lanchonete.dto.ClienteRequest;
import com.pontoesabor.lanchonete.dto.ClienteResponse;
import com.pontoesabor.lanchonete.dto.PaginaResponse;
import com.pontoesabor.lanchonete.exception.RecursoNaoEncontradoException;
import com.pontoesabor.lanchonete.exception.RegraNegocioException;
import com.pontoesabor.lanchonete.model.Cliente;
import com.pontoesabor.lanchonete.repository.ClienteRepository;
import com.pontoesabor.lanchonete.util.Textos;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ClienteService {

    private final ClienteRepository clienteRepository;

    public ClienteService(ClienteRepository clienteRepository) {
        this.clienteRepository = clienteRepository;
    }

    public PaginaResponse<ClienteResponse> listar(String termo, Boolean ativo, Pageable pageable) {
        Specification<Cliente> filtro = Specification.allOf(List.of(buscaGeral(Textos.limpar(termo)), comAtivo(ativo)));
        return PaginaResponse.de(clienteRepository.findAll(filtro, pageable).map(ClienteResponse::de));
    }

    public ClienteResponse buscar(Long id) {
        return ClienteResponse.de(buscarEntidade(id));
    }

    @Transactional
    public ClienteResponse criar(ClienteRequest request) {
        if (request.cpf() != null && clienteRepository.existsByCpf(request.cpf())) {
            throw new RegraNegocioException("Já existe um cliente com este CPF.");
        }
        Cliente cliente = new Cliente(request.nome());
        preencher(cliente, request);
        return ClienteResponse.de(clienteRepository.save(cliente));
    }

    @Transactional
    public ClienteResponse atualizar(Long id, ClienteRequest request) {
        Cliente cliente = buscarEntidade(id);
        if (request.cpf() != null && clienteRepository.existsByCpfAndIdNot(request.cpf(), id)) {
            throw new RegraNegocioException("Já existe um cliente com este CPF.");
        }
        cliente.setNome(request.nome());
        preencher(cliente, request);
        return ClienteResponse.de(clienteRepository.saveAndFlush(cliente));
    }

    /** Exclusão lógica: o cliente continua ligado às vendas antigas. */
    @Transactional
    public void inativar(Long id) {
        buscarEntidade(id).setAtivo(false);
    }

    @Transactional
    public void ativar(Long id) {
        buscarEntidade(id).setAtivo(true);
    }

    private static void preencher(Cliente cliente, ClienteRequest request) {
        cliente.setCpf(request.cpf());
        cliente.setTelefone(request.telefone());
        cliente.setEmail(request.email());
        cliente.setCep(request.cep());
        cliente.setLogradouro(request.logradouro());
        cliente.setNumero(request.numero());
        cliente.setComplemento(request.complemento());
        cliente.setBairro(request.bairro());
        cliente.setCidade(request.cidade());
        cliente.setUf(request.uf());
        cliente.setObservacoes(request.observacoes());
    }

    private Cliente buscarEntidade(Long id) {
        return clienteRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Cliente", id));
    }
}
