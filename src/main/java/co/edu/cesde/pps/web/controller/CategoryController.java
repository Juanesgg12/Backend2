package co.edu.cesde.pps.web.controller;

import co.edu.cesde.pps.dto.CategoryDTO;
import co.edu.cesde.pps.service.CategoryService;
import co.edu.cesde.pps.web.dto.response.CategoryResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// Endpoints públicos — no requieren autenticación
@RestController
@RequestMapping(ApiRoutes.CATEGORIES)
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    // GET /api/v1/categories
    @GetMapping
    public ResponseEntity<List<CategoryResponse>> getAllCategories() {
        List<CategoryResponse> categories = categoryService
                .findAllCategories()
                .stream()
                .map(this::toResponse)
                .toList();

        return ResponseEntity.ok(categories);
    }

    // GET /api/v1/categories/tree
    // Devuelve árbol completo con subcategorías anidadas
    @GetMapping("/tree")
    public ResponseEntity<List<CategoryResponse>> getCategoryTree() {
        List<CategoryResponse> tree = categoryService
                .buildFullCategoryTree()
                .stream()
                .map(this::toResponseWithChildren)
                .toList();

        return ResponseEntity.ok(tree);
    }

    // GET /api/v1/categories/{id}
    @GetMapping("/{id}")
    public ResponseEntity<CategoryResponse> getCategory(
            @PathVariable Long id) {

        CategoryDTO category = categoryService.findById(id);
        return ResponseEntity.ok(toResponse(category));
    }

    // GET /api/v1/categories/{id}/subcategories
    @GetMapping("/{id}/subcategories")
    public ResponseEntity<List<CategoryResponse>> getSubcategories(
            @PathVariable Long id) {

        List<CategoryResponse> subcategories = categoryService
                .findSubcategories(id)
                .stream()
                .map(this::toResponse)
                .toList();

        return ResponseEntity.ok(subcategories);
    }

    // ── Mappers privados ───────────────────────────────────────────────────

    private CategoryResponse toResponse(CategoryDTO dto) {
        return new CategoryResponse(
                dto.getCategoryId(),
                dto.getName(),
                dto.getSlug(),
                dto.getParentId(),
                dto.getParentName(),
                Boolean.TRUE.equals(dto.getIsRoot()),
                List.of() // sin subcategorías anidadas en listado plano
        );
    }

    // Para el endpoint /tree incluye subcategorías recursivamente
    private CategoryResponse toResponseWithChildren(CategoryDTO dto) {
        List<CategoryResponse> children = dto.getSubcategories() != null
                ? dto.getSubcategories().stream()
                .map(this::toResponseWithChildren)
                .toList()
                : List.of();

        return new CategoryResponse(
                dto.getCategoryId(),
                dto.getName(),
                dto.getSlug(),
                dto.getParentId(),
                dto.getParentName(),
                Boolean.TRUE.equals(dto.getIsRoot()),
                children
        );
    }
}