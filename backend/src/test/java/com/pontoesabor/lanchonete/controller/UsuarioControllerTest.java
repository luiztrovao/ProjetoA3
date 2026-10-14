package com.pontoesabor.lanchonete.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.pontoesabor.lanchonete.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/** Testes de ponta a ponta da API de usuários. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class UsuarioControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private long criarUsuario(String nome, String email, String perfil) throws Exception {
        String resposta = mockMvc.perform(post("/api/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome": "%s", "email": "%s", "senha": "senhaSegura1", "perfil": "%s"}
                                """.formatted(nome, email, perfil)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return Long.parseLong(resposta.replaceAll(".*?\"id\":(\\d+).*", "$1"));
    }

    @Test
    void criaUsuarioSemExporASenha() throws Exception {
        mockMvc.perform(post("/api/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome": "Ana", "email": "ANA@pontoesabor.com", "senha": "senhaSegura1", "perfil": "VENDEDOR"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("ana@pontoesabor.com"))
                .andExpect(jsonPath("$.perfil").value("VENDEDOR"))
                .andExpect(jsonPath("$.senha").doesNotExist())
                .andExpect(jsonPath("$.senhaHash").doesNotExist());

        String hash = usuarioRepository.findByEmail("ana@pontoesabor.com").orElseThrow().getSenhaHash();
        assertThat(hash).isNotEqualTo("senhaSegura1");
        assertThat(passwordEncoder.matches("senhaSegura1", hash)).isTrue();
    }

    @Test
    void recusaSenhaCurtaEPerfilAusente() throws Exception {
        mockMvc.perform(post("/api/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome": "Ana", "email": "ana@pontoesabor.com", "senha": "123"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos[*].campo", hasItem("senha")))
                .andExpect(jsonPath("$.campos[*].campo", hasItem("perfil")));
    }

    @Test
    void recusaPerfilInexistente() throws Exception {
        mockMvc.perform(post("/api/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome": "Ana", "email": "ana@pontoesabor.com", "senha": "senhaSegura1", "perfil": "CAIXA"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void recusaEmailDuplicado() throws Exception {
        criarUsuario("Ana", "ana@pontoesabor.com", "VENDEDOR");

        mockMvc.perform(post("/api/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome": "Outra Ana", "email": "ana@pontoesabor.com", "senha": "senhaSegura1", "perfil": "GERENTE"}
                                """))
                .andExpect(status().isUnprocessableContent());
    }

    @Test
    void filtraPorPerfil() throws Exception {
        criarUsuario("Gerente Teste", "gerente@pontoesabor.com", "GERENTE");
        criarUsuario("Vendedor Teste", "vendedor@pontoesabor.com", "VENDEDOR");

        mockMvc.perform(get("/api/usuarios").param("perfil", "GERENTE").param("termo", "teste"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElementos").value(1))
                .andExpect(jsonPath("$.conteudo[0].nome").value("Gerente Teste"));
    }

    @Test
    void naoPermiteInativarOUnicoDono() throws Exception {
        long dono = criarUsuario("Matheus", "matheus@pontoesabor.com", "DONO");

        mockMvc.perform(delete("/api/usuarios/" + dono))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.mensagem").value("O sistema precisa de pelo menos um usuário Dono ativo."));
    }

    @Test
    void atualizaUsuarioETrocaSenha() throws Exception {
        long id = criarUsuario("Rafael", "rafael@pontoesabor.com", "VENDEDOR");

        mockMvc.perform(put("/api/usuarios/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome": "Rafael Costa", "email": "rafael@pontoesabor.com", "senha": "novaSenha123", "perfil": "GERENTE"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Rafael Costa"))
                .andExpect(jsonPath("$.perfil").value("GERENTE"));

        String hash = usuarioRepository.findById(id).orElseThrow().getSenhaHash();
        assertThat(passwordEncoder.matches("novaSenha123", hash)).isTrue();
    }
}
