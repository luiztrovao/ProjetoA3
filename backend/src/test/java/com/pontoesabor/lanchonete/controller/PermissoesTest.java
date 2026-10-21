package com.pontoesabor.lanchonete.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.pontoesabor.lanchonete.model.Cliente;
import com.pontoesabor.lanchonete.model.Perfil;
import com.pontoesabor.lanchonete.model.Usuario;
import com.pontoesabor.lanchonete.repository.ClienteRepository;
import com.pontoesabor.lanchonete.repository.UsuarioRepository;
import com.pontoesabor.lanchonete.security.TokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

/**
 * Confere a tabela "Usuários e Permissões" do protótipo com tokens JWT reais de cada perfil.
 * Veja docs/modelagem/modelo-de-dados.md.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PermissoesTest {

    private static final String SEM_PERMISSAO = "Você não possui permissão para acessar esta funcionalidade.";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private TokenService tokenService;

    private String tokenDono;
    private String tokenGerente;
    private String tokenVendedor;
    private Long idCliente;

    @BeforeEach
    void setUp() {
        tokenDono = token(new Usuario("Matheus", "matheus", "hash", Perfil.DONO));
        tokenGerente = token(new Usuario("Carlos", "carlos", "hash", Perfil.GERENTE));
        tokenVendedor = token(new Usuario("Fernanda", "fernanda", "hash", Perfil.VENDEDOR));
        idCliente = clienteRepository.save(new Cliente("João Silva")).getId();
    }

    private String token(Usuario usuario) {
        return tokenService.gerar(usuarioRepository.save(usuario)).token();
    }

    private MockHttpServletRequestBuilder como(String token, MockHttpServletRequestBuilder requisicao) {
        return requisicao.header(HttpHeaders.AUTHORIZATION, "Bearer " + token).contentType(MediaType.APPLICATION_JSON);
    }

    // ===== Vendedor =====

    @Test
    void vendedorConsultaECadastraClientes() throws Exception {
        mockMvc.perform(como(tokenVendedor, get("/api/clientes"))).andExpect(status().isOk());
        mockMvc.perform(como(tokenVendedor, post("/api/clientes")).content("""
                        {"nome": "Cliente novo"}
                        """))
                .andExpect(status().isCreated());
    }

    @Test
    void vendedorNaoEditaNemDesativaClientes() throws Exception {
        mockMvc.perform(como(tokenVendedor, put("/api/clientes/" + idCliente)).content("""
                        {"nome": "Outro nome"}
                        """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.mensagem").value(SEM_PERMISSAO));
        mockMvc.perform(como(tokenVendedor, delete("/api/clientes/" + idCliente))).andExpect(status().isForbidden());
    }

    @Test
    void vendedorConsultaProdutosMasNaoGerencia() throws Exception {
        mockMvc.perform(como(tokenVendedor, get("/api/produtos"))).andExpect(status().isOk());
        mockMvc.perform(como(tokenVendedor, post("/api/produtos")).content("""
                        {"nome": "X-Bacon", "codigo": "XBC001", "categoriaId": 1, "preco": 20}
                        """))
                .andExpect(status().isForbidden());
    }

    @Test
    void vendedorNaoVeFuncionariosNemAlteraConfiguracoes() throws Exception {
        mockMvc.perform(como(tokenVendedor, get("/api/usuarios"))).andExpect(status().isForbidden());
        mockMvc.perform(como(tokenVendedor, get("/api/configuracoes"))).andExpect(status().isOk());
        mockMvc.perform(como(tokenVendedor, put("/api/configuracoes/empresa")).content("""
                        {"nomeFantasia": "Outro nome"}
                        """))
                .andExpect(status().isForbidden());
    }

    // ===== Gerente =====

    @Test
    void gerenteEditaClientesEConsultaFuncionarios() throws Exception {
        mockMvc.perform(como(tokenGerente, put("/api/clientes/" + idCliente)).content("""
                        {"nome": "João da Silva"}
                        """))
                .andExpect(status().isOk());
        mockMvc.perform(como(tokenGerente, get("/api/usuarios"))).andExpect(status().isOk());
    }

    @Test
    void gerenteNaoGerenciaFuncionariosNemConfiguracoes() throws Exception {
        mockMvc.perform(como(tokenGerente, post("/api/usuarios")).content("""
                        {"nome": "Novo", "username": "novo", "senha": "senha1", "perfil": "VENDEDOR"}
                        """))
                .andExpect(status().isForbidden());
        mockMvc.perform(como(tokenGerente, put("/api/configuracoes/empresa")).content("""
                        {"nomeFantasia": "Outro nome"}
                        """))
                .andExpect(status().isForbidden());
    }

    // ===== Dono =====

    @Test
    void donoGerenciaFuncionariosEConfiguracoes() throws Exception {
        mockMvc.perform(como(tokenDono, post("/api/usuarios")).content("""
                        {"nome": "Novo", "username": "novo", "senha": "senha1", "perfil": "VENDEDOR"}
                        """))
                .andExpect(status().isCreated());
        mockMvc.perform(como(tokenDono, put("/api/configuracoes/empresa")).content("""
                        {"nomeFantasia": "Ponto & Sabor Centro"}
                        """))
                .andExpect(status().isOk());
    }

    @Test
    void donoNaoDesativaASiProprio() throws Exception {
        Long idDono = usuarioRepository.findByUsername("matheus").orElseThrow().getId();

        mockMvc.perform(como(tokenDono, delete("/api/usuarios/" + idDono)))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.mensagem").value("Não é possível desativar o usuário da sessão atual."));
    }

    @Test
    void perfilAlteradoValeNaHoraParaOTokenJaEmitido() throws Exception {
        usuarioRepository.findByUsername("carlos").orElseThrow().setPerfil(Perfil.VENDEDOR);
        usuarioRepository.flush();

        mockMvc.perform(como(tokenGerente, get("/api/usuarios"))).andExpect(status().isForbidden());
    }

    @Test
    void documentacaoDaApiEhPublica() throws Exception {
        mockMvc.perform(get("/v3/api-docs")).andExpect(status().isOk());
    }
}
