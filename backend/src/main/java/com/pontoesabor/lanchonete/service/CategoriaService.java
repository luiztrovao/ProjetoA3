package com.pontoesabor.lanchonete.service;

import com.pontoesabor.lanchonete.dto.CategoriaResponse;
import com.pontoesabor.lanchonete.exception.RecursoNaoEncontradoException;
import com.pontoesabor.lanchonete.exception.RegraNegocioException;
import com.pontoesabor.lanchonete.model.Categoria;
import com.pontoesabor.lanchonete.repository.CategoriaRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class CategoriaService {

    private final CategoriaRepository categoriaRepository;

    public CategoriaService(CategoriaRepository categoriaRepository) {
        this.categoriaRepository = categoriaRepository;
    }

    public List<CategoriaResponse> listarAtivas() {
        return categoriaRepository.findByAtivoTrueOrderByNome().stream()
                .map(CategoriaResponse::de)
                .toList();
    }

    /** Busca uma categoria que possa receber produtos (existente e ativa). */
    Categoria buscarAtiva(Long id) {
        Categoria categoria = categoriaRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Categoria", id));
        if (!categoria.isAtivo()) {
            throw new RegraNegocioException("A categoria \"" + categoria.getNome() + "\" está inativa.");
        }
        return categoria;
    }
}
