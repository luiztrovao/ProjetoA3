package com.pontoesabor.lanchonete.dto;

import com.pontoesabor.lanchonete.model.Cliente;
import java.time.LocalDateTime;

public record ClienteResponse(
        Long id,
        String nome,
        String cpf,
        String telefone,
        String email,
        String cep,
        String logradouro,
        String numero,
        String complemento,
        String bairro,
        String cidade,
        String uf,
        String observacoes,
        boolean ativo,
        LocalDateTime criadoEm,
        LocalDateTime atualizadoEm) {

    public static ClienteResponse de(Cliente cliente) {
        return new ClienteResponse(
                cliente.getId(),
                cliente.getNome(),
                cliente.getCpf(),
                cliente.getTelefone(),
                cliente.getEmail(),
                cliente.getCep(),
                cliente.getLogradouro(),
                cliente.getNumero(),
                cliente.getComplemento(),
                cliente.getBairro(),
                cliente.getCidade(),
                cliente.getUf(),
                cliente.getObservacoes(),
                cliente.isAtivo(),
                cliente.getCriadoEm(),
                cliente.getAtualizadoEm());
    }
}
