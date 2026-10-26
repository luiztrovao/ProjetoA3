package com.pontoesabor.lanchonete.repository;

import com.pontoesabor.lanchonete.model.Imagem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ImagemRepository extends JpaRepository<Imagem, Long> {
}
