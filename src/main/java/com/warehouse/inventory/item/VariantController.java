package com.warehouse.inventory.item;

import com.warehouse.inventory.item.dto.AdjustStockRequest;
import com.warehouse.inventory.item.dto.UpdateVariantRequest;
import com.warehouse.inventory.item.dto.VariantResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/variants")
public class VariantController {

    private final VariantService variantService;

    public VariantController(VariantService variantService) {
        this.variantService = variantService;
    }

    @GetMapping("/{id}")
    public VariantResponse getVariant(@PathVariable Long id) {
        return variantService.getVariant(id);
    }

    @PutMapping("/{id}")
    public VariantResponse updateVariant(@PathVariable Long id, @Valid @RequestBody UpdateVariantRequest request) {
        return variantService.updateVariant(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteVariant(@PathVariable Long id) {
        variantService.deleteVariant(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/stock")
    public ResponseEntity<VariantResponse> adjustStock(@PathVariable Long id,
                                                       @Valid @RequestBody AdjustStockRequest request) {
        return ResponseEntity.ok(variantService.adjustStock(id, request));
    }
}
