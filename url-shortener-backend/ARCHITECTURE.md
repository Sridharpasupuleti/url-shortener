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
1. `HomeController` receives a `shortCode`.
2. `UrlServiceImpl.getOriginalUrl` is called.
3. **Cache Check**: Redis is checked for the `shortCode` $\rightarrow$ `originalUrl` mapping.
4. **DB Fallback**: If not in cache, MySQL is queried via `UrlRepository`.
5. `UrlServiceImpl.incrementClickCount` is called to update the analytics.
6. `HomeController` sends an HTTP 302 redirect to the `originalUrl`.

## Infrastructure
- **MySQL**: Stores the persistent mapping of URLs.
- **Redis**: Stores frequently accessed mappings to reduce DB load.
- **Rate limiter**: Atomically counts URL-shortening POST requests per observed client IP in Redis; exceeding 10 in a 60-second window returns HTTP 429. Other endpoints are not rate-limited.
- **Public short URL**: Generated links use the configurable `app.public-base-url` property (`APP_PUBLIC_BASE_URL`), defaulting to `http://localhost:8080` for local development.
- **ModelMapper**: Maps between DTOs and Entities.

## Test Coverage
- **Controller**: `HomeControllerTest` exercises HTTP responses, redirects, URL validation, unreachable targets, and duplicate-alias responses with MockMvc.
- **Persistence**: `UrlRepositoryIntegrationTest` runs against an in-memory H2 database and covers lookups, unique short codes, and database column constraints.
- **Application context**: `UrlShortenerApplicationTests` uses H2 and disables external cache connectivity so the context test does not require local MySQL or Redis.
- **Rate limiting**: `UrlShorteningRateLimiterTest` verifies the ten-request threshold, hashed Redis key, and fail-closed Redis behavior. Controller tests verify the 429 response and that GET endpoints are not rate-limited.
- Run the full suite with `./mvnw clean test`.
