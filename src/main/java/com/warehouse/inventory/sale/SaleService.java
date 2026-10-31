package com.warehouse.inventory.sale;

import com.warehouse.inventory.common.exception.InsufficientStockException;
import com.warehouse.inventory.common.exception.ResourceNotFoundException;
import com.warehouse.inventory.item.Item;
import com.warehouse.inventory.item.ItemRepository;
import com.warehouse.inventory.item.Variant;
import com.warehouse.inventory.item.VariantRepository;
import com.warehouse.inventory.sale.dto.CreateSaleRequest;
import com.warehouse.inventory.sale.dto.SaleResponse;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Records sales while guaranteeing stock can never go negative.
 *
 * <p>Stock is decremented with a single conditional UPDATE
 * ({@code set stock = stock - :q where stock >= :q}) so the check and the write
 * are one atomic database operation. Two concurrent sales of the last unit can
 * never both succeed: exactly one UPDATE matches the row.</p>
 */
@Service
@Transactional(readOnly = true)
public class SaleService {

    private final ItemRepository itemRepository;
    private final VariantRepository variantRepository;
    private final SaleRepository saleRepository;

    public SaleService(ItemRepository itemRepository,
                       VariantRepository variantRepository,
                       SaleRepository saleRepository) {
        this.itemRepository = itemRepository;
        this.variantRepository = variantRepository;
        this.saleRepository = saleRepository;
    }

    public List<SaleResponse> listSales(Long itemId) {
        List<Sale> sales = itemId != null
                ? saleRepository.findByItemIdOrderByCreatedAtDesc(itemId)
                : saleRepository.findAll().stream()
                        .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                        .toList();
        return sales.stream().map(sale -> SaleResponse.from(sale, 0)).toList();
    }

    @Transactional
    public SaleResponse sell(CreateSaleRequest request) {
        Item item = itemRepository.findById(request.itemId())
                .orElseThrow(() -> new ResourceNotFoundException("Item", request.itemId()));
        List<Variant> variants = item.getVariants();

        if (variants.isEmpty()) {
            return sellItem(item, request.quantity());
        }
        return sellVariant(item, variants, request.variantId(), request.quantity());
    }

    private SaleResponse sellItem(Item item, int quantity) {
        int updated = itemRepository.decrementStock(item.getId(), quantity);
        if (updated == 0) {
            int available = itemRepository.findById(item.getId()).map(Item::getStockQuantity).orElse(0);
            throw new InsufficientStockException("item '%s'".formatted(item.getName()), quantity, available);
        }
        Sale sale = new Sale(item.getId(), null, item.getName(), null, quantity, item.getPrice());
        saleRepository.save(sale);
        int remaining = itemRepository.findById(item.getId()).map(Item::getStockQuantity).orElse(0);
        return SaleResponse.from(sale, remaining);
    }

    private SaleResponse sellVariant(Item item, List<Variant> variants, Long variantId, int quantity) {
        if (variantId == null) {
            throw new IllegalArgumentException(
                    "Item '%s' has variants; a variantId must be provided to sell it".formatted(item.getName()));
        }
        Variant variant = variants.stream()
                .filter(v -> v.getId().equals(variantId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Variant with id '%d' does not belong to item '%d'".formatted(variantId, item.getId())));

        int updated = variantRepository.decrementStock(variant.getId(), quantity);
        if (updated == 0) {
            int available = variantRepository.findById(variant.getId()).map(Variant::getStockQuantity).orElse(0);
            throw new InsufficientStockException(
                    "variant '%s' (SKU %s)".formatted(variant.getName(), variant.getSku()), quantity, available);
        }
        Sale sale = new Sale(item.getId(), variant.getId(), item.getName(), variant.getName(),
                quantity, variant.effectivePrice());
        saleRepository.save(sale);
        int remaining = variantRepository.findById(variant.getId()).map(Variant::getStockQuantity).orElse(0);
        return SaleResponse.from(sale, remaining);
    }
}
