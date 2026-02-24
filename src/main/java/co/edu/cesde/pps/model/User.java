package co.edu.cesde.pps.model;

import co.edu.cesde.pps.enums.UserStatus;

import lombok.*;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Entidad User - Representa un usuario registrado del sistema.
 *
 * El usuario debe registrarse para completar el checkout y crear órdenes.
 *
 * Campos:
 * - userId: Identificador único del usuario (PK)
 * - role: Rol del usuario (N:1 con Role) - determina permisos
 * - email: Email único del usuario (UNIQUE) - usado para login
 * - passwordHash: Hash de la contraseña (nunca texto plano)
 * - firstName: Nombre del usuario
 * - lastName: Apellido del usuario
 * - phone: Teléfono de contacto
 * - status: Estado del usuario (ACTIVE, INACTIVE, BLOCKED)
 * - createdAt: Fecha de creación de la cuenta
 * - addresses: Lista de direcciones del usuario (1:N con Address)
 * - sessions: Lista de sesiones del usuario (1:N con UserSession)
 *
 * Relaciones:
 * - N:1 con Role (muchos usuarios tienen un rol)
 * - 1:N con Address (un usuario tiene muchas direcciones)
 * - 1:N con UserSession (un usuario tiene muchas sesiones)
 * - 1:N con Cart (un usuario puede tener carritos históricos)
 * - 1:N con Order (un usuario tiene muchas órdenes)
 *
 * NOTA: Los métodos de gestión bidireccional (addAddress, removeAddress) fueron movidos
 * a la capa de servicio (UserService) en etapa 05 para mantener el modelo limpio.
 */
@Entity
@Table (name = "users")

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder


public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;
    @Column(unique = true, nullable = false)
    private String email;
    @Column(name = "password_hash", nullable = false)
    private String passwordHash;
    @Column(name = "first_name", nullable = false)
    private String firstName;
    @Column(name = "last_name", nullable = false)
    private String lastName;
    private String phone;
    @Enumerated(EnumType.STRING)
    private UserStatus status = UserStatus.ACTIVE;// Por defecto activo al crear
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    // Colecciones para relaciones 1:N
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)

    @Builder.Default
    private List<Address> addresses = new ArrayList<>();









    // Métodos helper de consulta (sin efectos secundarios)

    /**
     * Obtiene la dirección por defecto del usuario
     */
    public Address getDefaultAddress() {
        return addresses.stream()
                .filter(Address::getIsDefault)
                .findFirst()
                .orElse(null);
    }

    /**
     * Obtiene el nombre completo del usuario
     */
    public String getFullName() {
        return firstName + " " + lastName;
    }

    // equals y hashCode basados en ID

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return Objects.equals(userId, user.userId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId);
    }

    // toString sin navegación a objetos relacionados (solo IDs y tamaño de colecciones)

    @Override
    public String toString() {
        return "User{" +
                "userId=" + userId +
                ", role=" + (role != null ? role.getName() : "null") +
                ", email='" + email + '\'' +
                ", firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", phone='" + phone + '\'' +
                ", status=" + status +
                ", createdAt=" + createdAt +
                ", addressesCount=" + (addresses != null ? addresses.size() : 0) +
                '}';
    }
}
