package co.edu.cesde.pps.model;

import lombok.*;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Entidad UserSession - Representa sesiones activas para navegación.
 *
 * Crucial para manejar carritos de invitado y sesiones de usuarios registrados.
 *
 * Campos:
 * - sessionId: Identificador único de la sesión (PK)
 * - user: Usuario asociado (N:1 con User) - NULLABLE para invitados
 * - sessionToken: Token único de sesión (UNIQUE) - mapea con cookie/JWT
 * - createdAt: Fecha de creación de la sesión
 * - expiresAt: Fecha de expiración de la sesión
 *
 * Comportamiento:
 * - user = NULL → sesión de invitado (guest)
 * - user = <User> → sesión de usuario registrado
 *
 * Relaciones:
 * - N:1 con User (opcional, nullable - muchas sesiones pueden pertenecer a un usuario)
 * - 1:N con Cart (una sesión puede tener múltiples carritos en el tiempo)
 */
@Entity
@Table(name = "user_session")

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserSession {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "session_id")
    private Long sessionId;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = true)
    private User user;
    @Column(unique = true, nullable = false, length = 100)
    private String sessionToken;
    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now(); // Se asigna al crear
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;



    // Método helper para verificar si es sesión de invitado
    public boolean isGuestSession() {
        return user == null;
    }

    // Método helper para verificar si la sesión ha expirado
    public boolean isExpired() {
        return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
    }

    // equals y hashCode basados en ID

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserSession that = (UserSession) o;
        return Objects.equals(sessionId, that.sessionId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sessionId);
    }

    // toString sin navegación a objetos relacionados (solo IDs)

    @Override
    public String toString() {
        return "UserSession{" +
                "sessionId=" + sessionId +
                ", userId=" + (user != null ? user.getUserId() : null) +
                ", sessionToken='" + sessionToken + '\'' +
                ", createdAt=" + createdAt +
                ", expiresAt=" + expiresAt +
                ", isGuest=" + isGuestSession() +
                ", isExpired=" + isExpired() +
                '}';
    }
}
