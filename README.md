# Warehouse Inventory Management API

A RESTful API for a shop's warehouse inventory, built with Spring Boot 3 and Java 17.
It manages items and their variants, pricing, stock levels, and sales — with a hard
guarantee that **out-of-stock products can never be sold**, even under concurrent requests.

## Tech Stack

| Concern        | Choice                                             |
|----------------|----------------------------------------------------|
| Language       | Java 17                                            |
| Framework      | Spring Boot 3.5.x (web, data-jpa, validation)      |
| Database       | H2 in-memory (JPA/Hibernate, schema auto-generated)|
| API docs       | springdoc-openapi (Swagger UI + OpenAPI JSON)      |
| Build          | Maven (wrapper included — no local Maven needed)   |
| Tests          | JUnit 5, AssertJ, TestRestTemplate                 |

## How to Run

Prerequisites: **Java 17+** only (Maven is not required — the wrapper downloads it).

```bash
./mvnw spring-boot:run
```

The API is then available at `http://localhost:8080`. Useful links:

- Swagger UI: http://localhost:8080/swagger-ui/index.html
- OpenAPI JSON: http://localhost:8080/v3/api-docs
- H2 console: http://localhost:8080/h2-console (JDBC URL `jdbc:h2:mem:warehousedb`, user `sa`, empty password)

Run the tests:

```bash
./mvnw test
```

The database starts pre-loaded with a small sample catalogue (a t-shirt with four
size/color variants, a tote bag, an enamel mug) so every endpoint is immediately usable.

## API Overview

| Method | Endpoint                       | Description                                            |
|--------|--------------------------------|--------------------------------------------------------|
| GET    | `/api/items?inStock=true`      | List items (optionally filter by availability)         |
| POST   | `/api/items`                   | Create an item, optionally with its initial variants   |
| GET    | `/api/items/{id}`              | Get item details (includes variants and total stock)   |
| PUT    | `/api/items/{id}`              | Update name / description / price                     |
| DELETE | `/api/items/{id}`              | Delete item and its variants                          |
| POST   | `/api/items/{id}/stock`        | Adjust item stock by a delta (restock / correction)   |
| GET    | `/api/items/{id}/variants`     | List an item's variants                               |
| POST   | `/api/items/{id}/variants`     | Add a variant to an item                              |
| GET    | `/api/variants/{id}`           | Get variant details                                   |
| PUT    | `/api/variants/{id}`           | Update variant name / SKU / price                     |
| DELETE | `/api/variants/{id}`           | Remove a variant                                      |
| POST   | `/api/variants/{id}/stock`     | Adjust variant stock by a delta                       |
| POST   | `/api/sales`                   | Record a sale (decrements stock atomically)           |
| GET    | `/api/sales?itemId={id}`       | Sale history (optionally per item)                    |

### Example Calls

