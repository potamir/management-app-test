package com.warehouse.inventory.item;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface VariantRepository extends JpaRepository<Variant, Long> {

    Optional<Variant> findBySku(String sku);

    boolean existsBySku(String sku);

    List<Variant> findByItemId(Long itemId);

    /**
     * Atomically decrements the variant's stock, but only if enough is available.
     * Returns 0 when the row was not updated (insufficient stock), which is how
     * overselling is prevented even under concurrent requests.
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update Variant v
               set v.stockQuantity = v.stockQuantity - :quantity
             where v.id = :id
               and v.stockQuantity >= :quantity
            """)
    int decrementStock(@Param("id") Long id, @Param("quantity") int quantity);

    /**
     * Atomically applies a stock delta, but never lets the result go negative.
     * Returns 0 when the guard rejected the update.
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update Variant v
               set v.stockQuantity = v.stockQuantity + :delta
             where v.id = :id
               and v.stockQuantity + :delta >= 0
            """)
    int adjustStock(@Param("id") Long id, @Param("delta") int delta);
}
