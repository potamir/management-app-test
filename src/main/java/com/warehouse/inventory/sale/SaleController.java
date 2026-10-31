package com.warehouse.inventory.sale;

import com.warehouse.inventory.sale.dto.CreateSaleRequest;
import com.warehouse.inventory.sale.dto.SaleResponse;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/sales")
public class SaleController {

    private final SaleService saleService;

    public SaleController(SaleService saleService) {
        this.saleService = saleService;
    }

    @GetMapping
    public List<SaleResponse> listSales(@RequestParam(required = false) Long itemId) {
        return saleService.listSales(itemId);
    }

    /**
     * Records a sale; responds 409 Conflict when there is not enough stock,
     * leaving the inventory untouched.
     */
    @PostMapping
    public ResponseEntity<SaleResponse> sell(@Valid @RequestBody CreateSaleRequest request) {
        SaleResponse sale = saleService.sell(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(sale.id())
                .toUri();
        return ResponseEntity.created(location).body(sale);
    }
}
