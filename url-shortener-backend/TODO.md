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

## Current Implementation State
The application is a functional MVP. Users can shorten URLs (with or without aliases) and be redirected back to the original site. Caching is active via Redis.

## Remaining Work
- [ ] **Comprehensive Testing**: Currently, only the default `contextLoads` test exists. Need unit tests for `UrlServiceImpl` and integration tests for `HomeController`.
- [ ] **API Improvements**:
    - Add an endpoint to retrieve analytics for a specific short code.
    - Implement pagination for a list of all shortened URLs (for admin/user view).
- [ ] **Security**:
    - Add rate limiting to prevent abuse of the shortening endpoint.
    - Implement basic authentication for management endpoints.
- [ ] **Optimization**:
    - Optimize the `generateShortCode` process to avoid the double-save (save $\rightarrow$ encode $\rightarrow$ save).
- [ ] **Frontend**: Create a simple UI to interact with the API.

## Next Recommended Step
**Implement Unit and Integration Tests**: The codebase has almost no tests. Before adding new features or optimizing the shortening process, we must establish a safety net of tests to prevent regressions.
