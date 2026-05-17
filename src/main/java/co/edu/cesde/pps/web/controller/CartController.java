package co.edu.cesde.pps.web.controller;

import co.edu.cesde.pps.dto.CartDTO;
import co.edu.cesde.pps.dto.CartItemDTO;
import co.edu.cesde.pps.model.User;
import co.edu.cesde.pps.model.UserSession;
import co.edu.cesde.pps.service.CartService;
import co.edu.cesde.pps.web.dto.request.AddCartItemRequest;
import co.edu.cesde.pps.web.dto.request.MergeGuestCartRequest;
import co.edu.cesde.pps.web.dto.request.UpdateCartItemQuantityRequest;
import co.edu.cesde.pps.web.dto.response.*;
import co.edu.cesde.pps.web.security.CurrentSessionResolver;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping(ApiRoutes.CART)
public class CartController {

    private final CartService cartService;
    private final CurrentSessionResolver sessionResolver;

    public CartController(CartService cartService,
                          CurrentSessionResolver sessionResolver) {
        this.cartService = cartService;
        this.sessionResolver = sessionResolver;
    }

    // GET /api/v1/cart/me
    // Funciona tanto para guest como para usuario autenticado
    @GetMapping("/me")
    public ResponseEntity<CartResponse> getMyCart(
            HttpServletRequest request) {

        // resolveSessionOrNull devuelve null si no hay token
        // en vez de lanzar excepción
        UserSession session = sessionResolver.resolveSessionOrNull(request);

        if (session == null) {
            return ResponseEntity.noContent().build();
        }

        // Buscar el carrito según si es guest o autenticado
        Long cartId = session.isGuestSession()
                ? getGuestCartId(session)
                : getUserCartId(session.getUser());

        if (cartId == null) {
            return ResponseEntity.noContent().build();
        }

        CartDTO cart = cartService.findById(cartId);
        return ResponseEntity.ok(toResponse(cart));
    }

    // POST /api/v1/cart/items
    @PostMapping("/items")
    public ResponseEntity<CartResponse> addItem(
            @RequestBody @Valid AddCartItemRequest body,
            HttpServletRequest request) {

        UserSession session = sessionResolver.resolveSession(request);
        Long cartId = resolveCartId(session);

        CartDTO cart = cartService.addItem(
                cartId,
                body.productId(),
                body.quantity());

        return ResponseEntity.ok(toResponse(cart));
    }

    // PATCH /api/v1/cart/items/{productId}
    @PatchMapping("/items/{productId}")
    public ResponseEntity<CartResponse> updateItem(
            @PathVariable Long productId,
            @RequestBody @Valid UpdateCartItemQuantityRequest body,
            HttpServletRequest request) {

        UserSession session = sessionResolver.resolveSession(request);
        Long cartId = resolveCartId(session);

        CartDTO cart = cartService.updateItemQuantity(
                cartId, productId, body.quantity());

        return ResponseEntity.ok(toResponse(cart));
    }

    // DELETE /api/v1/cart/items/{productId}
    @DeleteMapping("/items/{productId}")
    public ResponseEntity<CartResponse> removeItem(
            @PathVariable Long productId,
            HttpServletRequest request) {

        UserSession session = sessionResolver.resolveSession(request);
        Long cartId = resolveCartId(session);

        CartDTO cart = cartService.removeItem(cartId, productId);
        return ResponseEntity.ok(toResponse(cart));
    }

    // DELETE /api/v1/cart/items
    // Vacía todo el carrito
    @DeleteMapping("/items")
    public ResponseEntity<CartResponse> clearCart(
            HttpServletRequest request) {

        UserSession session = sessionResolver.resolveSession(request);
        Long cartId = resolveCartId(session);

        CartDTO cart = cartService.clearCart(cartId);
        return ResponseEntity.ok(toResponse(cart));
    }

    // POST /api/v1/cart/merge
    // Fusiona el carrito guest con el carrito del usuario autenticado
    @PostMapping("/merge")
    public ResponseEntity<CartResponse> mergeCart(
            @RequestBody @Valid MergeGuestCartRequest body,
            HttpServletRequest request) {

        User user = sessionResolver.resolveAuthenticatedUser(request);

        CartDTO cart = cartService.mergeGuestCartToUserCart(
                body.guestCartId(),
                user.getUserId());

        return ResponseEntity.ok(toResponse(cart));
    }

    // ── Helpers privados ───────────────────────────────────────────────────

    // Obtiene el cartId según el tipo de sesión
    private Long resolveCartId(UserSession session) {
        if (session.isGuestSession()) {
            return getGuestCartId(session);
        }
        return getUserCartId(session.getUser());
    }

    private Long getGuestCartId(UserSession session) {
        try {
            CartDTO cart = cartService.findById(session.getSessionId());
            return cart.getCartId();
        } catch (Exception e) {
            return null;
        }
    }

    private Long getUserCartId(User user) {
        try {
            // Buscar carrito abierto del usuario
            return cartService.findById(user.getUserId()).getCartId();
        } catch (Exception e) {
            return null;
        }
    }

    // ── Mappers privados ───────────────────────────────────────────────────

    private CartResponse toResponse(CartDTO dto) {
        List<CartItemResponse> items = dto.getItems().stream()
                .map(this::toItemResponse)
                .toList();

        // Calcular summary desde los items
        BigDecimal subtotal = dto.getTotal() != null
                ? dto.getTotal() : BigDecimal.ZERO;
        BigDecimal tax = subtotal.multiply(BigDecimal.valueOf(0.19));
        BigDecimal total = subtotal.add(tax);

        CartSummaryResponse summary = new CartSummaryResponse(
                items.size(),
                subtotal,
                tax,
                BigDecimal.ZERO,
                total
        );

        return new CartResponse(
                dto.getCartId(),
                dto.getStatus(),
                Boolean.TRUE.equals(dto.getIsGuest()),
                items,
                summary,
                dto.getUpdatedAt()
        );
    }

    private CartItemResponse toItemResponse(CartItemDTO dto) {
        return new CartItemResponse(
                dto.getCartItemId(),
                dto.getProductId(),
                dto.getProductName(),
                dto.getProductSku(),
                dto.getQuantity(),
                dto.getUnitPrice(),
                dto.getSubtotal()
        );
    }
}