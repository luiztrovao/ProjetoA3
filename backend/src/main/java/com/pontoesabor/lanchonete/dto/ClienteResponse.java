package com.pontoesabor.lanchonete.dto;

import com.pontoesabor.lanchonete.model.Cliente;
import java.time.LocalDateTime;

public record ClienteResponse(
        Long id,
        String nome,
        String cpf,
        String telefone,
        String email,
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
                cliente.isAtivo(),
                cliente.getCriadoEm(),
                cliente.getAtualizadoEm());
    }
}
