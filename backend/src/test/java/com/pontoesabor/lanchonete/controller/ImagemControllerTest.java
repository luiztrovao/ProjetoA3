package com.pontoesabor.lanchonete.controller;

import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.pontoesabor.lanchonete.model.Categoria;
import com.pontoesabor.lanchonete.model.Produto;
import com.pontoesabor.lanchonete.repository.CategoriaRepository;
import com.pontoesabor.lanchonete.repository.ProdutoRepository;
import java.math.BigDecimal;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/** Upload, leitura e remoção do logo e das fotos de produto. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ImagemControllerTest {

    /** Assinatura de PNG seguida de alguns bytes: o suficiente para o servidor reconhecer o formato. */
    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n', 1, 2, 3, 4};
    private static final byte[] JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 5, 6, 7};

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProdutoRepository produtoRepository;

    @Autowired
    private CategoriaRepository categoriaRepository;

    private static MockMultipartFile arquivo(byte[] conteudo, String nome, String tipo) {
        return new MockMultipartFile("arquivo", nome, tipo, conteudo);
    }

    private String enviarLogo(byte[] conteudo) throws Exception {
        String resposta = mockMvc.perform(multipart(HttpMethod.PUT, "/api/configuracoes/logo")
                        .file(arquivo(conteudo, "logo.png", "image/png")))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return resposta.replaceAll(".*?\"logoUrl\":\"([^\"]+)\".*", "$1");
    }

    // ===== Logo =====

    @Test
    @WithMockUser(username = "1", roles = "DONO")
    void donoEnviaLogoQueApareceNaIdentidadePublica() throws Exception {
        String logoUrl = enviarLogo(PNG);

        mockMvc.perform(get("/api/publico/identidade").with(request -> {
                    request.setUserPrincipal(null);
                    return request;
                }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nomeFantasia").value("Ponto & Sabor"))
                .andExpect(jsonPath("$.logoUrl").value(logoUrl));
        mockMvc.perform(get("/api/configuracoes"))
                .andExpect(jsonPath("$.empresa.logoUrl").value(logoUrl));
    }

    @Test
    @WithMockUser(username = "1", roles = "DONO")
    void imagemEhServidaComTipoECache() throws Exception {
        String logoUrl = enviarLogo(PNG);

        mockMvc.perform(get(logoUrl))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/png"))
                .andExpect(content().bytes(PNG))
                .andExpect(header().string("Cache-Control", startsWith("max-age=")));
    }

    @Test
    @WithMockUser(username = "1", roles = "DONO")
    void trocarLogoApagaOAnterior() throws Exception {
        String primeiro = enviarLogo(PNG);
        String segundo = enviarLogo(JPEG);

        mockMvc.perform(get(segundo)).andExpect(content().contentType("image/jpeg"));
        mockMvc.perform(get(primeiro)).andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "1", roles = "DONO")
    void removerLogoVoltaAoPadrao() throws Exception {
        String logoUrl = enviarLogo(PNG);

        mockMvc.perform(delete("/api/configuracoes/logo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.empresa.logoUrl").doesNotExist());
        mockMvc.perform(get(logoUrl)).andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "1", roles = "DONO")
    void recusaArquivoQueNaoEhPngNemJpg() throws Exception {
        mockMvc.perform(multipart(HttpMethod.PUT, "/api/configuracoes/logo")
                        .file(arquivo("<svg></svg>".getBytes(), "logo.png", "image/png")))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.mensagem").value("Envie uma imagem PNG ou JPG."));
    }

    @Test
    @WithMockUser(username = "1", roles = "DONO")
    void recusaImagemMaiorQue1Mb() throws Exception {
        byte[] grande = Arrays.copyOf(PNG, 1024 * 1024 + 1);

        mockMvc.perform(multipart(HttpMethod.PUT, "/api/configuracoes/logo")
                        .file(arquivo(grande, "grande.png", "image/png")))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.mensagem").value("A imagem deve ter no máximo 1 MB."));
    }

    @Test
    @WithMockUser(username = "1", roles = "DONO")
    void recusaUploadSemArquivo() throws Exception {
        mockMvc.perform(multipart(HttpMethod.PUT, "/api/configuracoes/logo"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "1", roles = "GERENTE")
    void gerenteNaoTrocaOLogo() throws Exception {
        mockMvc.perform(multipart(HttpMethod.PUT, "/api/configuracoes/logo")
                        .file(arquivo(PNG, "logo.png", "image/png")))
                .andExpect(status().isForbidden());
    }

    @Test
    void identidadeEImagemNaoExigemLogin() throws Exception {
        mockMvc.perform(get("/api/publico/identidade")).andExpect(status().isOk());
        mockMvc.perform(get("/api/imagens/999999")).andExpect(status().isNotFound());
    }

    // ===== Foto do produto =====

    private Long produto() {
        Categoria categoria = categoriaRepository.findByAtivoTrueOrderByNome().getFirst();
        return produtoRepository.save(
                new Produto("IMG001", "Produto com foto", categoria, new BigDecimal("10.00"), 5, 1)).getId();
    }

    @Test
    @WithMockUser(username = "1", roles = "GERENTE")
    void gerenteEnviaERemoveAFotoDoProduto() throws Exception {
        Long id = produto();

        String resposta = mockMvc.perform(multipart(HttpMethod.PUT, "/api/produtos/" + id + "/imagem")
                        .file(arquivo(JPEG, "foto.jpg", "image/jpeg")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.imagemUrl").value(startsWith("/api/imagens/")))
                .andReturn().getResponse().getContentAsString();
        String imagemUrl = resposta.replaceAll(".*?\"imagemUrl\":\"([^\"]+)\".*", "$1");

        mockMvc.perform(get("/api/produtos/" + id)).andExpect(jsonPath("$.imagemUrl").value(imagemUrl));

        mockMvc.perform(delete("/api/produtos/" + id + "/imagem"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.imagemUrl").doesNotExist());
        mockMvc.perform(get(imagemUrl)).andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "1", roles = "VENDEDOR")
    void vendedorNaoEnviaFotoDeProduto() throws Exception {
        Long id = produto();

        mockMvc.perform(multipart(HttpMethod.PUT, "/api/produtos/" + id + "/imagem")
                        .file(arquivo(PNG, "foto.png", "image/png")))
                .andExpect(status().isForbidden());
    }
}
