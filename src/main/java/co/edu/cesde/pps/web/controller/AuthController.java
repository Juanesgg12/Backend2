package co.edu.cesde.pps.web.controller;

import co.edu.cesde.pps.model.User;
import co.edu.cesde.pps.model.UserSession;
import co.edu.cesde.pps.service.AuthService;
import co.edu.cesde.pps.web.dto.request.LoginRequest;
import co.edu.cesde.pps.web.dto.request.RegisterRequest;
import co.edu.cesde.pps.web.dto.response.AuthSessionResponse;
import co.edu.cesde.pps.web.dto.response.UserResponse;
import co.edu.cesde.pps.web.security.BearerTokenExtractor;
import co.edu.cesde.pps.web.security.CurrentSessionResolver;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

// @RestController = @Controller + @ResponseBody
// Indica que esta clase maneja peticiones HTTP y devuelve JSON directamente
@RestController
// @RequestMapping define la ruta base de todos los endpoints de este controller
@RequestMapping(ApiRoutes.AUTH)
public class AuthController {

    private final AuthService authService;
    private final CurrentSessionResolver sessionResolver;
    private final BearerTokenExtractor tokenExtractor;

    public AuthController(AuthService authService,
                          CurrentSessionResolver sessionResolver,
                          BearerTokenExtractor tokenExtractor) {
        this.authService = authService;
        this.sessionResolver = sessionResolver;
        this.tokenExtractor = tokenExtractor;
    }

    // POST /api/v1/auth/guest-session
    // No requiere autenticación — cualquiera puede crear una sesión guest
    @PostMapping("/guest-session")
    public ResponseEntity<AuthSessionResponse> createGuestSession() {
        UserSession session = authService.createGuestSession();
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(toAuthResponse(session));
    }

    // POST /api/v1/auth/register
    // @RequestBody lee el JSON del body de la petición
    // @Valid activa Bean Validation sobre el DTO
    @PostMapping("/register")
    public ResponseEntity<AuthSessionResponse> register(
            @RequestBody @Valid RegisterRequest request) {

        UserSession session = authService.register(
                request.email(),
                request.password(),
                request.firstName(),
                request.lastName(),
                request.phone()
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(toAuthResponse(session));
    }

    // POST /api/v1/auth/login
    @PostMapping("/login")
    public ResponseEntity<AuthSessionResponse> login(
            @RequestBody @Valid LoginRequest request) {

        UserSession session = authService.login(
                request.email(),
                request.password()
        );

        return ResponseEntity.ok(toAuthResponse(session));
    }

    // GET /api/v1/auth/me
    // Requiere token válido — devuelve info del usuario autenticado
    @GetMapping("/me")
    public ResponseEntity<UserResponse> me(HttpServletRequest request) {
        User user = sessionResolver.resolveAuthenticatedUser(request);
        return ResponseEntity.ok(toUserResponse(user));
    }

    // POST /api/v1/auth/logout
    // Elimina la sesión de la BD
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        String token = tokenExtractor.extract(request);
        authService.logout(token);
        // 204 No Content = operación exitosa sin body de respuesta
        return ResponseEntity.noContent().build();
    }

    // ── Mappers privados ────────────────────────────────────────────────────

    // Convierte UserSession → AuthSessionResponse
    private AuthSessionResponse toAuthResponse(UserSession session) {
        UserResponse userResponse = session.getUser() != null
                ? toUserResponse(session.getUser())
                : null;

        return new AuthSessionResponse(
                session.getSessionToken(),
                session.isGuestSession(),
                userResponse
        );
    }

    // Convierte User → UserResponse
    private UserResponse toUserResponse(User user) {
        return new UserResponse(
                user.getUserId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getFullName(),
                user.getPhone(),
                user.getRole() != null ? user.getRole().getName() : null,
                user.getStatus(),
                user.getCreatedAt()
        );
    }
}