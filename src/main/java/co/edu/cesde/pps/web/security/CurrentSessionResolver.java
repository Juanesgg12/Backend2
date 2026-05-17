package co.edu.cesde.pps.web.security;

import co.edu.cesde.pps.exception.AuthenticationException;
import co.edu.cesde.pps.model.User;
import co.edu.cesde.pps.model.UserSession;
import co.edu.cesde.pps.repository.UserSesionRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class CurrentSessionResolver {

    private final BearerTokenExtractor tokenExtractor;
    private final UserSesionRepository userSesionRepository;

    public CurrentSessionResolver(BearerTokenExtractor tokenExtractor,
                                  UserSesionRepository userSesionRepository) {
        this.tokenExtractor = tokenExtractor;
        this.userSesionRepository = userSesionRepository;
    }

    // Resuelve la sesión activa a partir del token Bearer.
    // Lanza excepción si el token no existe o la sesión expiró.
    public UserSession resolveSession(HttpServletRequest request) {
        String token = tokenExtractor.extract(request);
        return findValidSession(token);
    }

    // Igual que resolveSession pero devuelve null si no hay token.
    // Para endpoints que funcionan tanto con sesión como sin ella.
    public UserSession resolveSessionOrNull(HttpServletRequest request) {
        String token = tokenExtractor.extractOrNull(request);
        if (token == null) return null;
        return findValidSession(token);
    }

    // Resuelve el usuario autenticado (no guest).
    // Lanza excepción si la sesión es de invitado.
    public User resolveAuthenticatedUser(HttpServletRequest request) {
        UserSession session = resolveSession(request);

        if (session.isGuestSession()) {
            throw new AuthenticationException(
                    "This endpoint requires an authenticated user, not a guest session");
        }

        return session.getUser();
    }

    // Lógica común: buscar la sesión y verificar que no esté expirada
    private UserSession findValidSession(String token) {
        UserSession session = userSesionRepository
                .findBySessionToken(token)
                .orElseThrow(() -> new AuthenticationException(
                        "Invalid or expired session token"));

        if (session.isExpired()) {
            throw new AuthenticationException(
                    "Session has expired, please login again");
        }

        return session;
    }
}