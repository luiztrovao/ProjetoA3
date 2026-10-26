package com.pontoesabor.lanchonete.config;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuração dos tokens JWT ({@code app.jwt.*}).
 *
 * @param segredo  chave usada para assinar os tokens (HMAC-SHA256); mínimo de 32 bytes
 * @param validade tempo até o token expirar (ex.: 8h)
 */
@ConfigurationProperties("app.jwt")
public record JwtProperties(String segredo, Duration validade) {

    public JwtProperties {
        if (segredo == null || segredo.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException(
                    "Configure a variável de ambiente JWT_SECRET com pelo menos 32 caracteres.");
        }
        if (validade == null || validade.isNegative() || validade.isZero()) {
            throw new IllegalStateException("A validade do token (app.jwt.validade) deve ser positiva.");
        }
    }
}
