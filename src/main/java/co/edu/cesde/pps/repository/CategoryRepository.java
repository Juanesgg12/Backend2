package co.edu.cesde.pps.repository;

import co.edu.cesde.pps.model.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    Optional<Category> findBySlugIgnoreCase(String slug);

    List<Category> findByParentIsNull();

    List<Category> findByParentCategoryId(Long parentId);
    boolean existsBySlugIgnoreCase(String slug);
}