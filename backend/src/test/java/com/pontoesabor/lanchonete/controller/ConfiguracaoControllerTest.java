package com.pontoesabor.lanchonete.controller;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/** Testes de ponta a ponta da API de configurações. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ConfiguracaoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void consultaConfiguracoesPadrao() throws Exception {
        mockMvc.perform(get("/api/configuracoes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.empresa.nomeFantasia").value("Ponto & Sabor"))
                .andExpect(jsonPath("$.preferenciasVenda.controleEstoque").value(true))
                .andExpect(jsonPath("$.preferenciasVenda.alertaEstoqueBaixo").value(5))
                .andExpect(jsonPath("$.preferenciasVenda.statusInicialVenda").value("EM_PREPARO"))
                .andExpect(jsonPath("$.preferenciasVenda.formasPagamento",
                        containsInAnyOrder("DINHEIRO", "PIX", "CREDITO", "DEBITO")));
    }

    @Test
    void atualizaDadosDaLanchonete() throws Exception {
        mockMvc.perform(put("/api/configuracoes/empresa")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nomeFantasia": "Ponto & Sabor Boa Viagem", "cnpj": "11.222.333/0001-81",
                                 "telefone": "(81) 3333-4444", "cep": "51020-030", "cidade": "Recife", "uf": "pe"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.empresa.nomeFantasia").value("Ponto & Sabor Boa Viagem"))
                .andExpect(jsonPath("$.empresa.cnpj").value("11222333000181"))
                .andExpect(jsonPath("$.empresa.telefone").value("8133334444"))
                .andExpect(jsonPath("$.empresa.uf").value("PE"));
    }

    @Test
    void recusaCnpjInvalidoENomeVazio() throws Exception {
        mockMvc.perform(put("/api/configuracoes/empresa")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nomeFantasia": "", "cnpj": "11.111.111/1111-11"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos[*].campo", hasItem("nomeFantasia")))
                .andExpect(jsonPath("$.campos[*].campo", hasItem("cnpj")));
    }

    @Test
    void atualizaPreferenciasDeVenda() throws Exception {
        mockMvc.perform(put("/api/configuracoes/vendas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"controleEstoque": true, "bloquearSemEstoque": false, "alertaEstoqueBaixo": 3,
                                 "confirmarCancelamento": true, "permitirVendaSemCliente": false,
                                 "statusInicialVenda": "CONCLUIDA", "formasPagamento": ["PIX", "DINHEIRO"]}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.preferenciasVenda.bloquearSemEstoque").value(false))
                .andExpect(jsonPath("$.preferenciasVenda.alertaEstoqueBaixo").value(3))
                .andExpect(jsonPath("$.preferenciasVenda.permitirVendaSemCliente").value(false))
                .andExpect(jsonPath("$.preferenciasVenda.statusInicialVenda").value("CONCLUIDA"))
                .andExpect(jsonPath("$.preferenciasVenda.formasPagamento", containsInAnyOrder("PIX", "DINHEIRO")));
    }

    @Test
    void recusaPreferenciasSemFormaDePagamento() throws Exception {
        mockMvc.perform(put("/api/configuracoes/vendas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"controleEstoque": true, "bloquearSemEstoque": true, "alertaEstoqueBaixo": 5,
                                 "confirmarCancelamento": true, "permitirVendaSemCliente": true,
                                 "statusInicialVenda": "EM_PREPARO", "formasPagamento": []}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos[*].campo", hasItem("formasPagamento")));
    }

    @Test
    void recusaStatusInicialCancelada() throws Exception {
        mockMvc.perform(put("/api/configuracoes/vendas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"controleEstoque": true, "bloquearSemEstoque": true, "alertaEstoqueBaixo": 5,
                                 "confirmarCancelamento": true, "permitirVendaSemCliente": true,
                                 "statusInicialVenda": "CANCELADA", "formasPagamento": ["PIX"]}
                                """))
                .andExpect(status().isUnprocessableContent());
    }
}
