package com.warehouse.inventory.sale;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * An immutable record of a sale.
 *
 * <p>Sales intentionally store ids and name/price snapshots instead of JPA
 * relations: a sale is a historical fact and must keep its meaning even if the
 * item or variant is renamed, repriced or deleted later.</p>
 */
@Entity
@Table(name = "sales")
public class Sale {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "item_id", nullable = false)
    private Long itemId;

    @Column(name = "variant_id")
    private Long variantId;

    @Column(name = "item_name", nullable = false, length = 120)
    private String itemName;

    @Column(name = "variant_name", length = 120)
    private String variantName;

    @Column(nullable = false)
    private int quantity;

    @Column(name = "unit_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "total_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected Sale() {
        // JPA
    }

    public Sale(Long itemId, Long variantId, String itemName, String variantName,
                int quantity, BigDecimal unitPrice) {
        this.itemId = itemId;
        this.variantId = variantId;
        this.itemName = itemName;
        this.variantName = variantName;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.totalAmount = unitPrice.multiply(BigDecimal.valueOf(quantity));
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public Long getItemId() {
        return itemId;
    }

    public Long getVariantId() {
        return variantId;
    }

    public String getItemName() {
        return itemName;
    }

    public String getVariantName() {
        return variantName;
    }

    public int getQuantity() {
        return quantity;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
