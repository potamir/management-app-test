package com.warehouse.inventory.item;

import com.warehouse.inventory.item.dto.AdjustStockRequest;
import com.warehouse.inventory.item.dto.CreateItemRequest;
import com.warehouse.inventory.item.dto.CreateVariantRequest;
import com.warehouse.inventory.item.dto.ItemResponse;
import com.warehouse.inventory.item.dto.UpdateItemRequest;
import com.warehouse.inventory.item.dto.VariantResponse;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/items")
public class ItemController {

    private final ItemService itemService;
    private final VariantService variantService;

    public ItemController(ItemService itemService, VariantService variantService) {
        this.itemService = itemService;
        this.variantService = variantService;
    }

    @GetMapping
    public List<ItemResponse> listItems(@RequestParam(required = false) Boolean inStock) {
        return itemService.listItems(inStock);
    }

    @GetMapping("/{id}")
    public ItemResponse getItem(@PathVariable Long id) {
        return itemService.getItem(id);
    }

    @PostMapping
    public ResponseEntity<ItemResponse> createItem(@Valid @RequestBody CreateItemRequest request) {
        ItemResponse created = itemService.createItem(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.id())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PutMapping("/{id}")
    public ItemResponse updateItem(@PathVariable Long id, @Valid @RequestBody UpdateItemRequest request) {
        return itemService.updateItem(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteItem(@PathVariable Long id) {
        itemService.deleteItem(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/stock")
    public ResponseEntity<ItemResponse> adjustStock(@PathVariable Long id,
                                                    @Valid @RequestBody AdjustStockRequest request) {
        return ResponseEntity.ok(itemService.adjustStock(id, request));
    }

    @GetMapping("/{id}/variants")
    public List<VariantResponse> listVariants(@PathVariable Long id) {
        return variantService.listVariantsOfItem(id);
    }

    @PostMapping("/{id}/variants")
    public ResponseEntity<VariantResponse> addVariant(@PathVariable Long id,
                                                      @Valid @RequestBody CreateVariantRequest request) {
        VariantResponse created = variantService.addVariant(id, request);
        URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/variants/{id}")
                .buildAndExpand(created.id())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }
}
