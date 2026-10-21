package com.pontoesabor.lanchonete.exception;

import com.pontoesabor.lanchonete.exception.ErroResponse.CampoInvalido;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.core.PropertyReferenceException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/** Converte exceções em respostas HTTP com o formato {@link ErroResponse}. */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    ResponseEntity<ErroResponse> naoEncontrado(RecursoNaoEncontradoException ex, HttpServletRequest request) {
        return resposta(HttpStatus.NOT_FOUND, ex.getMessage(), request, List.of());
    }

    @ExceptionHandler(RegraNegocioException.class)
    ResponseEntity<ErroResponse> regraNegocio(RegraNegocioException ex, HttpServletRequest request) {
        return resposta(HttpStatus.UNPROCESSABLE_CONTENT, ex.getMessage(), request, List.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ErroResponse> validacao(MethodArgumentNotValidException ex, HttpServletRequest request) {
        List<CampoInvalido> campos = ex.getBindingResult().getFieldErrors().stream()
                .map(erro -> new CampoInvalido(erro.getField(), erro.getDefaultMessage()))
                .toList();
        return resposta(HttpStatus.BAD_REQUEST, "Existem campos inválidos.", request, campos);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ErroResponse> corpoInvalido(HttpServletRequest request) {
        return resposta(HttpStatus.BAD_REQUEST, "O corpo da requisição está ausente ou mal formatado.", request, List.of());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ResponseEntity<ErroResponse> parametroInvalido(MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        return resposta(HttpStatus.BAD_REQUEST, "Valor inválido para o parâmetro '" + ex.getName() + "'.", request, List.of());
    }

    /** Ordenação por um campo que não existe (ex.: ?sort=campoInexistente). */
    @ExceptionHandler(PropertyReferenceException.class)
    ResponseEntity<ErroResponse> ordenacaoInvalida(PropertyReferenceException ex, HttpServletRequest request) {
        return resposta(HttpStatus.BAD_REQUEST, "Não é possível ordenar por '" + ex.getPropertyName() + "'.", request, List.of());
    }

    /** Última barreira para violações de restrições do banco (ex.: valor único duplicado). */
    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ErroResponse> integridade(HttpServletRequest request) {
        return resposta(HttpStatus.CONFLICT, "A operação viola uma restrição do banco de dados.", request, List.of());
    }

    private static ResponseEntity<ErroResponse> resposta(
            HttpStatus status, String mensagem, HttpServletRequest request, List<CampoInvalido> campos) {
        ErroResponse corpo = new ErroResponse(
                Instant.now(), status.value(), status.getReasonPhrase(), mensagem, request.getRequestURI(), campos);
        return ResponseEntity.status(status).body(corpo);
    }
}
