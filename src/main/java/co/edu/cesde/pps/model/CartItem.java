package co.edu.cesde.pps.model;

import co.edu.cesde.pps.util.CalculationUtils;
import co.edu.cesde.pps.util.ValidationUtils;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Entidad CartItem - Detalle de items en el carrito de compras.
 *
 * Representa un producto agregado al carrito con su cantidad y precio congelado.
 *
 * Campos:
 * - cartItemId: Identificador único del item (PK)
 * - cart: Carrito al que pertenece (N:1 con Cart)
 * - product: Producto agregado (N:1 con Product)
 * - quantity: Cantidad del producto en el carrito
 * - unitPrice: Precio unitario congelado al agregar (BigDecimal para precisión)
 * - addedAt: Fecha en que se agregó el item al carrito
 *
 * Restricción UNIQUE (cart, product):
 * Un producto no puede aparecer duplicado en el mismo carrito. Si se agrega
 * el mismo producto dos veces, se debe actualizar la cantidad del item existente.
 *
 * Congelación de precio (unitPrice):
 * Se guarda el precio del producto en el momento de agregarlo al carrito.
 * Esto asegura consistencia si el precio del producto cambia mientras el
 * usuario navega. El precio se "congela" al agregar al carrito.
 *
 * Relaciones:
 * - N:1 con Cart (muchos items pertenecen a un carrito)
 * - N:1 con Product (muchos items referencian a un producto)
 */
@Entity
@Table (name = "cart_items" , uniqueConstraints = {@UniqueConstraint(columnNames = {"cart_id", "product_id"})})

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CartItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cart_item_id")
    private Long cartItemId;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cart_id", nullable = false)
    @JsonBackReference
    private Cart cart;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;
    @Column(nullable = false)
    private Integer quantity;
    @Column(name = "unit_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPrice;
    @Column(name = "added_at", nullable = false)
    @Builder.Default
    private LocalDateTime addedAt = LocalDateTime.now();

    // Setters personalizados con validación (override de Lombok)

    public void setQuantity(Integer quantity) {
        ValidationUtils.validatePositive(quantity, "quantity");
        this.quantity = quantity;
    }
    public void setUnitPrice(BigDecimal unitPrice) {
        ValidationUtils.validateNonNegative(unitPrice, "unitPrice");
        this.unitPrice = unitPrice;
    }


    // Método helper para calcular subtotal del item
    public BigDecimal calculateSubtotal() {
        return CalculationUtils.calculateCartItemSubtotal(unitPrice, quantity);
    }
    // equals y hashCode basados en ID

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CartItem cartItem = (CartItem) o;
        return Objects.equals(cartItemId, cartItem.cartItemId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(cartItemId);
    }

    // toString personalizado sin navegación a objetos relacionados (solo IDs)

    @Override
    public String toString() {
        return "CartItem{" +
                "cartItemId=" + cartItemId +
                ", cartId=" + (cart != null ? cart.getCartId() : null) +
                ", productId=" + (product != null ? product.getProductId() : null) +
                ", quantity=" + quantity +
                ", unitPrice=" + unitPrice +
                ", subtotal=" + calculateSubtotal() +
                ", addedAt=" + addedAt +
                '}';
    }

}
