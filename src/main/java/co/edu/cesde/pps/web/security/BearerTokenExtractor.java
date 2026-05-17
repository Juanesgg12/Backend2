package co.edu.cesde.pps.web.security;

import co.edu.cesde.pps.exception.AuthenticationException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

// @Component registra esta clase como bean de Spring.
// No es un @Service porque no contiene lógica de negocio,
// es un helper de infraestructura HTTP.
@Component
public class BearerTokenExtractor {

    // Prefijo estándar del header de autorización
    private static final String BEARER_PREFIX = "Bearer ";

    // Extrae el token del header Authorization.
    // Lanza AuthenticationException si el header falta o está mal formado.
    public String extract(HttpServletRequest request) {
        String header = request.getHeader("Authorization");

        if (header == null || header.isBlank()) {
            throw new AuthenticationException(
                    "Missing Authorization header");
        }

        if (!header.startsWith(BEARER_PREFIX)) {
            throw new AuthenticationException(
                    "Authorization header must start with 'Bearer '");
        }

        String token = header.substring(BEARER_PREFIX.length()).trim();

        if (token.isEmpty()) {
            throw new AuthenticationException(
                    "Bearer token is empty");
        }

        return token;
    }

    // Versión que devuelve null en vez de lanzar excepción.
    // Útil para endpoints que aceptan tanto guest como autenticado.
    public String extractOrNull(HttpServletRequest request) {
        try {
            return extract(request);
        } catch (AuthenticationException e) {
            return null;
        }
    }
}