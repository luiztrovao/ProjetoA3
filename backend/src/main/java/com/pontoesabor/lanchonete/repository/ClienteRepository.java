package com.pontoesabor.lanchonete.repository;

import com.pontoesabor.lanchonete.model.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClienteRepository extends JpaRepository<Cliente, Long> {

    boolean existsByCpf(String cpf);
}
