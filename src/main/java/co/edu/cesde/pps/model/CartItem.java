package co.edu.cesde.pps.model;

import co.edu.cesde.pps.util.CalculationUtils;
import co.edu.cesde.pps.util.ValidationUtils;
import lombok.*;

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
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CartItem {

    private Long cartItemId;
    private Cart cart;
    private Product product;
    private Integer quantity;
    private BigDecimal unitPrice;
    private LocalDateTime addedAt;



    // Constructor con campos obligatorios
    public CartItem(Cart cart, Product product, Integer quantity, BigDecimal unitPrice) {
        this.cart = cart;
        this.product = product;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.addedAt = LocalDateTime.now();
    }



    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        ValidationUtils.validateNonNegative(unitPrice, "unitPrice");
        this.unitPrice = unitPrice;
    }

    public LocalDateTime getAddedAt() {
        return addedAt;
    }

    public void setAddedAt(LocalDateTime addedAt) {
        this.addedAt = addedAt;
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

    // toString sin navegación a objetos relacionados (solo IDs)


}
