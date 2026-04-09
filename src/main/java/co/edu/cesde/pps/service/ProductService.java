package co.edu.cesde.pps.service;

import co.edu.cesde.pps.dto.ProductDTO;
import co.edu.cesde.pps.exception.DuplicateEntityException;
import co.edu.cesde.pps.exception.EntityNotFoundException;
import co.edu.cesde.pps.exception.InsufficientStockException;
import co.edu.cesde.pps.mapper.ProductMapper;
import co.edu.cesde.pps.model.Category;
import co.edu.cesde.pps.model.Product;
import co.edu.cesde.pps.repository.ProductRepository;
import co.edu.cesde.pps.util.CalculationUtils;
import co.edu.cesde.pps.util.ValidationUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class ProductService {

    private final ProductMapper productMapper;
    private final ProductRepository productRepository;
    private final CategoryService categoryService;

    public ProductService(ProductRepository productRepository,
                          CategoryService categoryService) {
        this.productMapper = new ProductMapper();
        this.productRepository = productRepository;
        this.categoryService = categoryService;
    }

    @Transactional
    public ProductDTO createProduct(ProductDTO productDTO) {
        ValidationUtils.validateNotBlank(productDTO.getSku(), "sku");
        ValidationUtils.validateNotBlank(productDTO.getName(), "name");
        ValidationUtils.validateNonNegative(productDTO.getPrice(), "price");
        ValidationUtils.validateNonNegative(
                BigDecimal.valueOf(productDTO.getStockQty()), "stockQty");

        if (productRepository.existsBySkuIgnoreCase(productDTO.getSku())) {
            throw new DuplicateEntityException("Product", "sku", productDTO.getSku());
        }

        Category category = categoryService
                .findCategoryEntityOrThrow(productDTO.getCategoryId());

        Product product = productMapper.toEntity(productDTO);
        product.setCategory(category);
        product.setCreatedAt(LocalDateTime.now());

        Product saved = productRepository.save(product);
        return productMapper.toDTO(saved);
    }

    @Transactional
    public ProductDTO updateProduct(Long productId, ProductDTO productDTO) {
        Product product = findProductEntityOrThrow(productId);

        if (!product.getSku().equalsIgnoreCase(productDTO.getSku()) &&
                productRepository.existsBySkuIgnoreCase(productDTO.getSku())) {
            throw new DuplicateEntityException("Product", "sku", productDTO.getSku());
        }

        ValidationUtils.validateNotBlank(productDTO.getName(), "name");
        ValidationUtils.validateNonNegative(productDTO.getPrice(), "price");
        ValidationUtils.validateNonNegative(
                BigDecimal.valueOf(productDTO.getStockQty()), "stockQty");

        product.setSku(productDTO.getSku());
        product.setName(productDTO.getName());
        product.setDescription(productDTO.getDescription());
        product.setPrice(productDTO.getPrice());
        product.setStockQty(productDTO.getStockQty());
        product.setIsActive(productDTO.getIsActive());

        if (productDTO.getCategoryId() != null &&
                !productDTO.getCategoryId()
                        .equals(product.getCategory().getCategoryId())) {
            Category newCategory = categoryService
                    .findCategoryEntityOrThrow(productDTO.getCategoryId());
            product.setCategory(newCategory);
        }

        Product saved = productRepository.save(product);
        return productMapper.toDTO(saved);
    }

    @Transactional
    public void deleteProduct(Long productId) {
        Product product = findProductEntityOrThrow(productId);
        // Soft delete: no borramos el registro, solo lo desactivamos.
        // Así no se rompen órdenes históricas que referencian este producto.
        product.setIsActive(false);
        productRepository.save(product);
    }

    public ProductDTO findById(Long productId) {
        return productMapper.toDTO(findProductEntityOrThrow(productId));
    }

    public ProductDTO findBySku(String sku) {
        Product product = productRepository.findBySkuIgnoreCase(sku)
                .orElseThrow(() -> new EntityNotFoundException("Product with SKU: " + sku));
        return productMapper.toDTO(product);
    }

    public List<ProductDTO> findAllProducts() {
        return productMapper.toDTOList(productRepository.findAll());
    }

    public List<ProductDTO> findActiveProducts() {
        return productMapper.toDTOList(productRepository.findByIsActiveTrue());
    }

    public List<ProductDTO> findByCategory(Long categoryId) {
        categoryService.findCategoryEntityOrThrow(categoryId);
        return productMapper.toDTOList(
                productRepository.findByCategoryCategoryId(categoryId));
    }

    public List<ProductDTO> searchByName(String name) {
        return productMapper.toDTOList(
                productRepository.findByNameContainingIgnoreCase(name));
    }

    public boolean checkAvailability(Long productId, Integer quantity) {
        Product product = findProductEntityOrThrow(productId);
        return product.getIsActive() &&
                CalculationUtils.hasEnoughStock(product.getStockQty(), quantity);
    }

    @Transactional
    public void updateStock(Long productId, Integer newStock) {
        Product product = findProductEntityOrThrow(productId);
        ValidationUtils.validateNonNegative(BigDecimal.valueOf(newStock), "stock");
        product.setStockQty(newStock);
        productRepository.save(product);
    }

    @Transactional
    public void decreaseStock(Long productId, Integer quantity) {
        Product product = findProductEntityOrThrow(productId);
        if (!CalculationUtils.hasEnoughStock(product.getStockQty(), quantity)) {
            throw new InsufficientStockException(
                    productId, product.getSku(), quantity, product.getStockQty());
        }
        product.setStockQty(
                CalculationUtils.calculateNewStock(product.getStockQty(), quantity));
        productRepository.save(product);
    }

    @Transactional
    public void increaseStock(Long productId, Integer quantity) {
        Product product = findProductEntityOrThrow(productId);
        ValidationUtils.validatePositive(quantity, "quantity");
        product.setStockQty(product.getStockQty() + quantity);
        productRepository.save(product);
    }

    public boolean existsBySku(String sku) {
        return productRepository.existsBySkuIgnoreCase(sku);
    }

    public Product findProductEntityOrThrow(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new EntityNotFoundException("Product", productId));
    }
}