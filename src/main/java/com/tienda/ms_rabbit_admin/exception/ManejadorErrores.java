package com.tienda.ms_rabbit_admin.exception;

import com.tienda.ms_rabbit_admin.dto.ErrorApi;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.RestClientException;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

import java.util.LinkedHashMap;
import java.util.Map;

/** Traduce las excepciones a respuestas JSON claras y con el codigo HTTP correcto. */
@RestControllerAdvice
public class ManejadorErrores {

    private static final Logger log = LoggerFactory.getLogger(ManejadorErrores.class);

    /** Errores de @Valid en el cuerpo (DTO). */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorApi> cuerpoInvalido(MethodArgumentNotValidException ex) {
        Map<String, String> campos = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(e -> campos.putIfAbsent(e.getField(), e.getDefaultMessage()));
        return responder(HttpStatus.BAD_REQUEST, "Datos de entrada invalidos", campos);
    }

    /** Errores de validacion en @PathVariable / @RequestParam. */
    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ErrorApi> parametroInvalido(HandlerMethodValidationException ex) {
        String detalle = ex.getAllErrors().stream()
                .map(e -> e.getDefaultMessage())
                .distinct()
                .collect(java.util.stream.Collectors.joining("; "));
        Map<String, String> campos = Map.of("parametros", detalle);
        return responder(HttpStatus.BAD_REQUEST, "Parametros invalidos", campos);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorApi> restriccion(ConstraintViolationException ex) {
        Map<String, String> campos = new LinkedHashMap<>();
        ex.getConstraintViolations().forEach(v -> campos.putIfAbsent(v.getPropertyPath().toString(), v.getMessage()));
        return responder(HttpStatus.BAD_REQUEST, "Parametros invalidos", campos);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorApi> faltaParametro(MissingServletRequestParameterException ex) {
        return responder(HttpStatus.BAD_REQUEST, "Falta el parametro '" + ex.getParameterName() + "'", Map.of());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorApi> jsonInvalido(HttpMessageNotReadableException ex) {
        return responder(HttpStatus.BAD_REQUEST, "El cuerpo no es un JSON valido o tiene tipos incorrectos", Map.of());
    }

    @ExceptionHandler(SolicitudInvalidaException.class)
    public ResponseEntity<ErrorApi> solicitudInvalida(SolicitudInvalidaException ex) {
        return responder(HttpStatus.BAD_REQUEST, ex.getMessage(), Map.of());
    }

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<ErrorApi> noEncontrado(RecursoNoEncontradoException ex) {
        return responder(HttpStatus.NOT_FOUND, ex.getMessage(), Map.of());
    }

    @ExceptionHandler(ConflictoException.class)
    public ResponseEntity<ErrorApi> conflicto(ConflictoException ex) {
        return responder(HttpStatus.CONFLICT, ex.getMessage(), Map.of());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorApi> sinPermiso(AccessDeniedException ex) {
        return responder(HttpStatus.FORBIDDEN, "Requiere scope Menu-compra y rol ADMIN", Map.of());
    }

    /** RabbitMQ rechazo la operacion (p.ej. PRECONDITION_FAILED) o no esta disponible. */
    @ExceptionHandler(AmqpException.class)
    public ResponseEntity<ErrorApi> rabbit(AmqpException ex) {
        log.error("Error de RabbitMQ: {}", ex.getMessage());
        return responder(HttpStatus.BAD_GATEWAY, "RabbitMQ rechazo la operacion o no esta disponible: " + ex.getMessage(), Map.of());
    }

    /** La API HTTP de administracion (15672) no responde o rechazo las credenciales. */
    @ExceptionHandler(RestClientException.class)
    public ResponseEntity<ErrorApi> management(RestClientException ex) {
        log.error("Error consultando la API de administracion de RabbitMQ: {}", ex.getMessage());
        return responder(HttpStatus.BAD_GATEWAY, "No se pudo consultar la API de administracion de RabbitMQ (puerto 15672)", Map.of());
    }

    private static ResponseEntity<ErrorApi> responder(HttpStatus status, String mensaje, Map<String, String> campos) {
        return ResponseEntity.status(status).body(ErrorApi.de(status.value(), status.getReasonPhrase(), mensaje, campos));
    }
}
