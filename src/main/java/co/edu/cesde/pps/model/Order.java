package co.edu.cesde.pps.model;

import co.edu.cesde.pps.util.CalculationUtils;
import co.edu.cesde.pps.util.ValidationUtils;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import lombok.*;
import jakarta.persistence.*;


/**
 * Entidad Order - Representa una compra finalizada (pedido/orden).
 *
 * Una orden se crea cuando el usuario completa el checkout.
 * El checkout REQUIERE que el usuario esté registrado (userId NOT NULL).
 *
 * Campos:
 * - orderId: Identificador único de la orden (PK)
 * - orderNumber: Número de orden único (UNIQUE) - para tracking y referencia
 * - userId: Usuario que realizó la compra (FK a User) - NOT NULL
 * - orderStatusId: Estado actual de la orden (FK a OrderStatus)
 * - shippingAddressId: Dirección de envío (FK a Address)
 * - billingAddressId: Dirección de facturación (FK a Address)
 * - subtotal: Suma de precios de items antes de impuestos/envío (BigDecimal)
 * - tax: Impuestos aplicados (BigDecimal)
 * - shippingCost: Costo de envío (BigDecimal)
 * - total: Total final de la orden (subtotal + tax + shippingCost)
 * - createdAt: Fecha de creación de la orden
 *
 * Consideraciones de diseño:
 * - userId es obligatorio: los invitados deben registrarse antes del checkout
 * - Se guardan totales (subtotal, tax, shippingCost, total) para auditoría
 * - orderNumber único facilita búsqueda y tracking por parte del usuario
 * - Direcciones de envío y facturación pueden ser diferentes
 * - BigDecimal en todos los campos monetarios para precisión
 *
 * Relaciones (futuro - etapa02):
 * - N:1 con User (una orden pertenece a un usuario)
 * - N:1 con OrderStatus (estado actual)
 * - N:1 con Address (shipping_address_id)
 * - N:1 con Address (billing_address_id)
 * - 1:N con OrderItem (items de la orden)
 * - 1:N con Payment (pagos asociados, puede haber reintentos)
 */
@Entity
@Table (name = "orders")

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder

public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_id")
    private Long orderId;
    @Column(name = "order_number", unique = true, nullable = false, length = 50)
    private String orderNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_status_id", nullable = false)
    private OrderStatus orderStatus;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "shipping_address_id", nullable = false)
    private Address shippingAddress;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "billing_address_id", nullable = false)
    private Address billingAddress;
    @Column(nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal subtotal = BigDecimal.ZERO;
    @Column(nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal tax = BigDecimal.ZERO;
    @Column(nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal shippingCost = BigDecimal.ZERO;
    @Column(nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal total = BigDecimal.ZERO;
    @Column(nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    // Colección para relación 1:N con OrderItem
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    @Builder.Default
    private List<OrderItem> items = new ArrayList<>();


    // Setters personalizados con validación (override de Lombok)

    public void setSubtotal(BigDecimal subtotal) {
        ValidationUtils.validateNonNegative(subtotal, "subtotal");
        this.subtotal = subtotal;
    }

    public void setTax(BigDecimal tax) {
        ValidationUtils.validateNonNegative(tax, "tax");
        this.tax = tax;
    }

    public void setShippingCost(BigDecimal shippingCost) {
        ValidationUtils.validateNonNegative(shippingCost, "shippingCost");
        this.shippingCost = shippingCost;
    }

    public void setTotal(BigDecimal total) {
        ValidationUtils.validateNonNegative(total, "total");
        this.total = total;
    }

    // Método helper para calcular total automáticamente
    public BigDecimal calculateTotal() {
        return CalculationUtils.calculateOrderTotal(subtotal, tax, shippingCost);
    }


    // equals y hashCode basados en ID

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Order order = (Order) o;
        return Objects.equals(orderId, order.orderId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(orderId);
    }

    // toString personalizado sin navegación a objetos relacionados (solo IDs y tamaño de colección)

    @Override
    public String toString() {
        return "Order{" +
                "orderId=" + orderId +
                ", orderNumber='" + orderNumber + '\'' +
                ", userId=" + (user != null ? user.getUserId() : null) +
                ", orderStatusId=" + (orderStatus != null ? orderStatus.getOrderStatusId() : null) +
                ", shippingAddressId=" + (shippingAddress != null ? shippingAddress.getAddressId() : null) +
                ", billingAddressId=" + (billingAddress != null ? billingAddress.getAddressId() : null) +
                ", subtotal=" + subtotal +
                ", tax=" + tax +
                ", shippingCost=" + shippingCost +
                ", total=" + total +
                ", createdAt=" + createdAt +
                '}';
    }
}
