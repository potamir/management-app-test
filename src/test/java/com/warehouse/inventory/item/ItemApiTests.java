package com.warehouse.inventory.item;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ItemApiTests {

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

    private ResponseEntity<JsonNode> put(String path, Object body) {
        return rest.exchange(path, HttpMethod.PUT, new HttpEntity<>(body, jsonHeaders), JsonNode.class);
    }

    private ResponseEntity<JsonNode> get(String path) {
        return rest.getForEntity(path, JsonNode.class);
    }

    private Map<String, Object> newItem(String name, String price, Integer stock) {
        Map<String, Object> body = new HashMap<>();
        body.put("name", name);
        body.put("price", new BigDecimal(price));
        if (stock != null) {
            body.put("stockQuantity", stock);
        }
        return body;
    }

    @Test
    void createItem_returns201WithLocationAndBody() {
        Map<String, Object> body = newItem("Test Hoodie", "39.90", 10);

        ResponseEntity<JsonNode> response = post("/api/items", body);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getHeaders().getLocation()).isNotNull();
        JsonNode item = response.getBody();
        assertThat(item.get("id").asLong()).isPositive();
        assertThat(item.get("name").asText()).isEqualTo("Test Hoodie");
        assertThat(item.get("price").decimalValue()).isEqualByComparingTo("39.90");
        assertThat(item.get("stockQuantity").asInt()).isEqualTo(10);
        assertThat(item.get("inStock").asBoolean()).isTrue();
    }

    @Test
    void createItem_withVariants_persistsVariants() {
        Map<String, Object> small = new HashMap<>();
        small.put("name", "Small");
        small.put("sku", "HOODIE-S");
        small.put("stockQuantity", 4);
        Map<String, Object> large = new HashMap<>();
        large.put("name", "Large");
        large.put("sku", "HOODIE-L");
        large.put("price", new BigDecimal("44.90"));
        large.put("stockQuantity", 2);

        Map<String, Object> body = newItem("Zip Hoodie", "39.90", 0);
        body.put("variants", List.of(small, large));

        ResponseEntity<JsonNode> response = post("/api/items", body);
        JsonNode item = response.getBody();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(item.get("variants")).hasSize(2);
        // variant without price falls back to the item price
        assertThat(item.get("variants").get(0).get("price").decimalValue()).isEqualByComparingTo("39.90");
        // variant with its own price uses it
        assertThat(item.get("variants").get(1).get("price").decimalValue()).isEqualByComparingTo("44.90");
        // item stock is the sum of variant stock
        assertThat(item.get("stockQuantity").asInt()).isEqualTo(6);
    }

    @Test
    void createItem_blankName_returns400WithFieldErrors() {
        Map<String, Object> body = newItem("", "10.00", 1);

        ResponseEntity<JsonNode> response = post("/api/items", body);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().get("title").asText()).isEqualTo("Validation failed");
        assertThat(response.getBody().get("errors").get("name").asText()).contains("required");
    }

    @Test
    void createItem_negativePrice_returns400() {
        Map<String, Object> body = newItem("Bad item", "-1.00", 1);

        ResponseEntity<JsonNode> response = post("/api/items", body);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void createItem_duplicateSku_returns409() {
        Map<String, Object> variant = new HashMap<>();
        variant.put("name", "One Size");
        variant.put("sku", "DUPLICATE-SKU-1");
        variant.put("stockQuantity", 1);
        Map<String, Object> body = newItem("First cap", "9.00", 0);
        body.put("variants", List.of(variant));
        post("/api/items", body);

        Map<String, Object> variantAgain = new HashMap<>();
        variantAgain.put("name", "One Size");
        variantAgain.put("sku", "DUPLICATE-SKU-1");
        variantAgain.put("stockQuantity", 1);
        Map<String, Object> secondBody = newItem("Second cap", "9.00", 0);
        secondBody.put("variants", List.of(variantAgain));

        ResponseEntity<JsonNode> response = post("/api/items", secondBody);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().get("detail").asText()).contains("DUPLICATE-SKU-1");
    }

    @Test
    void getItem_unknownId_returns404ProblemDetail() {
        ResponseEntity<JsonNode> response = get("/api/items/999999");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().get("title").asText()).isEqualTo("Resource not found");
    }

    @Test
    void updateItem_changesFieldsButKeepsVariants() {
        Map<String, Object> variant = new HashMap<>();
        variant.put("name", "Standard");
        variant.put("sku", "UPDATABLE-1");
        variant.put("stockQuantity", 7);
        Map<String, Object> create = newItem("Old name", "5.00", 0);
        create.put("variants", List.of(variant));
        long id = post("/api/items", create).getBody().get("id").asLong();

        Map<String, Object> update = new HashMap<>();
        update.put("name", "New name");
        update.put("description", "Updated");
        update.put("price", new BigDecimal("6.00"));

        ResponseEntity<JsonNode> response = put("/api/items/" + id, update);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode item = response.getBody();
        assertThat(item.get("name").asText()).isEqualTo("New name");
        assertThat(item.get("description").asText()).isEqualTo("Updated");
        assertThat(item.get("price").decimalValue()).isEqualByComparingTo("6.00");
        assertThat(item.get("variants")).hasSize(1);
    }

    @Test
    void deleteItem_returns204Then404() {
        long id = post("/api/items", newItem("Doomed", "1.00", 1)).getBody().get("id").asLong();

        ResponseEntity<Void> delete = rest.exchange("/api/items/" + id, HttpMethod.DELETE, null, Void.class);
        ResponseEntity<JsonNode> after = get("/api/items/" + id);

        assertThat(delete.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(after.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void listItems_filtersByInStock() {
        post("/api/items", newItem("In stock item", "2.00", 5));
        post("/api/items", newItem("Out of stock item", "2.00", 0));

        ResponseEntity<JsonNode> all = get("/api/items");
        ResponseEntity<JsonNode> inStock = get("/api/items?inStock=true");
        ResponseEntity<JsonNode> outOfStock = get("/api/items?inStock=false");

        assertThat(all.getBody().size()).isGreaterThanOrEqualTo(2);
        assertThat(inStock.getBody().toString()).contains("In stock item");
        assertThat(inStock.getBody().toString()).doesNotContain("Out of stock item");
        assertThat(outOfStock.getBody().toString()).contains("Out of stock item");
    }

    @Test
    void adjustItemStock_restocksAndDepletes() {
        long id = post("/api/items", newItem("Adjustable", "3.00", 2)).getBody().get("id").asLong();

        ResponseEntity<JsonNode> restock = post("/api/items/" + id + "/stock", Map.of("delta", 10));
        ResponseEntity<JsonNode> deplete = post("/api/items/" + id + "/stock", Map.of("delta", -12));

        assertThat(restock.getBody().get("stockQuantity").asInt()).isEqualTo(12);
        assertThat(deplete.getBody().get("stockQuantity").asInt()).isZero();
        assertThat(deplete.getBody().get("inStock").asBoolean()).isFalse();
    }

    @Test
    void adjustItemStock_resultWouldBeNegative_returns422() {
        long id = post("/api/items", newItem("Scarce", "3.00", 5)).getBody().get("id").asLong();

        ResponseEntity<JsonNode> response = post("/api/items/" + id + "/stock", Map.of("delta", -6));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(get("/api/items/" + id).getBody().get("stockQuantity").asInt()).isEqualTo(5);
    }

    @Test
    void adjustItemStock_itemWithVariants_isRejected() {
        Map<String, Object> variant = new HashMap<>();
        variant.put("name", "Standard");
        variant.put("sku", "VARIANT-REJECT-1");
        variant.put("stockQuantity", 3);
        Map<String, Object> body = newItem("Variant holder", "7.00", 0);
        body.put("variants", List.of(variant));
        long id = post("/api/items", body).getBody().get("id").asLong();

        ResponseEntity<JsonNode> response = post("/api/items/" + id + "/stock", Map.of("delta", 5));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(response.getBody().get("detail").asText()).contains("variants");
    }

    @Test
    void variantLifecycle_addListUpdateDeleteAdjustStock() {
        long itemId = post("/api/items", newItem("Beanie", "12.00", 0)).getBody().get("id").asLong();

        Map<String, Object> variant = new HashMap<>();
        variant.put("name", "Red");
        variant.put("sku", "BEANIE-RED");
        variant.put("stockQuantity", 5);
        ResponseEntity<JsonNode> created = post("/api/items/" + itemId + "/variants", variant);

        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        long variantId = created.getBody().get("id").asLong();

        assertThat(get("/api/items/" + itemId + "/variants").getBody()).hasSize(1);

        Map<String, Object> update = new HashMap<>();
        update.put("name", "Crimson");
        update.put("sku", "BEANIE-RED");
        update.put("price", new BigDecimal("13.00"));
        ResponseEntity<JsonNode> updated = put("/api/variants/" + variantId, update);

        assertThat(updated.getBody().get("name").asText()).isEqualTo("Crimson");

        ResponseEntity<JsonNode> adjusted = post("/api/variants/" + variantId + "/stock", Map.of("delta", -5));
        assertThat(adjusted.getBody().get("stockQuantity").asInt()).isZero();

        ResponseEntity<Void> deleted =
                rest.exchange("/api/variants/" + variantId, HttpMethod.DELETE, null, Void.class);
        assertThat(deleted.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(get("/api/items/" + itemId + "/variants").getBody()).isEmpty();
    }
}
