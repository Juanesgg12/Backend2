package co.edu.cesde.pps.model;

import co.edu.cesde.pps.util.CalculationUtils;
import co.edu.cesde.pps.util.ValidationUtils;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.*;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.Objects;

/**
 * Entidad OrderItem - Detalle de productos comprados en una orden.
 *
 * Representa un producto incluido en una orden con su cantidad y precio histórico.
 *
 * Campos:
 * - orderItemId: Identificador único del item (PK)
 * - order: Orden a la que pertenece (N:1 con Order)
 * - product: Producto comprado (N:1 con Product)
 * - quantity: Cantidad comprada
 * - unitPrice: Precio unitario al momento de la compra (histórico)
 * - lineTotal: Total de la línea (unitPrice * quantity)
 *
 * Restricción UNIQUE (order, product):
 * Un producto no puede aparecer duplicado en la misma orden. Si el usuario
 * compra el mismo producto dos veces en checkout, debe consolidarse en un
 * solo OrderItem con cantidad sumada.
 *
 * Congelación de precio (unitPrice):
 * Se guarda el precio del producto en el momento de crear la orden.
 * Esto es crucial para auditoría y reportes históricos, ya que los precios
 * de productos pueden cambiar con el tiempo.
 *
 * lineTotal:
 * Se puede calcular (unitPrice * quantity) o guardar para optimización.
 * Guardarlo facilita consultas y reportes sin recalcular.
 *
 * Relaciones:
 * - N:1 con Order (muchos items pertenecen a una orden)
 * - N:1 con Product (muchos items referencian a un producto)
 */
@Entity
@Table(name = "order_items")

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_item_id")
    private Long orderItemId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    @JsonBackReference
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPrice;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal lineTotal;


    // Setters personalizados con validación (override de Lombok)

    public void setQuantity(Integer quantity) {
        ValidationUtils.validatePositive(quantity, "quantity");
        this.quantity = quantity;
        // Recalcular lineTotal al cambiar quantity
        this.lineTotal = calculateLineTotal();
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        ValidationUtils.validateNonNegative(unitPrice, "unitPrice");
        this.unitPrice = unitPrice;
        // Recalcular lineTotal al cambiar unitPrice
        this.lineTotal = calculateLineTotal();
    }

    public void setLineTotal(BigDecimal lineTotal) {
        ValidationUtils.validateNonNegative(lineTotal, "lineTotal");
        this.lineTotal = lineTotal;
    }


    // Método helper para calcular total de la línea
    public BigDecimal calculateLineTotal() {
        return CalculationUtils.calculateOrderItemLineTotal(unitPrice, quantity);
    }

    // equals y hashCode basados en ID

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OrderItem orderItem = (OrderItem) o;
        return Objects.equals(orderItemId, orderItem.orderItemId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(orderItemId);
    }

    // toString sin navegación a objetos relacionados (solo IDs)

    @Override
    public String toString() {
        return "OrderItem{" +
                "orderItemId=" + orderItemId +
                ", orderId=" + (order != null ? order.getOrderId() : null) +
                ", productId=" + (product != null ? product.getProductId() : null) +
                ", quantity=" + quantity +
                ", unitPrice=" + unitPrice +
                ", lineTotal=" + lineTotal +
                '}';
    }
}
