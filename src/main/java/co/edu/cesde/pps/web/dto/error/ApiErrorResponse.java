package co.edu.cesde.pps.web.dto.error;

import java.time.LocalDateTime;
import java.util.List;

// Shape estándar de error que recibirá el frontend en cualquier fallo
public record ApiErrorResponse(
        int status,           // Código HTTP (400, 404, 409, 500...)
        String error,         // Nombre del error ("Not Found", "Conflict"...)
        String message,       // Mensaje legible para el desarrollador
        LocalDateTime timestamp,
        List<FieldError> fieldErrors  // Solo para errores de validación Bean
) {
    // FieldError representa un campo específico que falló validación
    public record FieldError(String field, String message) {}

    // Constructor sin fieldErrors (para errores que no son de validación)
    public ApiErrorResponse(int status, String error,
                            String message, LocalDateTime timestamp) {
        this(status, error, message, timestamp, null);
    }
}