package com.pontoesabor.lanchonete.repository;

import com.pontoesabor.lanchonete.model.Categoria;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoriaRepository extends JpaRepository<Categoria, Long> {

    List<Categoria> findByAtivoTrueOrderByNome();
}
