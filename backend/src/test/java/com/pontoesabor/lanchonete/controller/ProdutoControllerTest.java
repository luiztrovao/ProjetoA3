package com.pontoesabor.lanchonete.controller;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.pontoesabor.lanchonete.repository.CategoriaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/** Testes de ponta a ponta da API de produtos (HTTP → controller → service → banco H2). */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ProdutoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CategoriaRepository categoriaRepository;

    private Long lanchesId;

    @BeforeEach
    void setUp() {
        lanchesId = categoriaRepository.findByAtivoTrueOrderByNome().stream()
                .filter(c -> c.getNome().equals("Lanches"))
                .findFirst()
                .orElseThrow()
                .getId();
    }

    private String produtoJson(String nome, String preco, int estoqueAtual, int estoqueMinimo) {
        return """
                {"nome": "%s", "descricao": "Lanche da casa", "categoriaId": %d,
                 "preco": %s, "estoqueAtual": %d, "estoqueMinimo": %d}
                """.formatted(nome, lanchesId, preco, estoqueAtual, estoqueMinimo);
    }

    private long criarProduto(String nome) throws Exception {
        String resposta = mockMvc.perform(post("/api/produtos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(produtoJson(nome, "25.90", 10, 2)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return Long.parseLong(resposta.replaceAll(".*?\"id\":(\\d+).*", "$1"));
    }

    @Test
    void listaCategorias() throws Exception {
        mockMvc.perform(get("/api/categorias"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].nome", hasItem("Lanches")));
    }

    @Test
    void criaProdutoEDevolveLocation() throws Exception {
        mockMvc.perform(post("/api/produtos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(produtoJson("X-Salada", "27.50", 3, 5)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/api/produtos/")))
                .andExpect(jsonPath("$.nome").value("X-Salada"))
                .andExpect(jsonPath("$.categoria.nome").value("Lanches"))
                .andExpect(jsonPath("$.preco").value(27.50))
                .andExpect(jsonPath("$.estoqueBaixo").value(true))
                .andExpect(jsonPath("$.ativo").value(true));
    }

    @Test
    void recusaProdutoInvalidoInformandoOsCampos() throws Exception {
        mockMvc.perform(post("/api/produtos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome": "", "preco": 0, "estoqueAtual": -1, "estoqueMinimo": 0}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos[*].campo", hasItem("nome")))
                .andExpect(jsonPath("$.campos[*].campo", hasItem("preco")))
                .andExpect(jsonPath("$.campos[*].campo", hasItem("categoriaId")))
                .andExpect(jsonPath("$.campos[*].campo", hasItem("estoqueAtual")));
    }

    @Test
    void recusaCategoriaInexistente() throws Exception {
        mockMvc.perform(post("/api/produtos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(produtoJson("X-Bacon", "30.00", 5, 1).replace("\"categoriaId\": " + lanchesId,
                                "\"categoriaId\": 9999")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensagem").value(containsString("Categoria")));
    }

    @Test
    void atualizaProduto() throws Exception {
        long id = criarProduto("X-Egg");

        mockMvc.perform(put("/api/produtos/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(produtoJson("X-Egg Especial", "29.90", 20, 2)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("X-Egg Especial"))
                .andExpect(jsonPath("$.preco").value(29.90))
                .andExpect(jsonPath("$.estoqueAtual").value(20));
    }

    @Test
    void inativaProdutoEFiltraPorAtivo() throws Exception {
        long id = criarProduto("X-Tudo");

        mockMvc.perform(delete("/api/produtos/" + id)).andExpect(status().isNoContent());

        mockMvc.perform(get("/api/produtos/" + id))
                .andExpect(jsonPath("$.ativo").value(false));
        mockMvc.perform(get("/api/produtos").param("termo", "x-tudo").param("ativo", "true"))
                .andExpect(jsonPath("$.totalElementos").value(0));
        mockMvc.perform(get("/api/produtos").param("termo", "x-tudo").param("ativo", "false"))
                .andExpect(jsonPath("$.totalElementos").value(1));
    }

    @Test
    void listaComPaginacao() throws Exception {
        criarProduto("Produto A");
        criarProduto("Produto B");
        criarProduto("Produto C");

        mockMvc.perform(get("/api/produtos").param("termo", "produto").param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conteudo.length()").value(2))
                .andExpect(jsonPath("$.conteudo[0].nome").value("Produto A"))
                .andExpect(jsonPath("$.totalElementos").value(3))
                .andExpect(jsonPath("$.totalPaginas").value(2));
    }

    @Test
    void produtoInexistenteDevolve404() throws Exception {
        mockMvc.perform(get("/api/produtos/9999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void ordenacaoPorCampoInexistenteDevolve400() throws Exception {
        mockMvc.perform(get("/api/produtos").param("sort", "campoQueNaoExiste"))
                .andExpect(status().isBadRequest());
    }
}
