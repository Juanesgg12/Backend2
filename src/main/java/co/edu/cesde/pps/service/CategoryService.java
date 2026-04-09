package co.edu.cesde.pps.service;

import co.edu.cesde.pps.dto.CategoryDTO;
import co.edu.cesde.pps.exception.DuplicateEntityException;
import co.edu.cesde.pps.exception.EntityNotFoundException;
import co.edu.cesde.pps.exception.ValidationException;
import co.edu.cesde.pps.mapper.CategoryMapper;
import co.edu.cesde.pps.model.Category;
import co.edu.cesde.pps.repository.CategoryRepository;
import co.edu.cesde.pps.util.StringUtils;
import co.edu.cesde.pps.util.ValidationUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// @Service le dice a Spring que esta clase es un bean de servicio.
// Spring la registra y la puede inyectar en otros componentes con @Autowired
// o por constructor (que es la forma recomendada).
@Service
// @Transactional(readOnly = true) significa que por defecto todos los métodos
// de esta clase abren una transacción de solo lectura.
// Esto le dice a la base de datos "no voy a modificar nada" → mejor rendimiento.
// Los métodos que SÍ modifican datos sobreescriben esto con @Transactional propio.
@Transactional(readOnly = true)
public class CategoryService {

    private final CategoryMapper categoryMapper;
    private final CategoryRepository categoryRepository;

    // Inyección por constructor: Spring detecta que CategoryRepository es un
    // bean (porque extiende JpaRepository) y lo inyecta automáticamente aquí.
    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryMapper = new CategoryMapper();
        this.categoryRepository = categoryRepository;
    }

    @Transactional  // Sobreescribe readOnly=true porque aquí sí guardamos datos
    public CategoryDTO createCategory(CategoryDTO categoryDTO) {
        ValidationUtils.validateNotBlank(categoryDTO.getName(), "name");

        String slug = categoryDTO.getSlug();
        if (slug == null || slug.isBlank()) {
            slug = StringUtils.slugify(categoryDTO.getName());
        }

        // existsBySlugIgnoreCase viene del método que definimos en el repositorio
        if (categoryRepository.existsBySlugIgnoreCase(slug)) {
            throw new DuplicateEntityException("Category", "slug", slug);
        }

        Category category = categoryMapper.toEntity(categoryDTO);
        // Ya no asignamos ID manualmente: JPA lo genera con @GeneratedValue
        category.setSlug(slug);

        if (categoryDTO.getParentId() != null) {
            Category parent = findCategoryEntityOrThrow(categoryDTO.getParentId());
            category.setParent(parent);
        }

        // categoryRepository.save() persiste en la base de datos y devuelve
        // la entidad con el ID generado asignado
        Category saved = categoryRepository.save(category);
        return categoryMapper.toDTO(saved);
    }

    @Transactional
    public CategoryDTO updateCategory(Long categoryId, CategoryDTO categoryDTO) {
        Category category = findCategoryEntityOrThrow(categoryId);
        ValidationUtils.validateNotBlank(categoryDTO.getName(), "name");

        String newSlug = categoryDTO.getSlug();
        if (newSlug == null || newSlug.isBlank()) {
            newSlug = StringUtils.slugify(categoryDTO.getName());
        }

        if (!category.getSlug().equalsIgnoreCase(newSlug) &&
                categoryRepository.existsBySlugIgnoreCase(newSlug)) {
            throw new DuplicateEntityException("Category", "slug", newSlug);
        }

        category.setName(categoryDTO.getName());
        category.setSlug(newSlug);

        if (categoryDTO.getParentId() != null) {
            if (categoryDTO.getParentId().equals(categoryId)) {
                throw new ValidationException("Category cannot be its own parent");
            }
            Category newParent = findCategoryEntityOrThrow(categoryDTO.getParentId());
            if (wouldCreateCycle(category, newParent)) {
                throw new ValidationException("Cannot create cycle in category hierarchy");
            }
            category.setParent(newParent);
        } else {
            category.setParent(null);
        }

        // Dentro de una transacción activa, JPA detecta automáticamente los cambios
        // en entidades "managed" (gestionadas) y hace el UPDATE al hacer flush.
        // El save() explícito también funciona y es más claro para el lector.
        Category saved = categoryRepository.save(category);
        return categoryMapper.toDTO(saved);
    }

    @Transactional
    public void deleteCategory(Long categoryId) {
        Category category = findCategoryEntityOrThrow(categoryId);

        if (category.getSubcategories() != null && !category.getSubcategories().isEmpty()) {
            throw new ValidationException("Cannot delete category with subcategories");
        }
        if (category.getProducts() != null && !category.getProducts().isEmpty()) {
            throw new ValidationException("Cannot delete category with products");
        }

        categoryRepository.delete(category);
    }

    public CategoryDTO findById(Long categoryId) {
        return categoryMapper.toDTO(findCategoryEntityOrThrow(categoryId));
    }

    public CategoryDTO findBySlug(String slug) {
        Category category = categoryRepository.findBySlugIgnoreCase(slug)
                .orElseThrow(() -> new EntityNotFoundException("Category with slug: " + slug));
        return categoryMapper.toDTO(category);
    }

    public List<CategoryDTO> findAllCategories() {
        return categoryMapper.toDTOList(categoryRepository.findAll());
    }

    public List<CategoryDTO> findRootCategories() {
        return categoryMapper.toDTOList(categoryRepository.findByParentIsNull());
    }

    public List<CategoryDTO> findSubcategories(Long parentId) {
        findCategoryEntityOrThrow(parentId); // Valida que el padre existe
        return categoryMapper.toDTOList(categoryRepository.findByParentCategoryId(parentId));
    }

    @Transactional
    public CategoryDTO addSubcategory(Long parentId, CategoryDTO subcategoryDTO) {
        Category parent = findCategoryEntityOrThrow(parentId);
        ValidationUtils.validateNotBlank(subcategoryDTO.getName(), "name");

        String slug = subcategoryDTO.getSlug();
        if (slug == null || slug.isBlank()) {
            slug = StringUtils.slugify(subcategoryDTO.getName());
        }
        if (categoryRepository.existsBySlugIgnoreCase(slug)) {
            throw new DuplicateEntityException("Category", "slug", slug);
        }

        Category subcategory = categoryMapper.toEntity(subcategoryDTO);
        subcategory.setSlug(slug);
        subcategory.setParent(parent);

        Category saved = categoryRepository.save(subcategory);
        return categoryMapper.toDTO(saved);
    }

    public List<CategoryDTO> buildFullCategoryTree() {
        List<Category> roots = categoryRepository.findByParentIsNull();
        return categoryMapper.toDTOListWithHierarchy(roots);
    }

    public CategoryDTO buildCategoryTree(Long categoryId) {
        Category category = findCategoryEntityOrThrow(categoryId);
        return categoryMapper.toDTOWithHierarchy(category);
    }

    public boolean existsBySlug(String slug) {
        return categoryRepository.existsBySlugIgnoreCase(slug);
    }

    // Método público porque ProductService también lo necesita para validar
    // que una categoría existe antes de asignarla a un producto
    public Category findCategoryEntityOrThrow(Long categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new EntityNotFoundException("Category", categoryId));
    }

    private boolean wouldCreateCycle(Category category, Category newParent) {
        Category current = newParent;
        while (current != null) {
            if (current.getCategoryId().equals(category.getCategoryId())) return true;
            current = current.getParent();
        }
        return false;
    }
}