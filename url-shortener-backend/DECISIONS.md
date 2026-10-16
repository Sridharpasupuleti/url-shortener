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

### 7. Endpoint Rate Limiting
**Decision**: Use separate Redis-backed 60-second fixed-window limits for URL creation (10 requests) and redirects (100 requests), configurable independently. Return HTTP 429 with `ErrorResponse` after a limit is exceeded; leave unrelated endpoints unrestricted. Fail closed with HTTP 503 when Redis cannot evaluate a rate check.
**Rationale**: A shared service and atomic Lua script provide counters across application instances without duplicate implementation. Separate namespaces prevent endpoint counters from interacting. The observed client IP is SHA-256 hashed before it is included in a Redis key. Forwarded headers are used only when the socket peer matches a configured trusted proxy IP or CIDR, and Spring's automatic forwarded-header processing is disabled.

### 8. Public Short URL Base
**Decision**: Build generated short URLs from `app.public-base-url`, configurable with `APP_PUBLIC_BASE_URL`, with a localhost default for development.
**Rationale**: The URL returned to clients must point to the load balancer or public hostname rather than the particular application instance that handled the request.

### 9. Nginx Load Balancing
**Decision**: Use Nginx as the only published entry point in the local Docker Compose deployment, balancing requests across Spring Boot replicas with least-connections and Docker DNS discovery. Reserve a stable private address for Nginx and trust only that address in the app.
**Rationale**: The application can scale horizontally while all replicas share MySQL and Redis. Nginx overwrites `X-Forwarded-For`; the app's explicit trusted-proxy resolver sees the connecting client's address without consuming arbitrary forwarding headers.

## Trade-offs
- **ID-based Shortening**: By saving the entity first to get an ID and then encoding it, we perform two database writes for every generated short code. This was chosen for simplicity over pre-generating codes or using UUIDs (which are too long).
- **Synchronous Validation**: URL reachability is checked synchronously during the POST request, which increases response time but ensures only valid URLs are stored.
- **Fixed-window limiting**: The 60-second window starts with the first request for each IP, rather than aligning to wall-clock minutes. Clients behind the same observed proxy/NAT address share one limit; deployments behind trusted proxies should configure the server to resolve client addresses safely before relying on per-client limits.
- **Redis availability**: Rate-limited endpoints return HTTP 503 if Redis is unavailable; requests are not allowed through without a rate-limit decision. Redis is also used for redirect caching.
- **Atomic click counts**: A JPQL bulk update increments `Urls.clickCount` in the database, avoiding lost updates from concurrent read-modify-write requests. The repository update count preserves 404 behavior for a missing short code.
- **Compose credentials**: The local Compose stack uses the MySQL root account for simplicity. Production deployments should use a least-privilege application account and a secrets manager instead of a checked-in `.env` file.
