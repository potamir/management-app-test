package com.warehouse.inventory.common;

import com.warehouse.inventory.item.Item;
import com.warehouse.inventory.item.ItemRepository;
import com.warehouse.inventory.item.Variant;
import java.math.BigDecimal;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * Seeds a small, realistic catalogue so the API is useful the moment it starts.
 * Only runs when the database is empty.
 */
@Component
public class SampleDataLoader implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(SampleDataLoader.class);

    private final ItemRepository itemRepository;

    public SampleDataLoader(ItemRepository itemRepository) {
        this.itemRepository = itemRepository;
    }

    @Override
    public void run(String... args) {
        if (itemRepository.count() > 0) {
            return;
        }

        Item tShirt = new Item("Classic Cotton T-Shirt", "Soft 100% organic cotton tee", new BigDecimal("19.99"), 0);
        tShirt.addVariant(new Variant("Small / Black", "TSHIRT-BLK-S", null, 15));
        tShirt.addVariant(new Variant("Medium / Black", "TSHIRT-BLK-M", null, 22));
        tShirt.addVariant(new Variant("Large / Black", "TSHIRT-BLK-L", null, 8));
        tShirt.addVariant(new Variant("Medium / White", "TSHIRT-WHT-M", new BigDecimal("21.50"), 5));
        itemRepository.save(tShirt);

        Item toteBag = new Item("Canvas Tote Bag", "Heavy-duty everyday tote", new BigDecimal("14.50"), 25);
        itemRepository.save(toteBag);

        Item mug = new Item("Enamel Camp Mug", "300ml enamel mug, stackable", new BigDecimal("8.75"), 3);
        itemRepository.save(mug);

        log.info("Seeded sample catalogue: {} items (t-shirt with 4 variants, tote bag, enamel mug)",
                itemRepository.count());
    }
}
