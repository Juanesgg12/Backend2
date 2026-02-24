package co.edu.cesde.pps.model;

import co.edu.cesde.pps.enums.CartStatus;
import co.edu.cesde.pps.util.CalculationUtils;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Entidad Cart - Contenedor del carrito de compras.
 *
 * El carrito puede pertenecer a un usuario registrado o a un invitado (guest).
 * Siempre está asociado a una sesión mediante session.
 *
 * Campos:
 * - cartId: Identificador único del carrito (PK)
 * - user: Usuario propietario (N:1 con User) - NULLABLE para carritos de invitado
 * - session: Sesión asociada (N:1 con UserSession) - siempre requerido
 * - status: Estado del carrito (OPEN, CONVERTED, ABANDONED)
 * - createdAt: Fecha de creación del carrito
 * - updatedAt: Fecha de última actualización
 * - items: Lista de items del carrito (1:N con CartItem)
 *
 * Comportamiento por tipo de usuario:
 * - Invitado: user = NULL, session = <UserSession>
 * - Registrado: user = <User>, session = <UserSession>
 *
 * Estados del carrito:
 * - OPEN: Carrito activo, usuario puede agregar/quitar items
 * - CONVERTED: Carrito convertido en orden (checkout completado)
 * - ABANDONED: Carrito abandonado o resultado de merge
 *
 * POLÍTICA DE CART MERGE (OBLIGATORIA):
 * =====================================
 * Cuando un usuario invitado se registra o inicia sesión y ya existe un carrito
 * abierto del usuario, se debe ejecutar el siguiente proceso de fusión (merge):
 *
 * Escenario:
 * - Carrito A: carrito del invitado (user = NULL, status = OPEN)
 * - Carrito B: carrito del usuario registrado (user = User, status = OPEN)
 *
 * Proceso de Merge (implementar en capa de servicio - etapa 05):
 * 1. Identificar ambos carritos por session y user
 * 2. Para cada CartItem del carrito invitado (A):
 *    a. Si el mismo product existe en carrito usuario (B):
 *       - Sumar las cantidades (quantity)
 *       - Resolver conflicto de unitPrice (conservar más reciente o del usuario según política)
 *    b. Si el product NO existe en carrito usuario (B):
 *       - Mover/copiar el CartItem al carrito del usuario (B)
 * 3. Marcar carrito invitado (A) como status = ABANDONED
 * 4. Usuario continúa con carrito único (B) sin pérdida de productos
 *
 * Resultado:
 * - El usuario mantiene un solo carrito activo
 * - No se pierden productos agregados como invitado
 * - No hay duplicación innecesaria de items
 *
 * Ver documentación completa en: documents_external/er_model_documentation.md - Sección 5
 *
 * Relaciones:
 * - N:1 con User (opcional, nullable para invitados)
 * - N:1 con UserSession (obligatorio)
 * - 1:N con CartItem (un carrito tiene muchos items)
 *
 * NOTA: Los métodos de gestión bidireccional (addItem, removeItem) fueron movidos
 * a la capa de servicio (CartService) en etapa 05 para mantener el modelo limpio.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder

public class Cart {

    private Long cartId;
    private User user; // Nullable - NULL para invitados
    private UserSession session;
    @Builder.Default
    private CartStatus status = CartStatus.OPEN; // Por defecto OPEN al crear
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now(); // Se asigna al crear
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now(); // Se actualiza al modificar

    // Colección para relación 1:N
    @Builder.Default
    private List<CartItem> items = new ArrayList<>();



    // Método helper para calcular total del carrito
    public BigDecimal calculateTotal() {
        List<BigDecimal> subtotals = items.stream()
                .map(CartItem::calculateSubtotal)
                .collect(Collectors.toList());
        return CalculationUtils.calculateCartTotal(subtotals);
    }

    // Método helper para verificar si el carrito está abierto
    public boolean isOpen() {
        return status == CartStatus.OPEN;
    }

    // equals y hashCode basados en ID

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Cart cart = (Cart) o;
        return Objects.equals(cartId, cart.cartId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(cartId);
    }

    // toString personalizado sin navegación a objetos relacionados (solo IDs y tamaño de colección)

    @Override
    public String toString() {
        return "Cart{" +
                "cartId=" + cartId +
                ", userId=" + (user != null ? user.getUserId() : null) +
                ", sessionId=" + (session != null ? session.getSessionId() : null) +
                ", status=" + status +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                ", itemsCount=" + (items != null ? items.size() : 0) +
                ", total=" + calculateTotal() +
                '}';
    }
}



