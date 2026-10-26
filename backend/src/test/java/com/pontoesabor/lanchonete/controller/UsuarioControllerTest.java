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

/** Testes de ponta a ponta da API de usuários (funcionários). */
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

    private long criarUsuario(String nome, String username, String perfil) throws Exception {
        String resposta = mockMvc.perform(post("/api/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome": "%s", "username": "%s", "senha": "senha1", "perfil": "%s"}
                                """.formatted(nome, username, perfil)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return Long.parseLong(resposta.replaceAll(".*?\"id\":(\\d+).*", "$1"));
    }

    @Test
    void criaUsuarioCompletoSemExporASenha() throws Exception {
        mockMvc.perform(post("/api/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome": "Fernanda Souza", "username": "Fernanda", "email": "FERNANDA@pontoesabor.com",
                                 "cpf": "529.982.247-25", "telefone": "(81) 97766-3300", "senha": "senha1", "perfil": "VENDEDOR"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("fernanda"))
                .andExpect(jsonPath("$.email").value("fernanda@pontoesabor.com"))
                .andExpect(jsonPath("$.cpf").value("52998224725"))
                .andExpect(jsonPath("$.telefone").value("81977663300"))
                .andExpect(jsonPath("$.perfil").value("VENDEDOR"))
                .andExpect(jsonPath("$.ultimoAcesso").doesNotExist())
                .andExpect(jsonPath("$.senha").doesNotExist())
                .andExpect(jsonPath("$.senhaHash").doesNotExist());

        String hash = usuarioRepository.findByUsername("fernanda").orElseThrow().getSenhaHash();
        assertThat(hash).isNotEqualTo("senha1");
        assertThat(passwordEncoder.matches("senha1", hash)).isTrue();
    }

    @Test
    void criaUsuarioSemEmail() throws Exception {
        criarUsuario("Carlos Lima", "carlos", "GERENTE");
        criarUsuario("Rafael Mendes", "rafael", "VENDEDOR");
    }

    @Test
    void recusaCamposInvalidos() throws Exception {
        mockMvc.perform(post("/api/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome": "Ana", "username": "a b", "cpf": "111.111.111-11", "senha": "123"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos[*].campo", hasItem("username")))
                .andExpect(jsonPath("$.campos[*].campo", hasItem("cpf")))
                .andExpect(jsonPath("$.campos[*].campo", hasItem("senha")))
                .andExpect(jsonPath("$.campos[*].campo", hasItem("perfil")));
    }

    @Test
    void recusaPerfilInexistente() throws Exception {
        mockMvc.perform(post("/api/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome": "Ana", "username": "ana", "senha": "senha1", "perfil": "CAIXA"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void recusaNomeDeUsuarioDuplicado() throws Exception {
        criarUsuario("Ana", "ana", "VENDEDOR");

        mockMvc.perform(post("/api/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome": "Outra Ana", "username": "ANA", "senha": "senha1", "perfil": "GERENTE"}
                                """))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.mensagem").value("Já existe um funcionário com o mesmo CPF, usuário ou e-mail."));
    }

    @Test
    void filtraPorPerfilEBuscaPorUsuario() throws Exception {
        criarUsuario("Gerente Teste", "gerente.teste", "GERENTE");
        criarUsuario("Vendedor Teste", "vendedor.teste", "VENDEDOR");

        mockMvc.perform(get("/api/usuarios").param("perfil", "GERENTE").param("termo", "teste"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElementos").value(1))
                .andExpect(jsonPath("$.conteudo[0].nome").value("Gerente Teste"));
        mockMvc.perform(get("/api/usuarios").param("termo", "vendedor.tes"))
                .andExpect(jsonPath("$.totalElementos").value(1));
    }

    @Test
    void naoPermiteInativarOUnicoDono() throws Exception {
        long dono = criarUsuario("Matheus", "matheus", "DONO");

        mockMvc.perform(delete("/api/usuarios/" + dono))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.mensagem").value("O sistema precisa de pelo menos um usuário Dono ativo."));
    }

    @Test
    void atualizaUsuarioETrocaSenha() throws Exception {
        long id = criarUsuario("Rafael", "rafael", "VENDEDOR");

        mockMvc.perform(put("/api/usuarios/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome": "Rafael Costa", "username": "rafael.costa", "senha": "novaSenha123", "perfil": "GERENTE"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Rafael Costa"))
                .andExpect(jsonPath("$.username").value("rafael.costa"))
                .andExpect(jsonPath("$.perfil").value("GERENTE"));

        String hash = usuarioRepository.findById(id).orElseThrow().getSenhaHash();
        assertThat(passwordEncoder.matches("novaSenha123", hash)).isTrue();
    }
}
