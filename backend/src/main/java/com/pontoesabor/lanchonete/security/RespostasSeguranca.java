package com.pontoesabor.lanchonete.security;

import com.pontoesabor.lanchonete.exception.ErroResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/** Respostas 401 e 403 no mesmo formato de erro do restante da API. */
@Component
public class RespostasSeguranca {

    public static final String MENSAGEM_NAO_AUTENTICADO = "Faça login para acessar esta funcionalidade.";
    public static final String MENSAGEM_SEM_PERMISSAO = "Você não possui permissão para acessar esta funcionalidade.";

    private final JsonMapper jsonMapper;

    public RespostasSeguranca(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    public AuthenticationEntryPoint naoAutenticado() {
        return (request, response, ex) -> escrever(request, response, HttpStatus.UNAUTHORIZED, MENSAGEM_NAO_AUTENTICADO);
    }

    public AccessDeniedHandler semPermissao() {
        return (request, response, ex) -> escrever(request, response, HttpStatus.FORBIDDEN, MENSAGEM_SEM_PERMISSAO);
    }

    private void escrever(HttpServletRequest request, HttpServletResponse response, HttpStatus status, String mensagem)
            throws IOException {
        ErroResponse corpo = new ErroResponse(
                Instant.now(), status.value(), status.getReasonPhrase(), mensagem, request.getRequestURI(), List.of());
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        jsonMapper.writeValue(response.getOutputStream(), corpo);
    }
}
