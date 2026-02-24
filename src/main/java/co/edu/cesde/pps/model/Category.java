package co.edu.cesde.pps.model;

import lombok.*;
import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;



/**
 * Entidad Category - Organiza el catálogo en categorías jerárquicas.
 *
 * Soporta estructura tipo árbol mediante auto-referencia (parent).
 * Ejemplo: "Computadores" → "Portátiles" → "Gaming"
 *
 * Campos:
 * - categoryId: Identificador único de la categoría (PK)
 * - parent: Categoría padre (N:1 con Category) - NULLABLE para categorías raíz
 * - name: Nombre de la categoría
 * - slug: URL-friendly identifier (UNIQUE) - para URLs amigables
 * - subcategories: Lista de subcategorías (1:N con Category)
 * - products: Lista de productos de esta categoría (1:N con Product)
 *
 * Relaciones:
 * - N:1 con Category (auto-referencia para jerarquía - muchas categorías tienen un padre)
 * - 1:N con Category (una categoría tiene muchas subcategorías)
 * - 1:N con Product (una categoría tiene muchos productos)
 *
 * NOTA: Los métodos de gestión bidireccional (addSubcategory, removeSubcategory) fueron movidos
 * a la capa de servicio (CategoryService) en etapa 05 para mantener el modelo limpio.
 */
@Entity
@Table (name = "category")

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder


public class Category {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "category_id")
    private Long categoryId;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Category parent; // Nullable - NULL para categorías raíz
    @Column (unique = true, nullable = false, length = 100)
    private String name;
    @Column (unique = true, nullable = false, length = 100)
    private String slug;

    // Colecciones para relaciones 1:N
    @OneToMany(mappedBy = "parent", fetch = FetchType.LAZY)
    @Builder.Default
    private List<Category> subcategories = new ArrayList<>();
    @OneToMany(mappedBy = "category", fetch = FetchType.LAZY)
    @Builder.Default
    private List<Product> products = new ArrayList<>();



    // Métodos helper de consulta (sin efectos secundarios)

    /**
     * Verifica si es categoría raíz
     */
    public boolean isRootCategory() {
        return parent == null;
    }

    // equals y hashCode basados en ID

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Category category = (Category) o;
        return Objects.equals(categoryId, category.categoryId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(categoryId);
    }

    // toString personalizado sin navegación a objetos relacionados (solo IDs y tamaño de colecciones)

    @Override
    public String toString() {
        return "Category{" +
                "categoryId=" + categoryId +
                ", parentId=" + (parent != null ? parent.getCategoryId() : null) +
                ", name='" + name + '\'' +
                ", slug='" + slug + '\'' +
                ", isRoot=" + isRootCategory() +
                ", subcategoriesCount=" + (subcategories != null ? subcategories.size() : 0) +
                ", productsCount=" + (products != null ? products.size() : 0) +
                '}';
    }
}


