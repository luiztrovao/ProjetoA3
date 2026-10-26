package com.pontoesabor.lanchonete.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.pontoesabor.lanchonete.model.Perfil;
import com.pontoesabor.lanchonete.model.Usuario;
import com.pontoesabor.lanchonete.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/** Login com JWT e tela "Minha Conta", de ponta a ponta. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuthControllerTest {

    private static final String SENHA = "senha1";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Usuario fernanda;

    @BeforeEach
    void setUp() {
        fernanda = new Usuario("Fernanda Souza", "fernanda", passwordEncoder.encode(SENHA), Perfil.VENDEDOR);
        fernanda.setEmail("fernanda@pontoesabor.com");
        usuarioRepository.save(fernanda);
    }

    private String login(String login, String senha) throws Exception {
        String resposta = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"login": "%s", "senha": "%s"}
                                """.formatted(login, senha)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return resposta.replaceAll(".*?\"token\":\"([^\"]+)\".*", "$1");
    }

    @Test
    void loginPorNomeDeUsuarioDevolveTokenEUsuario() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"login": " Fernanda ", "senha": "senha1"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.tipo").value("Bearer"))
                .andExpect(jsonPath("$.expiraEm").isNotEmpty())
                .andExpect(jsonPath("$.usuario.username").value("fernanda"))
                .andExpect(jsonPath("$.usuario.perfil").value("VENDEDOR"))
                .andExpect(jsonPath("$.usuario.ultimoAcesso").isNotEmpty());

        assertThat(usuarioRepository.findByUsername("fernanda").orElseThrow().getUltimoAcesso()).isNotNull();
    }

    @Test
    void loginPorEmail() throws Exception {
        String token = login("FERNANDA@pontoesabor.com", SENHA);

        assertThat(token).isNotBlank();
    }

    @Test
    void senhaErradaDevolve401ComMensagemGenerica() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"login": "fernanda", "senha": "errada"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensagem").value("Usuário ou senha inválidos."));
    }

    @Test
    void usuarioInexistenteDevolveAMesmaMensagem() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"login": "ninguem", "senha": "senha1"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensagem").value("Usuário ou senha inválidos."));
    }

    @Test
    void usuarioInativoNaoEntra() throws Exception {
        fernanda.setAtivo(false);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"login": "fernanda", "senha": "senha1"}
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void loginSemCamposDevolve400() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos[*].campo", hasItem("login")))
                .andExpect(jsonPath("$.campos[*].campo", hasItem("senha")));
    }

    @Test
    void semTokenDevolve401() throws Exception {
        mockMvc.perform(get("/api/conta"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensagem").value("Faça login para acessar esta funcionalidade."));
    }

    @Test
    void tokenInvalidoDevolve401() throws Exception {
        mockMvc.perform(get("/api/conta").header(HttpHeaders.AUTHORIZATION, "Bearer token.falso.assinatura"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tokenValidoAcessaMinhaConta() throws Exception {
        String token = login("fernanda", SENHA);

        mockMvc.perform(get("/api/conta").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Fernanda Souza"))
                .andExpect(jsonPath("$.senhaHash").doesNotExist());
    }

    @Test
    void tokenDeUsuarioInativadoDepoisDoLoginPerdeOAcesso() throws Exception {
        String token = login("fernanda", SENHA);
        fernanda.setAtivo(false);
        usuarioRepository.flush();

        mockMvc.perform(get("/api/conta").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void atualizaOsPropriosDados() throws Exception {
        String token = login("fernanda", SENHA);

        mockMvc.perform(put("/api/conta")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome": "Fernanda S. Lima", "username": "fernanda.lima", "telefone": "(81) 97766-3300"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Fernanda S. Lima"))
                .andExpect(jsonPath("$.username").value("fernanda.lima"))
                .andExpect(jsonPath("$.perfil").value("VENDEDOR"));
    }

    @Test
    void alteraASenhaENovaSenhaPassaAValer() throws Exception {
        String token = login("fernanda", SENHA);

        mockMvc.perform(put("/api/conta/senha")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"senhaAtual": "senha1", "novaSenha": "novaSenha9", "confirmacaoSenha": "novaSenha9"}
                                """))
                .andExpect(status().isNoContent());

        assertThat(login("fernanda", "novaSenha9")).isNotBlank();
    }

    @Test
    void recusaSenhaAtualErradaEConfirmacaoDiferente() throws Exception {
        String token = login("fernanda", SENHA);

        mockMvc.perform(put("/api/conta/senha")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"senhaAtual": "errada", "novaSenha": "novaSenha9", "confirmacaoSenha": "novaSenha9"}
                                """))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.mensagem").value("A senha atual está incorreta."));

        mockMvc.perform(put("/api/conta/senha")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"senhaAtual": "senha1", "novaSenha": "novaSenha9", "confirmacaoSenha": "outra123"}
                                """))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.mensagem").value("A confirmação da nova senha não confere."));
    }
}
