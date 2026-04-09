package co.edu.cesde.pps.repository;

import co.edu.cesde.pps.model.Cart;
import co.edu.cesde.pps.enums.CartStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface CartRepository extends JpaRepository<Cart, Long> {

    Optional<Cart> findByUserUserIdAndStatus(Long userId, CartStatus status);

    List<Cart> findByUserUserId(Long userId);

    Optional<Cart> findBySessionSessionIdAndStatus(Long sessionId, CartStatus status);
}