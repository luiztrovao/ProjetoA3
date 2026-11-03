package com.pontoesabor.lanchonete.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.pontoesabor.lanchonete.dto.ConfiguracaoDtos.PreferenciasVendaDto;
import com.pontoesabor.lanchonete.dto.VendaDtos.ItemRequest;
import com.pontoesabor.lanchonete.dto.VendaDtos.VendaRequest;
import com.pontoesabor.lanchonete.model.FormaPagamento;
import com.pontoesabor.lanchonete.model.Perfil;
import com.pontoesabor.lanchonete.model.StatusVenda;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Análise de valor-limite das regras de validação da API (caixa-preta): para cada faixa, os valores
 * logo abaixo, em cima e logo acima de cada fronteira.
 */
class LimitesValidacaoTest {

    private static ValidatorFactory fabrica;
    private static Validator validador;

    @BeforeAll
    static void criarValidador() {
        fabrica = Validation.buildDefaultValidatorFactory();
        validador = fabrica.getValidator();
    }

    @AfterAll
    static void fecharValidador() {
        fabrica.close();
    }

    /** Campos com erro de validação no objeto. */
    private static Set<String> camposInvalidos(Object objeto) {
        return validador.validate(objeto).stream()
                .map(ConstraintViolation::getPropertyPath)
                .map(Object::toString)
                .collect(Collectors.toSet());
    }

    private static UsuarioCriacaoRequest usuario(String username, String telefone, String senha) {
        return new UsuarioCriacaoRequest("Fernanda Souza", username, null, null, telefone, senha, Perfil.VENDEDOR);
    }

    @ParameterizedTest(name = "senha com {0} caracteres: válida = {1}")
    @CsvSource({"5, false", "6, true", "7, true", "71, true", "72, true", "73, false"})
    void senhaDe6A72Caracteres(int tamanho, boolean valida) {
        assertThat(camposInvalidos(usuario("fernanda", null, "s".repeat(tamanho))).contains("senha"))
                .isEqualTo(!valida);
    }

    @ParameterizedTest(name = "usuário com {0} caracteres: válido = {1}")
    @CsvSource({"2, false", "3, true", "4, true", "49, true", "50, true", "51, false"})
    void nomeDeUsuarioDe3A50Caracteres(int tamanho, boolean valido) {
        assertThat(camposInvalidos(usuario("u".repeat(tamanho), null, "senha1")).contains("username"))
                .isEqualTo(!valido);
    }

    @ParameterizedTest(name = "telefone com {0} dígitos: válido = {1}")
    @CsvSource({"9, false", "10, true", "11, true", "12, false"})
    void telefoneCom10Ou11Digitos(int digitos, boolean valido) {
        assertThat(camposInvalidos(usuario("fernanda", "8".repeat(digitos), "senha1")).contains("telefone"))
                .isEqualTo(!valido);
    }

    @ParameterizedTest(name = "quantidade {0}: válida = {1}")
    @CsvSource({"0, false", "1, true", "2, true", "998, true", "999, true", "1000, false"})
    void quantidadeDoItemDe1A999(int quantidade, boolean valida) {
        assertThat(camposInvalidos(new ItemRequest(1L, quantidade)).contains("quantidade")).isEqualTo(!valida);
    }

    @ParameterizedTest(name = "preço {0}: válido = {1}")
    @CsvSource({"0.00, false", "0.01, true", "0.02, true", "99999999.99, true", "0.001, false"})
    void precoMaiorQueZeroComDuasCasas(String preco, boolean valido) {
        ProdutoRequest produto = new ProdutoRequest("X-Burger", "XBG001", null, 1L, new BigDecimal(preco), 0, 0);

        assertThat(camposInvalidos(produto).contains("preco")).isEqualTo(!valido);
    }

    @ParameterizedTest(name = "estoque {0}: válido = {1}")
    @CsvSource({"-1, false", "0, true", "1, true"})
    void estoqueNaoNegativo(int estoque, boolean valido) {
        ProdutoRequest produto = new ProdutoRequest("X-Burger", "XBG001", null, 1L, new BigDecimal("10.00"), estoque, estoque);

        Set<String> campos = camposInvalidos(produto);
        assertThat(campos.contains("estoqueAtual")).isEqualTo(!valido);
        assertThat(campos.contains("estoqueMinimo")).isEqualTo(!valido);
    }

    @ParameterizedTest(name = "desconto {0}: válido = {1}")
    @CsvSource({"-0.01, false", "0.00, true", "0.01, true"})
    void descontoNaoNegativo(String desconto, boolean valido) {
        VendaRequest venda = new VendaRequest(
                null, List.of(new ItemRequest(1L, 1)), new BigDecimal(desconto), FormaPagamento.PIX, null, null);

        assertThat(camposInvalidos(venda).contains("desconto")).isEqualTo(!valido);
    }

    @ParameterizedTest(name = "alerta de atraso de {0} min: válido = {1}")
    @CsvSource({"4, false", "5, true", "6, true", "239, true", "240, true", "241, false"})
    void alertaDePedidoAtrasadoDe5A240Minutos(int minutos, boolean valido) {
        PreferenciasVendaDto preferencias = new PreferenciasVendaDto(
                true, true, 5, true, true, StatusVenda.EM_PREPARO, Set.of(FormaPagamento.PIX), minutos);

        assertThat(camposInvalidos(preferencias).contains("minutosAlertaPreparo")).isEqualTo(!valido);
    }

    @ParameterizedTest(name = "alerta de estoque baixo {0}: válido = {1}")
    @CsvSource({"-1, false", "0, true", "1, true"})
    void alertaDeEstoqueBaixoNaoNegativo(int alerta, boolean valido) {
        PreferenciasVendaDto preferencias = new PreferenciasVendaDto(
                true, true, alerta, true, true, StatusVenda.EM_PREPARO, Set.of(FormaPagamento.PIX), 20);

        assertThat(camposInvalidos(preferencias).contains("alertaEstoqueBaixo")).isEqualTo(!valido);
    }
}
