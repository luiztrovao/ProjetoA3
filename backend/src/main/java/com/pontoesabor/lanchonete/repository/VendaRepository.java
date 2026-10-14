package com.pontoesabor.lanchonete.repository;

import com.pontoesabor.lanchonete.model.StatusVenda;
import com.pontoesabor.lanchonete.model.Venda;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VendaRepository extends JpaRepository<Venda, Long> {

    List<Venda> findByStatusAndDataHoraBetween(StatusVenda status, LocalDateTime inicio, LocalDateTime fim);
}
