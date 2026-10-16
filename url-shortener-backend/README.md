# URL Shortener

A Spring Boot application that provides URL shortening services, allowing users to convert long URLs into short, manageable codes.

## Features
- **URL Shortening**: Convert long URLs into short codes.
- **Custom Aliases**: Support for user-defined short codes (column aliases).
- **Redirection**: Automatic redirection from short codes to original URLs.
- **Analytics**: Basic click tracking for shortened URLs.
- **Caching**: Redis-based caching for fast redirection.
- **Rate Limiting**: Redis-backed limits of 10 URL-creation requests and 100 redirects per minute per client IP, configurable independently.
- **URL Validation**: Ensures provided URLs are valid and reachable.

## Technology Stack
- **Language**: Java 17
- **Framework**: Spring Boot 4.1.0 (Spring Framework 6+)
- **Build Tool**: Maven
- **Database**: MySQL (Persistence)
- **Cache**: Redis (Performance)
- **Mapping**: ModelMapper

## Getting Started

### Prerequisites
- Java 17
- MySQL Server
- Redis Server

### Configuration
Set your database credentials through environment variables instead of committing them in `application.properties`.

In PowerShell:
```powershell
$env:DB_USERNAME = "root"
$env:DB_PASSWORD = "<your-database-password>"
```

In Bash:
```bash
export DB_USERNAME=root
export DB_PASSWORD='<your-database-password>'
```

The application uses `DB_USERNAME` (defaulting to `root`) and requires `DB_PASSWORD`.
Redis must be available for caching and rate limiting. Rate limiting fails closed: if Redis cannot evaluate the atomic counter, the endpoint returns HTTP 503 rather than bypassing the limit.
The public base URL defaults to `http://localhost:8080`; set `APP_PUBLIC_BASE_URL` to the externally reachable base URL when deploying behind a load balancer.

Configure the independent fixed-window limits in `application.properties` or with environment variables:

| Property | Default | Environment variable |
| --- | ---: | --- |
| `app.rate-limit.url-creation.max-requests` | `10` | `APP_RATE_LIMIT_URL_CREATION_MAX_REQUESTS` |
| `app.rate-limit.url-creation.window-seconds` | `60` | `APP_RATE_LIMIT_URL_CREATION_WINDOW_SECONDS` |
| `app.rate-limit.redirect.max-requests` | `100` | `APP_RATE_LIMIT_REDIRECT_MAX_REQUESTS` |
| `app.rate-limit.redirect.window-seconds` | `60` | `APP_RATE_LIMIT_REDIRECT_WINDOW_SECONDS` |
| `app.rate-limit.trusted-proxies` | empty (trust no forwarded headers) | `APP_RATE_LIMIT_TRUSTED_PROXIES` |

The endpoints have separate Redis key namespaces. Spring's forwarded-header processing is disabled; forwarded client addresses are used only when the socket peer matches a configured trusted proxy IP or CIDR.

In PowerShell:
```powershell
$env:APP_PUBLIC_BASE_URL = "https://short.example.com"
```

In Bash:
```bash
export APP_PUBLIC_BASE_URL=https://short.example.com
```

### Running behind Nginx with Docker Compose

The Compose stack builds the application image, runs two Spring Boot instances behind Nginx, and shares MySQL and Redis between them. Only Nginx is published to the host; the application, MySQL, and Redis ports remain internal to the Compose network.

1. Copy `.env.example` to `.env` and replace `MYSQL_ROOT_PASSWORD` with a local development password. Set `APP_PUBLIC_BASE_URL` to the public address clients will use if it differs from `http://localhost:8080`.
2. Start the stack from the project directory:

   ```powershell
   Copy-Item .env.example .env
   docker compose up --build -d --scale app=2
   ```

3. Open `http://localhost:8080/urlshortener/home` or send API requests to `http://localhost:8080`.

Nginx uses least-connections balancing and Docker's DNS to discover the app containers. Its access log includes the selected upstream address so you can observe where requests are routed. The number of app instances can be changed without editing the Nginx configuration:

```powershell
docker compose up --build -d --scale app=3
docker compose ps
docker compose logs -f nginx app
```

Stop the services with `docker compose down`. The MySQL data volume is retained; `docker compose down -v` also deletes that database data.

Nginx replaces `X-Forwarded-For` with the connecting client's address. The Compose network reserves `172.30.0.2` for Nginx and trusts only that address in the app. If the subnet conflicts with your environment, change the Compose subnet, Nginx address, and `APP_RATE_LIMIT_TRUSTED_PROXIES` together. For production, configure only the exact proxy IPs/CIDRs that can reach the app; if another load balancer sits in front of Nginx, configure Nginx real-IP handling to trust that upstream explicitly. Do not trust arbitrary `X-Forwarded-For` headers. Use managed secrets and configure TLS at Nginx or an upstream trusted load balancer.

Successful redirects still return HTTP 302. Click counts are incremented with one atomic database update instead of an in-memory read-modify-write.

### Running the Application
```bash
./mvnw spring-boot:run
```

### Running Tests
The test suite uses an in-memory H2 database and a local HTTP test server; it does not require MySQL or Redis.

```bash
./mvnw clean test
```

## Project Structure
The project follows a standard Spring Boot layered architecture:
- `controller`: REST API endpoints.
- `service`: Business logic interfaces.
- `serviceimplementation`: Logic implementation and utility classes.
- `repository`: Data access layer (Spring Data JPA).
- `entities`: Database models.
- `dtos`: Data Transfer Objects for requests and responses.
- `exception`: Custom exception handling.
- `config`: Application configurations (CORS, Redis).
