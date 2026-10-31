package com.warehouse.inventory.item;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ItemRepository extends JpaRepository<Item, Long> {

    /**
     * Atomically decrements the item's stock, but only if enough is available.
     * Returns 0 when the row was not updated (insufficient stock), which is how
     * overselling is prevented even under concurrent requests.
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update Item i
               set i.stockQuantity = i.stockQuantity - :quantity
             where i.id = :id
               and i.stockQuantity >= :quantity
            """)
    int decrementStock(@Param("id") Long id, @Param("quantity") int quantity);

    /**
     * Atomically applies a stock delta, but never lets the result go negative.
     * Returns 0 when the guard rejected the update.
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update Item i
               set i.stockQuantity = i.stockQuantity + :delta
             where i.id = :id
               and i.stockQuantity + :delta >= 0
            """)
    int adjustStock(@Param("id") Long id, @Param("delta") int delta);
}
