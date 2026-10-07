# URL Shortener

A Spring Boot application that provides URL shortening services, allowing users to convert long URLs into short, manageable codes.

## Features
- **URL Shortening**: Convert long URLs into short codes.
- **Custom Aliases**: Support for user-defined short codes (column aliases).
- **Redirection**: Automatic redirection from short codes to original URLs.
- **Analytics**: Basic click tracking for shortened URLs.
- **Caching**: Redis-based caching for fast redirection.
- **Rate Limiting**: Redis-backed limit of 10 URL-shortening requests per minute per client IP.
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
Redis must be available for caching and URL-shortening rate limits.
The public base URL defaults to `http://localhost:8080`; set `APP_PUBLIC_BASE_URL` to the externally reachable base URL when deploying behind a load balancer.

In PowerShell:
```powershell
$env:APP_PUBLIC_BASE_URL = "https://short.example.com"
```

In Bash:
```bash
export APP_PUBLIC_BASE_URL=https://short.example.com
```

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
