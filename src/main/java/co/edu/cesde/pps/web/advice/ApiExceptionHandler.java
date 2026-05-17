package co.edu.cesde.pps.web.advice;

import co.edu.cesde.pps.exception.*;
import co.edu.cesde.pps.web.dto.error.ApiErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.List;

// @RestControllerAdvice intercepta excepciones lanzadas desde cualquier
// @RestController y las convierte en ResponseEntity con el formato correcto.
// Sin esto, Spring devolvería HTML de error o un JSON genérico de Whitelabel.
@RestControllerAdvice
public class ApiExceptionHandler {

    // ── 400 Bad Request ─────────────────────────────────────────────────────

    // Errores de validación de negocio (lanzados manualmente en los services)
    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(
            ValidationException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(build(400, "Bad Request", ex.getMessage()));
    }

    // Errores de Bean Validation (@Valid en controllers)
    // Spring lanza MethodArgumentNotValidException cuando un @RequestBody
    // no pasa las anotaciones @NotBlank, @Email, @Positive, etc.
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleBeanValidation(
            MethodArgumentNotValidException ex) {

        // Extraer todos los campos que fallaron y su mensaje
        List<ApiErrorResponse.FieldError> fieldErrors = ex
                .getBindingResult()
                .getFieldErrors()
                .stream()
                .map(fe -> new ApiErrorResponse.FieldError(
                        fe.getField(),
                        fe.getDefaultMessage()))
                .toList();

        ApiErrorResponse body = new ApiErrorResponse(
                400,
                "Validation Failed",
                "One or more fields have invalid values",
                LocalDateTime.now(),
                fieldErrors
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    // ── 401 Unauthorized ────────────────────────────────────────────────────

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiErrorResponse> handleAuth(
            AuthenticationException ex) {
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(build(401, "Unauthorized", ex.getMessage()));
    }

    // ── 404 Not Found ───────────────────────────────────────────────────────

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNotFound(
            EntityNotFoundException ex) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(build(404, "Not Found", ex.getMessage()));
    }

    // ── 409 Conflict ────────────────────────────────────────────────────────

    @ExceptionHandler(DuplicateEntityException.class)
    public ResponseEntity<ApiErrorResponse> handleDuplicate(
            DuplicateEntityException ex) {
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(build(409, "Conflict", ex.getMessage()));
    }

    @ExceptionHandler(InsufficientStockException.class)
    public ResponseEntity<ApiErrorResponse> handleStock(
            InsufficientStockException ex) {
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(build(409, "Conflict", ex.getMessage()));
    }

    @ExceptionHandler(InvalidCartStateException.class)
    public ResponseEntity<ApiErrorResponse> handleCartState(
            InvalidCartStateException ex) {
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(build(409, "Conflict", ex.getMessage()));
    }

    @ExceptionHandler(CartMergeException.class)
    public ResponseEntity<ApiErrorResponse> handleCartMerge(
            CartMergeException ex) {
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(build(409, "Conflict", ex.getMessage()));
    }

    // ── 500 Internal Server Error ────────────────────────────────────────────

    // Captura cualquier excepción no manejada arriba.
    // Es el último recurso: nunca debería llegar aquí en producción.
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGeneral(Exception ex) {
        // Loguear el stack trace real para debugging
        ex.printStackTrace();

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(build(500, "Internal Server Error",
                        "An unexpected error occurred"));
    }

    // Método auxiliar para construir el body de error
    private ApiErrorResponse build(int status, String error, String message) {
        return new ApiErrorResponse(status, error, message, LocalDateTime.now());
    }
}