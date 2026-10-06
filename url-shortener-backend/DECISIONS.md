# Decisions

This document records the key architectural and design decisions made for the URL Shortener project.

## Design Decisions

### 1. Base62 Encoding
**Decision**: Use Base62 encoding for generated short codes.
**Rationale**: Base62 (0-9, A-Z, a-z) provides a compact, URL-friendly representation of the database auto-incremented ID. It avoids special characters and is more efficient than Base10.

### 2. Database Selection
**Decision**: Use MySQL for primary persistence.
**Rationale**: A relational database ensures ACID properties and provides efficient indexing on `shortCode` and `originalUrl`, which are critical for the primary lookups.

### 3. Caching Strategy
**Decision**: Use Redis for caching original URLs.
**Rationale**: Redirection is a read-heavy operation. Caching the `shortCode` $\rightarrow$ `originalUrl` mapping in Redis significantly reduces latency and database load.

### 4. URL Validation
**Decision**: Use `RestClient` to perform a `HEAD` request to verify URL reachability.
**Rationale**: Preventing the creation of short codes for broken or non-existent URLs improves the quality of the service.

### 5. Mapping Strategy
**Decision**: Use `ModelMapper` for Entity $\leftrightarrow$ DTO conversion.
**Rationale**: Reduces boilerplate code for mapping fields between layers.

## Trade-offs
- **ID-based Shortening**: By saving the entity first to get an ID and then encoding it, we perform two database writes for every generated short code. This was chosen for simplicity over pre-generating codes or using UUIDs (which are too long).
- **Synchronous Validation**: URL reachability is checked synchronously during the POST request, which increases response time but ensures only valid URLs are stored.
