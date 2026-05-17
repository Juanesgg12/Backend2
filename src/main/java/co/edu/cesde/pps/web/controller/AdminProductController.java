package co.edu.cesde.pps.web.controller;

import co.edu.cesde.pps.dto.CategoryDTO;
import co.edu.cesde.pps.dto.ProductDTO;
import co.edu.cesde.pps.service.CategoryService;
import co.edu.cesde.pps.service.ProductService;
import co.edu.cesde.pps.web.dto.request.CategoryUpsertRequest;
import co.edu.cesde.pps.web.dto.request.ProductUpsertRequest;
import co.edu.cesde.pps.web.dto.response.CategoryResponse;
import co.edu.cesde.pps.web.dto.response.ProductResponse;
import co.edu.cesde.pps.web.security.CurrentSessionResolver;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(ApiRoutes.ADMIN)
public class AdminProductController {

    private final ProductService productService;
    private final CategoryService categoryService;
    private final CurrentSessionResolver sessionResolver;

    public AdminProductController(ProductService productService,
                                  CategoryService categoryService,
                                  CurrentSessionResolver sessionResolver) {
        this.productService = productService;
        this.categoryService = categoryService;
        this.sessionResolver = sessionResolver;
    }

    // ── Productos ──────────────────────────────────────────────────────────

    // POST /api/v1/admin/products
    @PostMapping("/products")
    public ResponseEntity<ProductResponse> createProduct(
            @RequestBody @Valid ProductUpsertRequest body,
            HttpServletRequest request) {

        // Por ahora solo verificamos que esté autenticado
        // Control de rol admin queda como mejora futura
        sessionResolver.resolveAuthenticatedUser(request);

        ProductDTO dto = toProductDTO(body);
        ProductDTO created = productService.createProduct(dto);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(toProductResponse(created));
    }

    // PUT /api/v1/admin/products/{id}
    @PutMapping("/products/{id}")
    public ResponseEntity<ProductResponse> updateProduct(
            @PathVariable Long id,
            @RequestBody @Valid ProductUpsertRequest body,
            HttpServletRequest request) {

        sessionResolver.resolveAuthenticatedUser(request);

        ProductDTO dto = toProductDTO(body);
        ProductDTO updated = productService.updateProduct(id, dto);

        return ResponseEntity.ok(toProductResponse(updated));
    }

    // DELETE /api/v1/admin/products/{id}
    @DeleteMapping("/products/{id}")
    public ResponseEntity<Void> deleteProduct(
            @PathVariable Long id,
            HttpServletRequest request) {

        sessionResolver.resolveAuthenticatedUser(request);
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }

    // ── Categorías ─────────────────────────────────────────────────────────

    // POST /api/v1/admin/categories
    @PostMapping("/categories")
    public ResponseEntity<CategoryResponse> createCategory(
            @RequestBody @Valid CategoryUpsertRequest body,
            HttpServletRequest request) {

        sessionResolver.resolveAuthenticatedUser(request);

        CategoryDTO dto = toCategoryDTO(body);
        CategoryDTO created = categoryService.createCategory(dto);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(toCategoryResponse(created));
    }

    // PUT /api/v1/admin/categories/{id}
    @PutMapping("/categories/{id}")
    public ResponseEntity<CategoryResponse> updateCategory(
            @PathVariable Long id,
            @RequestBody @Valid CategoryUpsertRequest body,
            HttpServletRequest request) {

        sessionResolver.resolveAuthenticatedUser(request);

        CategoryDTO dto = toCategoryDTO(body);
        CategoryDTO updated = categoryService.updateCategory(id, dto);

        return ResponseEntity.ok(toCategoryResponse(updated));
    }

    // ── Mappers privados ───────────────────────────────────────────────────

    private ProductDTO toProductDTO(ProductUpsertRequest request) {
        ProductDTO dto = new ProductDTO();
        dto.setCategoryId(request.categoryId());
        dto.setSku(request.sku());
        dto.setName(request.name());
        dto.setDescription(request.description());
        dto.setPrice(request.price());
        dto.setStockQty(request.stockQty());
        dto.setIsActive(request.isActive());
        return dto;
    }

    private ProductResponse toProductResponse(ProductDTO dto) {
        return new ProductResponse(
                dto.getProductId(),
                dto.getSku(),
                dto.getName(),
                dto.getDescription(),
                dto.getPrice(),
                dto.getStockQty(),
                Boolean.TRUE.equals(dto.getIsActive()),
                Boolean.TRUE.equals(dto.getIsAvailable()),
                dto.getCategoryId(),
                dto.getCategoryName(),
                dto.getCreatedAt()
        );
    }

    private CategoryDTO toCategoryDTO(CategoryUpsertRequest request) {
        CategoryDTO dto = new CategoryDTO();
        dto.setName(request.name());
        dto.setSlug(request.slug());
        dto.setParentId(request.parentId());
        return dto;
    }

    private CategoryResponse toCategoryResponse(CategoryDTO dto) {
        return new CategoryResponse(
                dto.getCategoryId(),
                dto.getName(),
                dto.getSlug(),
                dto.getParentId(),
                dto.getParentName(),
                Boolean.TRUE.equals(dto.getIsRoot()),
                List.of()
        );
    }
}