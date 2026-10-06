# Learning Project Guide

This project is designed as a learning experience. To ensure deep understanding, every major feature must follow this rigorous implementation process.

## Feature Implementation Process

For every new feature, follow these 8 steps:

1. **Requirement**: Clearly define what the feature should do and why it is needed.
2. **Design**: Propose the architectural change, including new classes, method signatures, or database schema changes.
3. **Concepts**: Explain the underlying computer science or framework concepts used (e.g., "What is an `@Cacheable` annotation?").
4. **Alternatives**: Discuss other ways the feature could have been implemented and the trade-offs involved.
5. **Implementation**: Write the actual code.
6. **Explanation**: Walk through the implemented code and explain how it fulfills the design.
7. **Testing**: Create or update unit and integration tests to verify the feature.
8. **Documentation**: Update `ARCHITECTURE.md`, `DECISIONS.md`, and `TODO.md`.

## Current Learning Milestones
- [x] Understanding Spring Boot Layered Architecture.
- [x] Implementing JPA Repositories for MySQL.
- [x] Implementing Base62 encoding logic.
- [x] Integrating Redis for caching.
- [x] Handling Custom Exceptions and Global Error Responses.
- [ ] Implementing a comprehensive Test Suite.
- [ ] Adding advanced analytics/metrics.
- [ ] Improving the shortening algorithm (reduce DB writes).
