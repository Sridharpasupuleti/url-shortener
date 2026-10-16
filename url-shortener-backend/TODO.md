# TODO List

## Completed Functionality
- [x] Project setup with Spring Boot, Maven, MySQL, and Redis.
- [x] Database Entity `Urls` with fields for original URL, short code, and click count.
- [x] DAO Layer (`UrlRepository`) implementing basic CRUD and lookups by short code and original URL.
- [x] Shortening Logic:
    - Support for custom aliases (column aliases).
    - Auto-generation of short codes using Base62 encoding of the database ID.
- [x] Redirection Logic:
    - Retrieval of original URL via short code.
    - HTTP 302 redirect.
- [x] Analytics:
    - Incrementing click count on every redirection.
- [x] Infrastructure:
    - Redis caching for the redirection path.
    - CORS configuration for frontend integration.
    - URL validation (Syntax and Reachability) using `RestClient`.
- [x] Exception Handling:
    - Custom exceptions for `URLNotFound`, `UrlAlreadyExists`, and `ColumnAliasAlreadyExists`.
    - `ErrorResponse` DTO for consistent API error messages.
- [x] Automated tests for controller behavior, redirects, URL validation, duplicate aliases, JPA persistence, and database constraints.
- [x] Enforce short-code uniqueness in the database and handle concurrent alias conflicts.
- [x] Apply Redis-backed per-IP rate limiting to URL-shortening POST requests.
- [x] Add configurable independent Redis rate limits for URL creation and redirects, with trusted-proxy-aware client IP resolution and fail-closed Redis errors.
- [x] Make click counts atomic in the database and test concurrent increments.
- [x] Add a Docker Compose deployment with Nginx load balancing across scalable Spring Boot instances.

## Current Implementation State
The application is a functional MVP. Users can shorten URLs (with or without aliases) and be redirected back to the original site. Caching is active via Redis, and the optional Docker Compose stack puts Nginx in front of multiple app instances.

## Remaining Work
- [ ] **Comprehensive Testing**: Add broader end-to-end coverage for URL creation and redirection with the application, persistence layer, and cache working together.
- [ ] **API Improvements**:
    - Add an endpoint to retrieve analytics for a specific short code.
    - Implement pagination for a list of all shortened URLs (for admin/user view).
- [ ] **Security**:
    - Implement basic authentication for management endpoints.
- [ ] **Optimization**:
    - Optimize the `generateShortCode` process to avoid the double-save (save $\rightarrow$ encode $\rightarrow$ save).
- [ ] **Frontend**: Create a simple UI to interact with the API.

## Next Recommended Step
**Comprehensive Testing**: Add broader end-to-end coverage for URL creation and redirection with the application, persistence layer, and cache working together.
