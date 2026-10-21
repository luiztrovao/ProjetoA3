package com.pontoesabor.lanchonete.controller;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/** Testes de ponta a ponta da API de clientes. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ClienteControllerTest {

    private static final String CPF_VALIDO = "529.982.247-25";

    @Autowired
    private MockMvc mockMvc;

    private long criarCliente(String nome, String cpf, String telefone) throws Exception {
        String resposta = mockMvc.perform(post("/api/clientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome": "%s", "cpf": %s, "telefone": "%s", "email": "cliente@email.com"}
                                """.formatted(nome, cpf == null ? "null" : "\"" + cpf + "\"", telefone)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return Long.parseLong(resposta.replaceAll(".*?\"id\":(\\d+).*", "$1"));
    }

    @Test
    void criaClienteNormalizandoCpfETelefone() throws Exception {
        mockMvc.perform(post("/api/clientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome": "João Silva", "cpf": "529.982.247-25", "telefone": "(81) 99999-1234"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.cpf").value("52998224725"))
                .andExpect(jsonPath("$.telefone").value("81999991234"))
                .andExpect(jsonPath("$.ativo").value(true));
    }

    @Test
    void criaClienteSemCpf() throws Exception {
        criarCliente("Cliente sem CPF 1", null, "81988887777");
        criarCliente("Cliente sem CPF 2", null, "81988887766");
    }

    @Test
    void recusaCpfInvalido() throws Exception {
        mockMvc.perform(post("/api/clientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome": "Maria", "cpf": "123.456.789-00", "telefone": "123"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos[*].campo", hasItem("cpf")))
                .andExpect(jsonPath("$.campos[*].campo", hasItem("telefone")));
    }

    @Test
    void recusaCpfDuplicado() throws Exception {
        criarCliente("Primeiro", CPF_VALIDO, "81999990000");

        mockMvc.perform(post("/api/clientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome": "Segundo", "cpf": "52998224725"}
                                """))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.mensagem").value("Já existe um cliente com este CPF."));
    }

    @Test
    void buscaPorNomeCpfOuTelefone() throws Exception {
        criarCliente("Pedro Lima", CPF_VALIDO, "81977774321");

        mockMvc.perform(get("/api/clientes").param("termo", "pedro"))
                .andExpect(jsonPath("$.totalElementos").value(1));
        mockMvc.perform(get("/api/clientes").param("termo", "529.982"))
                .andExpect(jsonPath("$.totalElementos").value(1));
        mockMvc.perform(get("/api/clientes").param("termo", "(81) 97777"))
                .andExpect(jsonPath("$.totalElementos").value(1));
        mockMvc.perform(get("/api/clientes").param("termo", "inexistente"))
                .andExpect(jsonPath("$.totalElementos").value(0));
    }

    @Test
    void atualizaInativaEReativaCliente() throws Exception {
        long id = criarCliente("Ana Souza", null, "81988885678");

        mockMvc.perform(put("/api/clientes/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome": "Ana Souza Lima", "telefone": "81988885678"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Ana Souza Lima"))
                .andExpect(jsonPath("$.email").doesNotExist());

        mockMvc.perform(delete("/api/clientes/" + id)).andExpect(status().isNoContent());
        mockMvc.perform(get("/api/clientes/" + id)).andExpect(jsonPath("$.ativo").value(false));

        mockMvc.perform(patch("/api/clientes/" + id + "/ativar")).andExpect(status().isNoContent());
        mockMvc.perform(get("/api/clientes/" + id)).andExpect(jsonPath("$.ativo").value(true));
    }

    @Test
    void corpoMalFormatadoDevolve400() throws Exception {
        mockMvc.perform(post("/api/clientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ isso não é json"))
                .andExpect(status().isBadRequest());
    }
}
