# Codebase Structure Documentation

This document describes the current structure of the BasicCRM_FWF backend application, a Spring Boot-based CRM system.

---

## Table of Contents

1. [Overview](#overview)
2. [Project Structure](#project-structure)
3. [Package Organization](#package-organization)
4. [Architecture Layers](#architecture-layers)
5. [Key Components](#key-components)
6. [Technology Stack](#technology-stack)

---

## Overview

**Project Name:** BasicCRM_FWF (Face Wash Fox CRM)  
**Framework:** Spring Boot 3.5.3  
**Java Version:** 21  
**Architecture:** Layered Architecture (Controller → Service → Repository → Model)  
**Database:** MySQL with JPA/Hibernate  
**Security:** Spring Security with JWT & OAuth2

---

## Project Structure

```
Backend/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/example/BasicCRM_FWF/
│   │   │       ├── BasicCrmFwfApplication.java    # Main application entry point
│   │   │       ├── Config/                        # Configuration classes
│   │   │       ├── Controller/                    # REST API controllers
│   │   │       ├── DTO/                           # Data Transfer Objects (general)
│   │   │       ├── DTORealTime/                   # Real-time data DTOs
│   │   │       ├── DTORequest/                    # Request DTOs
│   │   │       ├── DTOResponse/                  # Response DTOs
│   │   │       ├── Exception/                    # Custom exception handlers
│   │   │       ├── Filter/                       # HTTP filters (JWT, rate limiting, etc.)
│   │   │       ├── Helper/                       # Utility helper classes
│   │   │       ├── Mailing/                      # Email service components
│   │   │       ├── Model/                        # JPA entity models
│   │   │       ├── Repository/                   # JPA repositories
│   │   │       ├── RoleAndPermission/            # Role and permission enums/classes
│   │   │       ├── Service/                     # Business logic services
│   │   │       ├── Token/                       # Token management
│   │   │       └── Utils/                       # Utility classes
│   │   └── resources/
│   │       ├── application.yml                  # Main configuration
│   │       ├── application-prod.yml             # Production configuration
│   │       ├── static/                          # Static resources
│   │       └── templates/                       # Template files (JTE)
│   └── test/
│       └── java/
│           └── com/example/BasicCRM_FWF/
│               └── BasicCrmFwfApplicationTests.java
├── target/                                       # Compiled classes and build artifacts
├── pom.xml                                       # Maven dependencies and build config
├── mvnw                                          # Maven wrapper (Unix)
└── mvnw.cmd                                      # Maven wrapper (Windows)
```

---

## Package Organization

### 1. **Config/** (12 files)
Configuration classes for Spring Boot application setup.

| File | Purpose |
|------|---------|
| `ApplicationConfiguration.java` | General application configuration |
| `CustomAccessDeniedHandler.java` | Custom 403 error handler |
| `CustomAuthEntryPoint.java` | Custom 401 authentication entry point |
| `DotenvPropertySourceInitializer.java` | Loads `.env` file for environment variables |
| `FWFApiExecutor.java` | Executor for FWF API calls with rate limiting |
| `JWTDecoderConfig.java` | JWT decoder configuration |
| `MailConfig.java` | Email service configuration |
| `OpenApiConfiguration.java` | Swagger/OpenAPI documentation setup |
| `ProjectConfiguration.java` | Project-specific configurations |
| `SecurityConfiguration.java` | Spring Security configuration (JWT, OAuth2) |
| `TokenCache.java` | Token caching mechanism |
| `WebClientConfig.java` | WebClient configuration for HTTP calls |
| `WebConfig.java` | Web MVC configuration (CORS, interceptors) |

### 2. **Controller/** (11 files)
REST API endpoints - handles HTTP requests and responses.

| File | Purpose |
|------|---------|
| `AdminController.java` | Admin-specific endpoints |
| `AppUsageRecordController.java` | Application usage tracking endpoints |
| `AuthenticationController.java` | Login, register, password reset endpoints |
| `BookingRecordController.java` | Booking management endpoints |
| `CustomerSaleRecordController.java` | Customer sales tracking endpoints |
| `ManagerController.java` | Manager-specific endpoints |
| `RealTimeController.java` | Real-time data endpoints |
| `SalesTransactionController.java` | Sales transaction endpoints |
| `ServiceRecordController.java` | Service record management endpoints |
| `ShiftController.java` | Shift management endpoints |
| `UserController.java` | User management endpoints |

### 3. **DTO/** (11 files)
General Data Transfer Objects for API communication.

| File | Purpose |
|------|---------|
| `AuthenticationRequest.java` | Login/register request DTO |
| `AuthenticationResponse.java` | Authentication response with tokens |
| `ChangeForgotPasswordRequest.java` | Password reset request DTO |
| `ChangePasswordRequest.java` | Change password request DTO |
| `CustomerSource.java` | Customer source information DTO |
| `FullDateRangeResponse.java` | Date range response wrapper |
| `PageableResponse.java` | Paginated response wrapper |
| `PhoneExportDTO.java` | Phone number export DTO |
| `RegisterRequest.java` | User registration request DTO |
| `ResponseMessageAPI.java` | Standard API response wrapper |
| `UserDTO.java` | User data transfer object |

### 4. **DTORealTime/** (6 files)
Real-time data transfer objects for live updates.

| File | Purpose |
|------|---------|
| `BookingDTO.java` | Booking real-time data |
| `CustomerDTO.java` | Customer real-time data |
| `SalesDetailDTO.java` | Sales detail real-time data |
| `SalesSummaryDTO.java` | Sales summary real-time data |
| `ServiceItems.java` | Service items real-time data |
| `ServiceSummaryDTO.java` | Service summary real-time data |

### 5. **DTORequest/** (1 file)
Request-specific DTOs.

| File | Purpose |
|------|---------|
| `CustomerReportRequest.java` | Customer report request parameters |

### 6. **DTOResponse/** (43 files)
Response DTOs for various API endpoints.

*Note: Contains 43 response DTOs for different API responses including reports, summaries, and detailed data structures.*

### 7. **Exception/** (7 files)
Custom exception classes and global exception handlers.

| Purpose |
|---------|
| Custom business logic exceptions |
| Global exception handler |
| Error response formatting |
| Validation exception handling |

### 8. **Filter/** (3 files)
HTTP request filters for cross-cutting concerns.

| File | Purpose |
|------|---------|
| `DomainBlockFilter.java` | Blocks requests from unauthorized domains |
| `JWTAuthenticationFilter.java` | JWT token validation filter |
| `RateLimitFilter.java` | Rate limiting to prevent abuse |

### 9. **Helper/** (2 files)
Utility helper classes.

| File | Purpose |
|------|---------|
| `Helper.java` | General utility methods (date ranges, comparisons, etc.) |
| `ValidateRole.java` | Role validation utilities |

### 10. **Mailing/** (4 files)
Email service components.

| Purpose |
|---------|
| Email template management |
| Email sending service |
| Email content builders |

### 11. **Model/** (15 files)
JPA entity models representing database tables.

| File | Purpose |
|------|---------|
| `AppliedCard.java` | Applied card/coupon entity |
| `AppUsageRecord.java` | Application usage tracking entity |
| `BaseEntity.java` | Base entity with common fields (id, timestamps, audit) |
| `BookingRecord.java` | Booking/appointment entity |
| `BookingStatus.java` | Booking status enum |
| `CustomerSaleRecord.java` | Customer sales record entity |
| `OTP.java` | One-time password entity |
| `Region.java` | Region/location entity |
| `SaleServiceItem.java` | Sale service item entity |
| `SalesTransaction.java` | Sales transaction entity |
| `ServiceRecord.java` | Service record entity |
| `ServiceType.java` | Service type entity |
| `ServiceTypeTemp.java` | Temporary service type entity |
| `Shift.java` | Employee shift entity |
| `User.java` | User entity |

### 12. **Repository/** (16 files)
JPA repositories for database access.

*Note: Contains 16 repository interfaces extending JpaRepository for each entity model.*

### 13. **RoleAndPermission/** (2 files)
Role and permission management.

| File | Purpose |
|------|---------|
| `Permission.java` | Permission enum/class |
| `Role.java` | User role enum (ADMIN, MANAGER, etc.) |

### 14. **Service/** (26 files)
Business logic layer - organized by feature domain.

#### Service Structure:
```
Service/
├── AppUsageRecord/
│   ├── AppUsageRecordInterface.java
│   └── AppUsageRecordService.java
├── AuthenticationService/
│   ├── IAuthenticationService.java
│   └── AuthenticationServiceImpl.java
├── AuthRealTime/
│   └── AuthService.java
├── BookingRecord/
│   ├── BookingRecordInterface.java
│   └── BookingRecordService.java
├── CustomerSaleRecord/
│   ├── CustomerSaleRecordInterface.java
│   └── CustomerSaleRecordService.java
├── FullDateRangeService.java
├── JWTService.java
├── LogoutService.java
├── Realtime/
│   ├── RealTimeInterface.java
│   ├── RealTimeService.java
│   └── SalesServiceImpl.java
├── SalesTransaction/
│   ├── SalesTransactionInterface.java
│   └── SalesTransactionService.java
├── SecureTokenService/
│   ├── ISecureTokenService.java
│   └── SecureTokenServiceImpl.java
├── ServiceRecord/
│   ├── ServiceRecordInterface.java
│   └── ServiceRecordService.java
├── ShiftEmployee/
│   ├── ShiftEmployeeInterface.java
│   └── ShiftEmployeeService.java
├── TokenCleanupScheduler.java
└── User/
    ├── UserService.java
    └── UserServiceInterface.java
```

### 15. **Token/** (3 files)
Token management and validation.

| Purpose |
|---------|
| JWT token generation |
| Token validation |
| Token refresh logic |

### 16. **Utils/** (1 file)
General utility classes.

---

## Architecture Layers

The application follows a **layered architecture** pattern:

```
┌─────────────────────────────────────┐
│         Controller Layer             │  ← REST API endpoints
│      (HTTP Request/Response)          │
└─────────────────┬───────────────────┘
                  │
                  ▼
┌─────────────────────────────────────┐
│          Service Layer               │  ← Business logic
│    (Interface + Implementation)     │
└─────────────────┬───────────────────┘
                  │
                  ▼
┌─────────────────────────────────────┐
│        Repository Layer              │  ← Data access
│      (JPA Repository Interfaces)    │
└─────────────────┬───────────────────┘
                  │
                  ▼
┌─────────────────────────────────────┐
│          Model Layer                 │  ← Database entities
│      (JPA Entity Classes)           │
└─────────────────────────────────────┘
```

### Layer Responsibilities:

1. **Controller Layer**
   - Handles HTTP requests
   - Validates input
   - Returns HTTP responses
   - Maps DTOs to/from entities

2. **Service Layer**
   - Contains business logic
   - Orchestrates multiple repositories
   - Handles transactions
   - Implements interface-based design

3. **Repository Layer**
   - Database access abstraction
   - CRUD operations
   - Custom queries
   - Extends Spring Data JPA

4. **Model Layer**
   - Database entity definitions
   - JPA annotations
   - Relationships between entities
   - Extends `BaseEntity` for common fields

### Cross-Cutting Concerns:

- **Security**: JWT authentication, OAuth2, role-based access control
- **Filtering**: JWT validation, rate limiting, domain blocking
- **Exception Handling**: Global exception handlers
- **Configuration**: Centralized in `Config/` package
- **Caching**: Token caching mechanism

---

## Key Components

### 1. **Authentication & Authorization**
- **JWT-based authentication** with refresh tokens
- **OAuth2** integration for social login
- **Role-based access control** (ADMIN, MANAGER, USER)
- **Custom security filters** for token validation

### 2. **Real-time Data**
- Real-time endpoints for live updates
- WebSocket or Server-Sent Events support
- Real-time DTOs for live data transfer

### 3. **Email Service**
- Email templates
- Password reset emails
- Notification emails

### 4. **Rate Limiting**
- Prevents API abuse
- Configurable rate limits per endpoint

### 5. **API Documentation**
- OpenAPI/Swagger integration
- Auto-generated API documentation

### 6. **Scheduling**
- `@EnableScheduling` enabled
- Token cleanup scheduler
- Background job processing

---

## Technology Stack

### Core Framework
- **Spring Boot 3.5.3** - Application framework
- **Java 21** - Programming language
- **Maven** - Build tool and dependency management

### Web & API
- **Spring Web MVC** - REST API framework
- **Spring Validation** - Input validation
- **OpenAPI/Swagger** - API documentation

### Data Access
- **Spring Data JPA** - Database abstraction
- **Hibernate** - ORM framework
- **MySQL** - Relational database
- **Redis** - Caching (configured in application.yml)

### Security
- **Spring Security** - Security framework
- **JWT (jjwt)** - Token-based authentication
- **OAuth2 Client & Resource Server** - OAuth2 support

### Email
- **Spring Mail** - Email sending
- **SMTP** - Email protocol (Gmail configured)

### Template Engine
- **JTE** - Java template engine
- **Thymeleaf Layout Dialect** - Layout management

### HTTP Client
- **WebClient** (Spring WebFlux) - Reactive HTTP client
- **Apache HttpClient5** - HTTP client library

### Cloud Services
- **Cloudinary** - Media storage and management

### Utilities
- **Lombok** - Reduces boilerplate code
- **Apache Commons Lang** - Utility functions
- **Dotenv Java** - Environment variable management
- **JavaFaker** - Test data generation
- **Apache POI** - Excel file handling
- **Jsoup** - HTML parsing
- **Bucket4j** - Rate limiting library

### Testing
- **Spring Boot Test** - Testing framework
- **Mockito** - Mocking framework
- **Spring Security Test** - Security testing utilities

---

## Design Patterns Used

1. **Repository Pattern** - Data access abstraction
2. **Service Layer Pattern** - Business logic separation
3. **DTO Pattern** - Data transfer objects for API communication
4. **Builder Pattern** - Used in DTOs (Lombok @Builder)
5. **Strategy Pattern** - Different authentication strategies
6. **Filter Pattern** - HTTP request filtering
7. **Dependency Injection** - Spring's IoC container

---

## Configuration Files

### `application.yml`
Main configuration file containing:
- Database connection settings
- JPA/Hibernate configuration
- Redis configuration
- Mail server configuration
- JWT settings
- Application URLs
- Logging configuration
- Server settings

### `application-prod.yml`
Production-specific overrides.

---

## Best Practices Implemented

1. ✅ **Separation of Concerns** - Clear layer separation
2. ✅ **Interface-based Design** - Services use interfaces
3. ✅ **DTO Pattern** - Separate DTOs from entities
4. ✅ **Exception Handling** - Centralized exception management
5. ✅ **Security** - JWT + OAuth2 authentication
6. ✅ **Rate Limiting** - API protection
7. ✅ **API Documentation** - OpenAPI/Swagger
8. ✅ **Environment Variables** - `.env` file support
9. ✅ **Base Entity** - Common fields in base class
10. ✅ **Scheduling** - Background job support

---

## File Count Summary

| Package | File Count | Description |
|---------|-----------|-------------|
| Config | 12 | Configuration classes |
| Controller | 11 | REST API controllers |
| DTO | 11 | General DTOs |
| DTORealTime | 6 | Real-time DTOs |
| DTORequest | 1 | Request DTOs |
| DTOResponse | 43 | Response DTOs |
| Exception | 7 | Exception handlers |
| Filter | 3 | HTTP filters |
| Helper | 2 | Utility helpers |
| Mailing | 4 | Email services |
| Model | 15 | JPA entities |
| Repository | 16 | Data repositories |
| RoleAndPermission | 2 | Role/permission classes |
| Service | 26 | Business logic services |
| Token | 3 | Token management |
| Utils | 1 | Utilities |
| **Total** | **~163** | Java source files |

---

## Development Guidelines

### Adding New Features

1. **Create Entity** in `Model/` package
2. **Create Repository** in `Repository/` package
3. **Create Service Interface** in `Service/<FeatureName>/`
4. **Implement Service** in `Service/<FeatureName>/`
5. **Create DTOs** in appropriate DTO package
6. **Create Controller** in `Controller/` package
7. **Add Security Rules** in `SecurityConfiguration.java`
8. **Update API Documentation** (auto-generated via annotations)

### Naming Conventions

- **Controllers**: `*Controller.java`
- **Services**: `*Service.java` (interface), `*ServiceImpl.java` (implementation)
- **Repositories**: `*Repository.java`
- **DTOs**: `*DTO.java`, `*Request.java`, `*Response.java`
- **Models**: PascalCase (e.g., `User.java`, `BookingRecord.java`)

---

## Notes

- The application uses **Lombok** extensively for reducing boilerplate code
- **Builder pattern** is used in DTOs via `@Builder` annotation
- **BaseEntity** provides common fields (id, timestamps, audit fields)
- **Real-time features** are supported through dedicated DTOs and services
- **Token cleanup** is handled by scheduled tasks
- **Rate limiting** prevents API abuse
- **Domain blocking** filter restricts unauthorized access

---

**Last Updated:** [Current Date]  
**Maintained by:** Development Team