Create an item with variants (a variant's `price` and `stockQuantity` are optional;
price falls back to the item's price, stock defaults to 0):

```bash
curl -X POST http://localhost:8080/api/items \
  -H 'Content-Type: application/json' \
  -d '{
        "name": "Classic Cotton T-Shirt",
        "description": "Soft 100% organic cotton tee",
        "price": 19.99,
        "variants": [
          {"name": "Small / Black",  "sku": "TSHIRT-BLK-S", "stockQuantity": 15},
          {"name": "Medium / White", "sku": "TSHIRT-WHT-M", "price": 21.50, "stockQuantity": 5}
        ]
      }'
```

Restock and correct stock with a signed delta (result may never be negative):

```bash
curl -X POST http://localhost:8080/api/variants/1/stock \
  -H 'Content-Type: application/json' -d '{"delta": 10}'
```

Sell — `variantId` is required when the item has variants, and must be omitted otherwise:

```bash
# sell a variant
curl -X POST http://localhost:8080/api/sales \
  -H 'Content-Type: application/json' \
  -d '{"itemId": 1, "variantId": 2, "quantity": 3}'

# sell an item without variants
curl -X POST http://localhost:8080/api/sales \
  -H 'Content-Type: application/json' \
  -d '{"itemId": 3, "quantity": 1}'
```

Successful response (201):

```json
{
  "id": 1,
  "itemId": 1,
  "variantId": 2,
  "itemName": "Classic Cotton T-Shirt",
  "variantName": "Medium / Black",
  "quantity": 3,
  "unitPrice": 19.99,
  "totalAmount": 59.97,
  "createdAt": "2026-10-31T06:36:24.226Z",
  "remainingStock": 19
}
```

Selling more than is available fails with `409 Conflict` and leaves stock untouched:

```json
{
  "title": "Insufficient stock",
  "status": 409,
  "detail": "Insufficient stock for item 'Enamel Camp Mug': requested 99, available 3",
  "errorCode": "INSUFFICIENT_STOCK",
  "timestamp": "2026-10-31T06:36:24.249Z"
}
```

All errors follow the same RFC 7807 `application/problem+json` shape.

## Design Decisions

**Architecture.** Classic layered architecture (`controller → service → repository`)
organized by feature (`item`, `sale`) rather than by technical role. For a domain of
this size this keeps each feature self-contained and readable; the service layer holds
all business rules and transactions, controllers stay thin, and repositories are only
persistence. Entities never cross the API boundary — DTOs are Java `records`.

**Stock model.** Variants are optional. An item *without* variants tracks stock directly
on the item; an item *with* variants tracks stock **only** on its variants (the item's
available stock is then computed as the sum of its variants). There is deliberately no
denormalized "total stock" column — maintaining one would invite consistency bugs, and
the catalogue sizes involved here make summing cheap. Item-level stock adjustments are
rejected (`422`) once variants exist, so there is exactly one source of truth per item.

**No overselling — atomic conditional updates.** The check-and-decrement is a single
conditional SQL `UPDATE ... SET stock = stock - :q WHERE id = :id AND stock >= :q`.
If zero rows are affected, the sale fails with `409`. Because the guard and the write
are one database operation, two concurrent purchases of the last unit can never both
succeed — no read-check-write race window exists. This is verified by a test that fires
25 concurrent purchase attempts at a stock of 10 and expects exactly 10 to succeed.
Stock adjustments use the same pattern (`stock + :delta >= 0`) so stock can never go
negative through any path.

**Pricing.** Prices are `BigDecimal` (never floating point for money). A variant's price
is optional and falls back to the item's price — a common retail pattern ("Medium is
$19.99, XXL is a premium $24.99") that avoids repeating the same price on every variant.
The effective price at the time of sale is snapshotted onto the sale record.

**Sales as immutable history.** A `Sale` stores ids plus name/price snapshots rather
than JPA relations, so history remains meaningful after an item is renamed, repriced,
or deleted. Sales cannot be modified or deleted through the API.

**SKUs.** Uniqueness is enforced globally at the database level (unique constraint) and
validated up front with a friendly `409` for duplicate SKUs within a request or across
the catalogue.

**Error handling.** A single `@RestControllerAdvice` maps every error to RFC 7807
problem+json with a consistent title/detail/timestamp shape, including bean-validation
field errors (`400` with a map of field → message), insufficient stock (`409` with
`errorCode: INSUFFICIENT_STOCK`), and invalid stock operations (`422`). It extends
`ResponseEntityExceptionHandler` so framework errors keep their correct status codes.

**API shape.** Nested resources where ownership matters (`/api/items/{id}/variants`),
flat paths where a variant is addressed on its own (`/api/variants/{id}/stock`).
`POST /api/items/{id}/stock` with a signed delta covers restocking, corrections, and
shrinkage in one operation. `201` responses carry a `Location` header.

**No Lombok.** The project is deliberately dependency-light; with only three entities,
explicit constructors/accessors read fine and the project builds anywhere without
annotation processing.

**H2 in-memory + seeder.** Chosen for zero-setup reviewability (the task allows
in-memory). The JPA mapping is standard, so switching to PostgreSQL/MySQL is a
driver + URL change; a `SampleDataLoader` seeds a small catalogue only when the
database is empty.

## Assumptions

- Single shop, single warehouse; no auth/multi-tenancy (out of scope).
- Catalogue sizes are small, so no pagination yet (a natural next step).
- Currency is a single implicit currency; prices are stored as decimal amounts.
- Prices are exclusive of tax and sales record no customer data — this API is the
  inventory/fulfilment side of a shop, not its checkout.
- Deleting an item deletes its variants; historical sales survive via snapshots.
- A sale is final (no refunds/returns flow), and sales cannot be edited after creation.
- Zero-delta stock adjustments are rejected as no-ops that likely indicate client bugs.

## Possible Next Steps

Pagination + sorting on lists, Flyway schema migrations, optimistic-lock `version`
columns, low-stock threshold notifications, an authentication layer (e.g. JWT with
admin/operator roles), request-level idempotency keys for sales, and a Dockerfile.
