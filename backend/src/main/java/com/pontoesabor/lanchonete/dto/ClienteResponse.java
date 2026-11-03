package com.pontoesabor.lanchonete.dto;

import com.pontoesabor.lanchonete.model.Cliente;
import com.pontoesabor.lanchonete.model.Contato;
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
        Contato contato = cliente.getContato();
        return new ClienteResponse(
                cliente.getId(),
                cliente.getNome(),
                cliente.getCpf(),
                contato.telefone(),
                contato.email(),
                contato.cep(),
                contato.logradouro(),
                contato.numero(),
                cliente.getComplemento(),
                contato.bairro(),
                contato.cidade(),
                contato.uf(),
                cliente.getObservacoes(),
                cliente.isAtivo(),
                cliente.getCriadoEm(),
                cliente.getAtualizadoEm());
    }
}
