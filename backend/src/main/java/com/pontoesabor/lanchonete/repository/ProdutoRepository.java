package com.pontoesabor.lanchonete.repository;

import com.pontoesabor.lanchonete.model.Produto;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProdutoRepository extends JpaRepository<Produto, Long> {

    List<Produto> findByAtivoTrueOrderByNome();
}
