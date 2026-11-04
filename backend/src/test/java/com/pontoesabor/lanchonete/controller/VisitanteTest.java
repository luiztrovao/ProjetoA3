package com.pontoesabor.lanchonete.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.pontoesabor.lanchonete.model.Perfil;
import com.pontoesabor.lanchonete.model.Usuario;
import com.pontoesabor.lanchonete.repository.UsuarioRepository;
import com.pontoesabor.lanchonete.security.TokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

/**
 * Perfil Visitante (acesso dos alunos na apresentação): lê a tela Relatórios e nada mais.
 * Teste baseado em risco: o Visitante usa uma conta compartilhada, então qualquer escrita precisa ser barrada.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class VisitanteTest {

    private static final String SEM_PERMISSAO = "Você não possui permissão para acessar esta funcionalidade.";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private TokenService tokenService;

    private String tokenVisitante;
    private String tokenDono;

    @BeforeEach
    void setUp() {
        tokenVisitante = tokenService.gerar(usuarioRepository.save(new Usuario("Visitante", "visitante", "hash", Perfil.VISITANTE))).token();
        tokenDono = tokenService.gerar(usuarioRepository.save(new Usuario("Matheus", "matheus", "hash", Perfil.DONO))).token();
    }

    private MockHttpServletRequestBuilder como(String token, MockHttpServletRequestBuilder requisicao) {
        return requisicao.header(HttpHeaders.AUTHORIZATION, "Bearer " + token).contentType(MediaType.APPLICATION_JSON);
    }

    @ParameterizedTest(name = "GET {0} é permitido")
    @CsvSource({
        "/api/relatorios/resumo",
        "/api/relatorios/picos?periodo=SEMANA",
        "/api/vendas",
        "/api/conta",
        "/api/configuracoes",
    })
    void visitanteLeOQueATelaRelatoriosPrecisa(String rota) throws Exception {
        mockMvc.perform(como(tokenVisitante, get(rota))).andExpect(status().isOk());
    }

    @ParameterizedTest(name = "GET {0} é proibido")
    @CsvSource({
        "/api/dashboard",
        "/api/clientes",
        "/api/produtos",
        "/api/categorias",
        "/api/usuarios",
        "/api/notificacoes",
        "/api/vendas/1",
    })
    void visitanteNaoLeOutrasTelas(String rota) throws Exception {
        mockMvc.perform(como(tokenVisitante, get(rota)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.mensagem").value(SEM_PERMISSAO));
    }

    @Test
    void visitanteNaoAlteraNada() throws Exception {
        mockMvc.perform(como(tokenVisitante, post("/api/clientes")).content("{\"nome\": \"Ana\"}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(como(tokenVisitante, post("/api/vendas")).content("{}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(como(tokenVisitante, patch("/api/vendas/1/cancelar")).content("{\"motivo\": \"x\"}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(como(tokenVisitante, put("/api/configuracoes/empresa")).content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void visitanteNaoTrocaOsDadosNemASenhaDaContaCompartilhada() throws Exception {
        mockMvc.perform(como(tokenVisitante, put("/api/conta"))
                        .content("{\"nome\": \"Outro\", \"username\": \"outro\"}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(como(tokenVisitante, put("/api/conta/senha"))
                        .content("{\"senhaAtual\": \"x\", \"novaSenha\": \"123456\", \"confirmacaoSenha\": \"123456\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void donoCadastraUmVisitante() throws Exception {
        String corpo = """
                {"nome": "Turma de Qualidade", "username": "turma", "senha": "turma2026", "perfil": "VISITANTE"}
                """;
        mockMvc.perform(como(tokenDono, post("/api/usuarios")).content(corpo))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.perfil").value("VISITANTE"));
    }
}
