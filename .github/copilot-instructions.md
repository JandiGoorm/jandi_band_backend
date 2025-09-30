# RhythMeet Backend - AI Coding Assistant Instructions

## Project Overview
RhythMeet is a Spring Boot backend for a university music band community platform. It manages clubs, teams, users, performance promotions, and social features for musicians.

## Architecture & Tech Stack
- **Framework**: Spring Boot 3.4.5 with Java 21
- **Build**: Gradle with multi-stage Docker builds
- **Database**: MySQL with JPA/Hibernate (ddl-auto: validate)
- **Auth**: JWT tokens with Kakao OAuth2 integration
- **Storage**: AWS S3 for images, Redis for caching
- **Search**: Elasticsearch 8.12.0 with Kibana
- **Monitoring**: Prometheus + Grafana stack
- **CI/CD**: Jenkins with GitHub Container Registry

## Core Domain Structure
```
src/main/java/com/jandi/band_backend/
├── auth/           # Kakao OAuth authentication
├── club/           # University clubs and membership
├── user/           # User profiles and management
├── team/           # Band teams within clubs
├── promo/          # Performance promotions and events
├── invite/         # Club/team invitation system
├── poll/           # Voting for songs/setlists
├── notice/         # Club announcements
├── univ/           # University and region data
├── image/          # Image upload/management
├── health/         # Health checks and monitoring
├── security/       # JWT and Spring Security config
├── config/         # Application configuration
└── global/         # Common DTOs, exceptions, utilities
```

## API Patterns & Conventions

### Response Format
All APIs return `CommonRespDTO<T>` with consistent structure:
```json
{
  "success": true,
  "message": "Operation successful",
  "data": { /* actual response */ },
  "errorCode": null
}
```

### Authentication
- JWT tokens in `Authorization: Bearer {token}` header
- `@AuthenticationPrincipal CustomUserDetails` for current user
- Kakao OAuth for login with refresh token rotation

### File Uploads
- Multipart form data with `@RequestParam MultipartFile`
- AWS S3 integration for storage
- Profile photos, club images, promo photos

### Error Handling
- Custom exceptions in `global/exception/`
- Global exception handler maps to HTTP status codes
- Soft deletes preserve referential integrity

## Development Workflows

### Local Development Setup
```bash
# Start Elasticsearch stack
.\search\start-elasticsearch-windows.ps1

# Start monitoring stack
.\monitoring-local\scripts\start-local.ps1

# Run application
./gradlew bootRun
```

### Database Schema
- JPA entities with validation annotations
- `ddl-auto: validate` - schema managed externally
- Foreign key constraints with soft delete support

### Testing
- Unit tests with `@SpringBootTest`
- Integration tests for API endpoints
- Test containers for infrastructure dependencies

### Deployment
- Multi-stage Docker build for optimization
- Jenkins pipelines for dev/master branches
- External config files mounted at runtime

## Key Configuration Files

### `application.properties`
- Database: MySQL connection with timezone settings
- JWT: Token validity periods and secrets
- AWS: S3 credentials and bucket config
- Redis: Local development connection
- CORS: Frontend origin allowlist
- Monitoring: Actuator endpoints and metrics

### Docker Compose Files
- `monitoring-local/docker-compose.local.yml`: Dev monitoring stack
- `search/docker-compose.elasticsearch.windows.yml`: Local search stack
- External networks for service communication

## Common Patterns

### Entity Relationships
- Users belong to Clubs, Clubs have Teams
- Soft delete with `deletedAt` timestamps
- Cascade operations for membership changes

### Service Layer
- Transactional operations with `@Transactional`
- Repository injection with constructor injection
- Business logic separated from controllers

### Controller Patterns
- RESTful endpoints with `@RequestMapping`
- Swagger documentation with `@Operation`
- Multipart handling for file uploads

### Security Configuration
- JWT filter before username/password auth
- CORS for multiple frontend environments
- Public endpoints for auth, images, promos

## Infrastructure Commands

### Elasticsearch Operations
```bash
# Sync all promo data to ES
POST /api/admin/promos/sync-all

# Check ES health
GET /_cluster/health
```

### Monitoring
- Prometheus: `http://localhost:9090`
- Grafana: `http://localhost:3000` (admin/admin123)
- Health checks: `/actuator/health`

### Docker Networks
- `spring-app_spring-network`: Application services
- `monitoring`: Metrics collection
- `elastic`: Search infrastructure

## Code Quality Standards

### Naming Conventions
- PascalCase for classes, camelCase for methods/variables
- RESTful URL patterns: `/api/{resource}s/{id}/{subresource}`
- Consistent package structure mirroring domains

### Validation
- Bean validation annotations on DTOs
- Custom validators for business rules
- Input sanitization for security

### Logging
- Structured JSON logging with Logstash encoder
- Different log files for business, errors, security
- Configurable log levels per environment

## Deployment Environments

### Development (dev branch)
- Jenkins builds and pushes to GHCR with `:BUILD_NUMBER` tag
- Deploys to local on-premise server
- Full monitoring stack available

### Production (master branch)
- Automated deployment to EC2 via Jenkins
- External monitoring and alerting
- Production-grade Docker configurations

## Troubleshooting

### Common Issues
- Database connection: Check MySQL container and credentials
- Redis connection: Verify local Redis service
- Elasticsearch: Check cluster health and data sync
- File uploads: Validate S3 credentials and bucket permissions

### Logs
- Application logs: `logs/jandi-backend-*.log`
- Docker logs: `docker logs {container_name}`
- Jenkins builds: Pipeline console output

### Health Checks
- Application: `/actuator/health`
- Database: `/api/health/database`
- Redis: `/api/health/redis`
- Elasticsearch: `/api/health/elasticsearch`</content>
<parameter name="filePath">c:\Users\USER\source\jandi_band_backend\.github\copilot-instructions.md