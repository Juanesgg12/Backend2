package co.edu.cesde.pps.service;

import co.edu.cesde.pps.exception.AuthenticationException;
import co.edu.cesde.pps.exception.DuplicateEntityException;
import co.edu.cesde.pps.model.User;
import co.edu.cesde.pps.model.UserSession;
import co.edu.cesde.pps.repository.UserRepository;
import co.edu.cesde.pps.repository.UserSesionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class AuthService {

    private final UserRepository userRepository;
    private final UserSesionRepository userSesionRepository;
    private final UserService userService;

    public AuthService(UserRepository userRepository,
                       UserSesionRepository userSesionRepository,
                       UserService userService) {
        this.userRepository = userRepository;
        this.userSesionRepository = userSesionRepository;
        this.userService = userService;
    }

    // Crea una sesión para un invitado (sin usuario).
    // El frontend llama esto primero para obtener un token
    // y poder usar el carrito antes de registrarse.
    @Transactional
    public UserSession createGuestSession() {
        UserSession session = UserSession.builder()
                .user(null)                          // null = invitado
                .sessionToken(generateToken())
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusDays(7)) // sesión guest dura 7 días
                .build();

        return userSesionRepository.save(session);
    }

    // Registra un usuario nuevo y crea su sesión autenticada.
    // passwordHash: en un proyecto real usarías BCrypt aquí.
    // Para esta etapa se recibe como viene (el documento no exige encriptación).
    @Transactional
    public UserSession register(String email, String password,
                                String firstName, String lastName, String phone) {

        // Verificar que el email no esté ya registrado
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new DuplicateEntityException("User", "email", email);
        }

        // Registrar el usuario usando el UserService existente
        // que ya valida email, contraseña, nombres, etc.
        userService.registerUser(email, password, firstName, lastName, phone);

        // Obtener el usuario recién creado para asociar la sesión
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AuthenticationException(
                        "Error creating user session after registration"));

        return createAuthenticatedSession(user);
    }

    // Login: verifica credenciales y crea sesión autenticada.
    // En producción compararías el hash, aquí comparamos directo
    // porque el proyecto no implementa BCrypt todavía.
    @Transactional
    public UserSession login(String email, String password) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AuthenticationException(
                        "Invalid email or password"));

        // Comparación directa del password
        // TODO etapa futura: usar BCrypt → passwordEncoder.matches(password, user.getPasswordHash())
        if (!user.getPasswordHash().equals(password)) {
            throw new AuthenticationException("Invalid email or password");
        }

        return createAuthenticatedSession(user);
    }

    // Invalida la sesión borrándola de la BD.
    // El frontend debe eliminar el token de localStorage al recibir 204.
    @Transactional
    public void logout(String sessionToken) {
        UserSession session = userSesionRepository
                .findBySessionToken(sessionToken)
                .orElseThrow(() -> new AuthenticationException(
                        "Invalid session token"));

        userSesionRepository.delete(session);
    }

    // Crea una sesión autenticada para un usuario ya validado
    private UserSession createAuthenticatedSession(User user) {
        UserSession session = UserSession.builder()
                .user(user)
                .sessionToken(generateToken())
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusDays(30)) // sesión autenticada dura 30 días
                .build();

        return userSesionRepository.save(session);
    }

    // Genera un token único usando UUID.
    // UUID.randomUUID() genera algo como: "550e8400-e29b-41d4-a716-446655440000"
    // Es suficientemente único y seguro para esta etapa sin JWT.
    private String generateToken() {
        return UUID.randomUUID().toString();
    }
}