package com.productcatalog.application.rest;

import com.productcatalog.application.rest.mappers.ProductMapper;
import com.productcatalog.application.rest.params.ProductParams;
import com.productcatalog.domain.model.Product;
import com.productcatalog.domain.model.ProductStatus;
import com.productcatalog.domain.ports.out.ProductRepository;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/products")
public class ProductController {

    private final ProductRepository productRepository;
    private final ProductMapper productMapper;

    public ProductController(ProductRepository productRepository, ProductMapper productMapper) {
        this.productRepository = productRepository;
        this.productMapper = productMapper;
    }

    @PostMapping
    public ResponseEntity<Product> create(@Valid @RequestBody ProductParams productParams) {
        Product product = productMapper.toProductFromProductParams(productParams);
        Product toSave = product.toBuilder()
                .id(UUID.randomUUID())
                .status(ProductStatus.SUBMITTED)
                .build();
        Product saved = productRepository.save(toSave);
        return ResponseEntity.created(URI.create("/products/" + saved.getId())).body(saved);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Product> getById(@PathVariable UUID id) {
        return productRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    public ResponseEntity<List<Product>> getAll() {
        return ResponseEntity.ok(productRepository.findAll());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        if (productRepository.findById(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        productRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}")
    public ResponseEntity<Void> update(
            @PathVariable UUID id,
            @Valid @RequestBody ProductParams productParams) {
        if (productRepository.findById(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        Product product = productMapper.toProductFromProductParams(productParams);

        productRepository.update(product.toBuilder().id(id).build());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/resubmit")
    public ResponseEntity<Void> resubmit(@PathVariable UUID id) {
        return productRepository.findById(id)
                .map(product -> {
                    productRepository.resubmit(id);
                    return ResponseEntity.ok().<Void>build();
                })
                .orElse(ResponseEntity.notFound().build());
    }
}