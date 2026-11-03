package com.pontoesabor.lanchonete.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Proteção contra segredo fraco ou validade inválida do token JWT (análise de valor-limite). */
class JwtPropertiesTest {

    private static final Duration OITO_HORAS = Duration.ofHours(8);

    @Test
    void segredoDe32CaracteresEhAceito() {
        JwtProperties propriedades = new JwtProperties("a".repeat(32), OITO_HORAS);

        assertThat(propriedades.segredo()).hasSize(32);
    }

    @Test
    void segredoDe31CaracteresEhRecusado() {
        assertThatThrownBy(() -> new JwtProperties("a".repeat(31), OITO_HORAS))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT_SECRET");
    }

    @Test
    void segredoAusenteEhRecusado() {
        assertThatThrownBy(() -> new JwtProperties(null, OITO_HORAS))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void segredoContaBytesENaoCaracteres() {
        // 16 letras acentuadas ocupam 32 bytes em UTF-8
        assertThat(new JwtProperties("é".repeat(16), OITO_HORAS).segredo()).hasSize(16);
    }

    @ParameterizedTest
    @ValueSource(longs = {-1, 0})
    void validadeZeroOuNegativaEhRecusada(long segundos) {
        assertThatThrownBy(() -> new JwtProperties("a".repeat(32), Duration.ofSeconds(segundos)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("validade");
    }

    @Test
    void validadeAusenteEhRecusada() {
        assertThatThrownBy(() -> new JwtProperties("a".repeat(32), null))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void validadeDeUmSegundoEhAceita() {
        assertThat(new JwtProperties("a".repeat(32), Duration.ofSeconds(1)).validade()).hasSeconds(1);
    }
}
