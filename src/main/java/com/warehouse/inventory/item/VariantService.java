package com.warehouse.inventory.item;

import com.warehouse.inventory.common.exception.DuplicateSkuException;
import com.warehouse.inventory.common.exception.InvalidStockOperationException;
import com.warehouse.inventory.common.exception.ResourceNotFoundException;
import com.warehouse.inventory.item.dto.AdjustStockRequest;
import com.warehouse.inventory.item.dto.CreateVariantRequest;
import com.warehouse.inventory.item.dto.UpdateVariantRequest;
import com.warehouse.inventory.item.dto.VariantResponse;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class VariantService {

    private static final String RESOURCE = "Variant";

    private final ItemRepository itemRepository;
    private final VariantRepository variantRepository;

    public VariantService(ItemRepository itemRepository, VariantRepository variantRepository) {
        this.itemRepository = itemRepository;
        this.variantRepository = variantRepository;
    }

    public List<VariantResponse> listVariantsOfItem(Long itemId) {
        findItem(itemId);
        return variantRepository.findByItemId(itemId).stream()
                .map(VariantResponse::from)
                .toList();
    }

    public VariantResponse getVariant(Long id) {
        return VariantResponse.from(findVariant(id));
    }

    @Transactional
    public VariantResponse addVariant(Long itemId, CreateVariantRequest request) {
        Item item = findItem(itemId);
        if (variantRepository.existsBySku(request.sku())) {
            throw new DuplicateSkuException(request.sku());
        }
        Variant variant = new Variant(
                request.name(),
                request.sku(),
                request.price(),
                request.stockQuantity() != null ? request.stockQuantity() : 0);
        item.addVariant(variant);
        // Persist the variant directly so its generated id is available immediately;
        // relying on cascading merge from the parent item would leave it null here.
        variantRepository.save(variant);
        return VariantResponse.from(variant);
    }

    @Transactional
    public VariantResponse updateVariant(Long id, UpdateVariantRequest request) {
        Variant variant = findVariant(id);
        if (!variant.getSku().equals(request.sku()) && variantRepository.existsBySku(request.sku())) {
            throw new DuplicateSkuException(request.sku());
        }
        variant.setName(request.name());
        variant.setSku(request.sku());
        variant.setPrice(request.price());
        return VariantResponse.from(variant);
    }

    @Transactional
    public void deleteVariant(Long id) {
        variantRepository.delete(findVariant(id));
    }

    /**
     * Adjusts the stock of a variant (restock, corrections, shrinkage).
     * The resulting stock may never be negative.
     */
    @Transactional
    public VariantResponse adjustStock(Long id, AdjustStockRequest request) {
        Variant variant = findVariant(id);
        int delta = request.delta();
        if (delta == 0) {
            throw new InvalidStockOperationException("Stock delta must not be zero");
        }
        int updated = variantRepository.adjustStock(id, delta);
        if (updated == 0) {
            throw new InvalidStockOperationException(
                    "Stock adjustment of %+d would make stock of variant '%s' (SKU %s) negative (current: %d)"
                            .formatted(delta, variant.getName(), variant.getSku(), variant.getStockQuantity()));
        }
        return VariantResponse.from(findVariant(id));
    }

    private Item findItem(Long id) {
        return itemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Item", id));
    }

    private Variant findVariant(Long id) {
        return variantRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(RESOURCE, id));
    }
}
