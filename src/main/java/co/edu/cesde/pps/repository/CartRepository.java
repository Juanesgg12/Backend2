package co.edu.cesde.pps.repository;
import co.edu.cesde.pps.model.Cart;
import co.edu.cesde.pps.model.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
public interface CartRepository extends JpaRepository<Cart,Long> {
}
