package com.pontoesabor.lanchonete.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

/** Rota de ping usada para manter a API acordada no Render. */
@SpringBootTest
@AutoConfigureMockMvc
class PublicoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void pingNaoExigeLoginEDevolveRespostaVazia() throws Exception {
        mockMvc.perform(get("/api/publico/ping"))
                .andExpect(status().isNoContent())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(content().string(""));
    }
}
