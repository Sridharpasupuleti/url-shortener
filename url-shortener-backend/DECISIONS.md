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

### 6. Short-Code Uniqueness
**Decision**: Enforce short-code uniqueness with a named database constraint and translate concurrent alias conflicts into the existing client conflict exception.
**Rationale**: An application-level existence check alone cannot prevent two simultaneous requests from claiming the same alias.

### 7. URL-Creation Rate Limiting
**Decision**: Allow at most 10 `POST /urlshortener/posturl` requests per observed client IP in a Redis-backed 60-second fixed window. Return HTTP 429 with a JSON error after the limit; leave other endpoints unrestricted.
**Rationale**: Redis provides shared, atomic counters across application instances. A Lua script combines increment and initial expiry so concurrent requests cannot create inconsistent counter windows. The IP is SHA-256 hashed before it is included in the Redis key, and client-supplied forwarding headers are not trusted.

### 8. Public Short URL Base
**Decision**: Build generated short URLs from `app.public-base-url`, configurable with `APP_PUBLIC_BASE_URL`, with a localhost default for development.
**Rationale**: The URL returned to clients must point to the load balancer or public hostname rather than the particular application instance that handled the request.

## Trade-offs
- **ID-based Shortening**: By saving the entity first to get an ID and then encoding it, we perform two database writes for every generated short code. This was chosen for simplicity over pre-generating codes or using UUIDs (which are too long).
- **Synchronous Validation**: URL reachability is checked synchronously during the POST request, which increases response time but ensures only valid URLs are stored.
- **Fixed-window limiting**: The 60-second window starts with the first request for each IP, rather than aligning to wall-clock minutes. Clients behind the same observed proxy/NAT address share one limit; deployments behind trusted proxies should configure the server to resolve client addresses safely before relying on per-client limits.
