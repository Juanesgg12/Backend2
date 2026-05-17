package co.edu.cesde.pps.web.dto.response;

// Lo que devuelve el backend después de login, register o guest-session.
// El frontend guarda el sessionToken en localStorage y lo manda
// en cada petición como: Authorization: Bearer <sessionToken>
public record AuthSessionResponse(
        String sessionToken,
        boolean isGuest,
        UserResponse user   // null si es sesión guest
) {}