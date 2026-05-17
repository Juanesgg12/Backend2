package co.edu.cesde.pps.web.controller;

import co.edu.cesde.pps.dto.ProductDTO;
import co.edu.cesde.pps.service.ProductService;
import co.edu.cesde.pps.web.dto.response.ProductResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// Endpoints públicos — catálogo visible sin autenticación
@RestController
@RequestMapping(ApiRoutes.PRODUCTS)
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    // GET /api/v1/products
    // Soporta filtros opcionales por query params:
    // ?search=mouse  ?categoryId=3  ?activeOnly=true
    @GetMapping
    public ResponseEntity<List<ProductResponse>> getProducts(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false, defaultValue = "true") boolean activeOnly) {

        List<ProductDTO> products;

        if (search != null && !search.isBlank()) {
            // Búsqueda por nombre
            products = productService.searchByName(search);
        } else if (categoryId != null) {
            // Filtro por categoría
            products = productService.findByCategory(categoryId);
        } else if (activeOnly) {
            // Solo productos activos (default)
            products = productService.findActiveProducts();
        } else {
            // Todos los productos (admin)
            products = productService.findAllProducts();
        }

        List<ProductResponse> response = products.stream()
                .map(this::toResponse)
                .toList();

        return ResponseEntity.ok(response);
    }

    // GET /api/v1/products/{id}
    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> getProduct(
            @PathVariable Long id) {

        ProductDTO product = productService.findById(id);
        return ResponseEntity.ok(toResponse(product));
    }

    // ── Mapper privado ─────────────────────────────────────────────────────

    private ProductResponse toResponse(ProductDTO dto) {
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
}