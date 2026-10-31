package com.warehouse.inventory.item;

import com.warehouse.inventory.common.exception.DuplicateSkuException;
import com.warehouse.inventory.common.exception.InvalidStockOperationException;
import com.warehouse.inventory.common.exception.ResourceNotFoundException;
import com.warehouse.inventory.item.dto.AdjustStockRequest;
import com.warehouse.inventory.item.dto.CreateItemRequest;
import com.warehouse.inventory.item.dto.CreateVariantRequest;
import com.warehouse.inventory.item.dto.ItemResponse;
import com.warehouse.inventory.item.dto.UpdateItemRequest;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ItemService {

    private static final String RESOURCE = "Item";

    private final ItemRepository itemRepository;
    private final VariantRepository variantRepository;

    public ItemService(ItemRepository itemRepository, VariantRepository variantRepository) {
        this.itemRepository = itemRepository;
        this.variantRepository = variantRepository;
    }

    public List<ItemResponse> listItems(Boolean inStock) {
        return itemRepository.findAll().stream()
                .map(ItemResponse::from)
                .filter(item -> inStock == null || item.inStock() == inStock)
                .toList();
    }

    public ItemResponse getItem(Long id) {
        return ItemResponse.from(findItem(id));
    }

    @Transactional
    public ItemResponse createItem(CreateItemRequest request) {
        validateVariantSkus(request.variants());

        Item item = new Item(
                request.name(),
                request.description(),
                request.price(),
                request.stockQuantity() != null ? request.stockQuantity() : 0);

        if (request.variants() != null) {
            for (CreateVariantRequest variantRequest : request.variants()) {
                item.addVariant(toVariant(variantRequest));
            }
        }
        return ItemResponse.from(itemRepository.save(item));
    }

    @Transactional
    public ItemResponse updateItem(Long id, UpdateItemRequest request) {
        Item item = findItem(id);
        item.setName(request.name());
        item.setDescription(request.description());
        item.setPrice(request.price());
        return ItemResponse.from(item);
    }

    @Transactional
    public void deleteItem(Long id) {
        Item item = findItem(id);
        itemRepository.delete(item);
    }

    /**
     * Adjusts the stock of an item without variants (restock, corrections, shrinkage).
     * Items with variants manage stock per variant instead.
     */
    @Transactional
    public ItemResponse adjustStock(Long id, AdjustStockRequest request) {
        Item item = findItem(id);
        if (!item.getVariants().isEmpty()) {
            throw new InvalidStockOperationException(
                    "Item '%s' has variants; adjust stock on a variant instead".formatted(item.getName()));
        }
        int delta = request.delta();
        if (delta == 0) {
            throw new InvalidStockOperationException("Stock delta must not be zero");
        }
        int updated = itemRepository.adjustStock(id, delta);
        if (updated == 0) {
            throw new InvalidStockOperationException(
                    "Stock adjustment of %+d would make stock of '%s' negative (current: %d)"
                            .formatted(delta, item.getName(), item.getStockQuantity()));
        }
        return ItemResponse.from(findItem(id));
    }

    private Item findItem(Long id) {
        return itemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(RESOURCE, id));
    }

    private Variant toVariant(CreateVariantRequest request) {
        return new Variant(
                request.name(),
                request.sku(),
                request.price(),
                request.stockQuantity() != null ? request.stockQuantity() : 0);
    }

    private void validateVariantSkus(List<CreateVariantRequest> variantRequests) {
        if (variantRequests == null || variantRequests.isEmpty()) {
            return;
        }
        Set<String> seen = new HashSet<>();
        for (CreateVariantRequest variantRequest : variantRequests) {
            String sku = variantRequest.sku();
            if (!seen.add(sku) || variantRepository.existsBySku(sku)) {
                throw new DuplicateSkuException(sku);
            }
        }
    }
}
