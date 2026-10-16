# Architecture

The URL Shortener project uses a layered architecture to separate concerns and ensure maintainability.

## Layered Overview

### 1. Controller Layer (`com.example.url_shortener.controller`)
Handles incoming HTTP requests and returns responses.
- **`HomeController`**: The primary entry point.
    - `POST /urlshortener/posturl`: Shortens a URL.
    - `GET /urlshortener/{shortCode}`: Redirects to the original URL.
    - `GET /urlshortener/home`: Health check.

### 2. Service Layer (`com.example.url_shortener.service` & `serviceimplementation`)
Contains the business logic of the application.
- **`UrlService` (Interface)**: Defines the contract for URL operations.
- **`UrlServiceImpl` (Implementation)**:
    - Implements the shortening logic.
    - Handles custom aliases.
    - Manages click counts.
    - Integrates with Redis for caching `getOriginalUrl`.
- **`Base62` (Utility)**: Provides Base62 encoding to convert database IDs into short, alphanumeric codes.

### 3. Repository/DAO Layer (`com.example.url_shortener.repository`)
Handles data persistence and retrieval.
- **`UrlRepository`**: Extends `JpaRepository`, providing standard CRUD operations and custom queries:
    - `findByShortCode(String shortCode)`
    - `findByOriginalUrl(String url)`
    - Short codes are protected by the named database constraint `uk_urls_short_code`; concurrent alias conflicts are returned as the existing alias-conflict error.

### 4. Entity Layer (`com.example.url_shortener.entities`)
Defines the data model.
- **`Urls`**: Represents the URL mapping in the database.
    - `id` (Primary Key)
    - `originalUrl` (The long URL)
    - `shortCode` (The unique short identifier)
    - `clickCount` (Counter for redirections)

## Data Flow

### Shortening Process
1. `HomeController` receives `Urldto`.
2. Validates URL format and reachability using `RestClient`.
3. `UrlServiceImpl` checks if the URL or alias already exists.
4. If a custom alias is provided, it's used as the `shortCode`.
5. Otherwise, the entity is saved to MySQL to generate an ID, which is then Base62 encoded to create the `shortCode`.
6. The entity is updated with the `shortCode` and saved again.

### Redirection Process
1. `HomeController` resolves the caller IP, trusting `X-Forwarded-For` only when the direct socket peer matches a configured trusted proxy.
2. The redirect-specific Redis counter is atomically incremented. Requests over the configured limit return HTTP 429 before URL lookup or click updates.
3. `UrlServiceImpl.getOriginalUrl` checks Redis for the `shortCode` $\rightarrow$ `originalUrl` mapping.
4. **DB Fallback**: If not in cache, MySQL is queried via `UrlRepository`.
5. `UrlServiceImpl.incrementClickCount` performs an atomic JPQL update against the mapped `Urls` entity. A zero-row update is treated as URL-not-found.
6. `HomeController` returns an HTTP 302 redirect to the `originalUrl`.

## Infrastructure
- **MySQL**: Stores the persistent mapping of URLs.
- **Redis**: Stores frequently accessed mappings to reduce DB load.
- **Rate limiter**: One shared Redis-backed service atomically counts URL creation and redirect requests in distinct key namespaces. Defaults are 10 creations and 100 redirects per client IP in 60-second fixed windows. Other endpoints are not rate-limited. Redis failures fail closed with HTTP 503.
- **Public short URL**: Generated links use the configurable `app.public-base-url` property (`APP_PUBLIC_BASE_URL`), defaulting to `http://localhost:8080` for local development.
- **Load balancing**: Docker Compose runs two or more Spring Boot app containers behind Nginx. Nginx uses least-connections balancing and Docker DNS service discovery; MySQL and Redis are shared across the app instances. Only Nginx publishes a host port.
- **Proxy headers**: Spring forwarded-header processing is disabled. The resolver trusts forwarded headers only when the socket peer is in `app.rate-limit.trusted-proxies`; Compose reserves `172.30.0.2` for Nginx, which overwrites `X-Forwarded-For` with the connecting client's address. Do not expose app containers directly to untrusted clients.
- **ModelMapper**: Maps between DTOs and Entities.

## Test Coverage
- **Controller**: `HomeControllerTest` exercises HTTP responses, redirects, URL validation, unreachable targets, and duplicate-alias responses with MockMvc.
- **Persistence**: `UrlRepositoryIntegrationTest` runs against an in-memory H2 database and covers lookups, unique short codes, and database column constraints.
- **Application context**: `UrlShortenerApplicationTests` uses H2 and disables external cache connectivity so the context test does not require local MySQL or Redis.
- **Rate limiting**: `UrlShorteningRateLimiterTest` verifies the creation threshold, independent redirect threshold/key namespace, shared per-IP keys across limiter instances, fixed-window expiry behavior, hashed identities, and fail-closed behavior. `ClientIpResolverTest` verifies the trusted-proxy boundary. Controller tests verify rejected redirects do not look up or count URLs.
- **Click counting**: repository integration tests run against the entity mappings and H2 dialect, including concurrent atomic increments and zero-row updates for missing codes.
- Run the full suite with `./mvnw clean test`.
