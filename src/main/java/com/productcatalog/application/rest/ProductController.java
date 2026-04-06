package com.productcatalog.application.rest;

import com.productcatalog.application.rest.mappers.ProductMapper;
import com.productcatalog.application.rest.params.ProductParams;
import com.productcatalog.domain.model.CatalogSearchResult;
import com.productcatalog.domain.model.Product;
import com.productcatalog.domain.model.ProductStatus;
import com.productcatalog.domain.ports.out.ProductRepository;
import com.productcatalog.domain.ports.out.TrackRepository;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@Tag(name = "Products", description = "Music product submission and management")
@RestController
@RequestMapping("/products")
public class ProductController {

    private final ProductRepository productRepository;
    private final TrackRepository trackRepository;
    private final ProductMapper productMapper;

    public ProductController(ProductRepository productRepository, TrackRepository trackRepository, ProductMapper productMapper) {
        this.productRepository = productRepository;
        this.trackRepository = trackRepository;
        this.productMapper = productMapper;
    }

    @Operation(summary = "Submit a new product for validation")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Product created and submitted for validation"),
            @ApiResponse(responseCode = "400", description = "Invalid product data")
    })
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

    @Operation(summary = "Get a product by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Product found"),
            @ApiResponse(responseCode = "404", description = "Product not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<Product> getById(@PathVariable UUID id) {
        return productRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Get all products")
    @ApiResponse(responseCode = "200", description = "List of products")
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

    @Operation(summary = "Update a product")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Product updated"),
            @ApiResponse(responseCode = "400", description = "Invalid product data"),
            @ApiResponse(responseCode = "404", description = "Product not found")
    })
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
    @Operation(summary = "Search the catalog by any combination of artist, label, genre, track title, ISRC, or status")
    @ApiResponse(responseCode = "200", description = "Search results")
    @GetMapping("/search")
    public ResponseEntity<List<CatalogSearchResult>> search(
            @RequestParam(required = false) String artist,
            @RequestParam(required = false) String label,
            @RequestParam(required = false) String genre,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String isrc,
            @RequestParam(required = false) String status) {
        return ResponseEntity.ok(trackRepository.searchCatalog(artist, label, genre, title, isrc, status));
    }
}