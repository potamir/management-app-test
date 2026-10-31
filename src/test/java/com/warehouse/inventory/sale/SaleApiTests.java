package com.warehouse.inventory.sale;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SaleApiTests {

    @Autowired
    private TestRestTemplate rest;

    private final HttpHeaders jsonHeaders = jsonHeaders();

    private static HttpHeaders jsonHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    private ResponseEntity<JsonNode> post(String path, Object body) {
        return rest.postForEntity(path, new HttpEntity<>(body, jsonHeaders), JsonNode.class);
    }

    private JsonNode createItem(String name, String price, int stock) {
        Map<String, Object> body = new HashMap<>();
        body.put("name", name);
        body.put("price", new BigDecimal(price));
        body.put("stockQuantity", stock);
        return post("/api/items", body).getBody();
    }

    private JsonNode createItemWithVariant(String name, String price, String sku, int variantStock) {
        Map<String, Object> variant = new HashMap<>();
        variant.put("name", "One Size");
        variant.put("sku", sku);
        variant.put("stockQuantity", variantStock);
        Map<String, Object> body = new HashMap<>();
        body.put("name", name);
        body.put("price", new BigDecimal(price));
        body.put("stockQuantity", 0);
        body.put("variants", List.of(variant));
        return post("/api/items", body).getBody();
    }

    private Map<String, Object> sale(long itemId, Long variantId, int quantity) {
        Map<String, Object> body = new HashMap<>();
        body.put("itemId", itemId);
        body.put("quantity", quantity);
        if (variantId != null) {
            body.put("variantId", variantId);
        }
        return body;
    }

    @Test
    void sell_itemWithoutVariants_decrementsStockAndRecordsSale() {
        JsonNode item = createItem("Sticker pack", "4.50", 10);

        ResponseEntity<JsonNode> response = post("/api/sales", sale(item.get("id").asLong(), null, 3));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        JsonNode sale = response.getBody();
        assertThat(sale.get("unitPrice").decimalValue()).isEqualByComparingTo("4.50");
        assertThat(sale.get("totalAmount").decimalValue()).isEqualByComparingTo("13.50");
        assertThat(sale.get("remainingStock").asInt()).isEqualTo(7);

        ResponseEntity<JsonNode> after = rest.getForEntity("/api/items/" + item.get("id").asLong(), JsonNode.class);
        assertThat(after.getBody().get("stockQuantity").asInt()).isEqualTo(7);
    }

    @Test
    void sell_variant_fallsBackToItemPriceWhenVariantHasNoPrice() {
        JsonNode item = createItemWithVariant("Poster", "15.00", "POSTER-A1", 5);
        long variantId = item.get("variants").get(0).get("id").asLong();

        ResponseEntity<JsonNode> response = post("/api/sales", sale(item.get("id").asLong(), variantId, 2));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().get("unitPrice").decimalValue()).isEqualByComparingTo("15.00");
        assertThat(response.getBody().get("remainingStock").asInt()).isEqualTo(3);
    }

    @Test
    void sell_variant_usesOwnPriceWhenSet() {
        Map<String, Object> variant = new HashMap<>();
        variant.put("name", "XXL");
        variant.put("sku", "SHIRT-XXL-PREMIUM");
        variant.put("price", new BigDecimal("29.99"));
        variant.put("stockQuantity", 5);
        Map<String, Object> body = new HashMap<>();
        body.put("name", "Premium tee");
        body.put("price", new BigDecimal("19.99"));
        body.put("variants", List.of(variant));
        JsonNode item = post("/api/items", body).getBody();
        long variantId = item.get("variants").get(0).get("id").asLong();

        ResponseEntity<JsonNode> response = post("/api/sales", sale(item.get("id").asLong(), variantId, 1));

        assertThat(response.getBody().get("unitPrice").decimalValue()).isEqualByComparingTo("29.99");
    }

    @Test
    void sell_insufficientStock_returns409AndLeavesStockUntouched() {
        JsonNode item = createItem("Rare pin", "50.00", 2);

        ResponseEntity<JsonNode> response = post("/api/sales", sale(item.get("id").asLong(), null, 3));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().get("title").asText()).isEqualTo("Insufficient stock");
        assertThat(response.getBody().get("errorCode").asText()).isEqualTo("INSUFFICIENT_STOCK");

        JsonNode after = rest.getForEntity("/api/items/" + item.get("id").asLong(), JsonNode.class).getBody();
        assertThat(after.get("stockQuantity").asInt()).isEqualTo(2);
    }

    @Test
    void sell_itemWithVariants_withoutVariantId_returns400() {
        JsonNode item = createItemWithVariant("Socks", "6.00", "SOCKS-FREE", 10);

        ResponseEntity<JsonNode> response = post("/api/sales", sale(item.get("id").asLong(), null, 1));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().get("detail").asText()).contains("variantId");
    }

    @Test
    void sell_variantOfDifferentItem_returns400() {
        JsonNode itemA = createItemWithVariant("Cap A", "8.00", "CAP-A-STD", 5);
        JsonNode itemB = createItemWithVariant("Cap B", "8.00", "CAP-B-STD", 5);
        long variantOfB = itemB.get("variants").get(0).get("id").asLong();

        ResponseEntity<JsonNode> response = post("/api/sales", sale(itemA.get("id").asLong(), variantOfB, 1));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void sell_unknownItem_returns404() {
        ResponseEntity<JsonNode> response = post("/api/sales", sale(999999L, null, 1));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void listSales_returnsRecordedSales() {
        JsonNode item = createItem("Keychain", "2.00", 9);
        long itemId = item.get("id").asLong();
        post("/api/sales", sale(itemId, null, 4));

        ResponseEntity<List<Map<String, Object>>> response = rest.exchange(
                "/api/sales?itemId=" + itemId,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<>() {
                });

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
        assertThat(response.getBody().get(0).get("itemName")).isEqualTo("Keychain");
    }

    @Test
    void concurrentSales_neverOversell() throws Exception {
        JsonNode item = createItem("Flash sale item", "9.99", 10);
        long itemId = item.get("id").asLong();

        int buyers = 25;
        ExecutorService pool = Executors.newFixedThreadPool(buyers);
        try {
            List<Callable<Integer>> purchases = java.util.stream.IntStream.range(0, buyers)
                    .mapToObj(i -> (Callable<Integer>) () -> post("/api/sales", sale(itemId, null, 1))
                            .getStatusCode()
                            .value())
                    .toList();
            List<Future<Integer>> results = pool.invokeAll(purchases);
            int created = 0;
            for (Future<Integer> result : results) {
                if (result.get() == 201) {
                    created++;
                }
            }

            assertThat(created).isEqualTo(10);

            JsonNode after = rest.getForEntity("/api/items/" + itemId, JsonNode.class).getBody();
            assertThat(after.get("stockQuantity").asInt()).isZero();
            assertThat(after.get("inStock").asBoolean()).isFalse();
        } finally {
            pool.shutdown();
            assertThat(pool.awaitTermination(10, TimeUnit.SECONDS)).isTrue();
        }
    }
}
