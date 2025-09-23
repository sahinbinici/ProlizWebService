# ProlizWebServices - SOAP to REST Adapter

A Spring Boot-based web service application that acts as an intermediary layer between external clients and a remote SOAP-based service hosted at Gaziantep University.

## Project Overview

This application provides a RESTful interface to access and interact with the remote SOAP-based service, simplifying integration for modern web and mobile clients that prefer REST over SOAP.

## Recent Improvements

### 1. ✅ Complete API Implementation
- **All missing client methods implemented**: Added all missing SOAP client methods that were referenced in the controller
- **Student Operations**: `ogrenciSifreKontrol`, `getOgrenciBilgileri`, `getOgrenciDersKayitlari`, `getOgrenciNotlari`, `getOgrenciTranskripti`
- **Organizational Data**: `getDonemBilgileri`, `getFakulteBilgileri`, `getBolumBilgileri`, `getDersProgrami`, `getSinavProgrami`

### 2. ✅ Enhanced Security
- **Replaced global SSL disabling** with targeted certificate management
- **Custom SSL Configuration**: Created `SSLConfig` class with domain-specific trust management
- **Secure RestTemplate**: Uses Apache HttpClient with proper SSL context
- **Development Fallback**: Insecure RestTemplate available for development only

### 3. ✅ Comprehensive Error Handling
- **Custom Exceptions**: `SoapServiceException` and `ValidationException` for specific error types
- **Global Exception Handler**: Consistent error responses across all endpoints
- **Detailed Error Information**: Includes timestamps, error codes, and contextual information
- **Proper HTTP Status Codes**: Appropriate status codes for different error scenarios

### 4. ✅ Input Validation
- **Parameter Validation**: All endpoints validate required parameters
- **Custom Validation Logic**: Validates non-empty strings and required fields
- **Meaningful Error Messages**: Clear validation error responses with field information

### 5. ✅ Enhanced Logging
- **Structured Logging**: SLF4J with logback configuration
- **Request/Response Logging**: Detailed logging for debugging and monitoring
- **Log Levels**: Appropriate log levels (INFO, DEBUG, ERROR) for different operations
- **Log File Configuration**: Rotating log files with size and history management

## Technology Stack

- **Java 17**
- **Spring Boot 3.4.5**
- **JAX-WS 4.0.0** (for WSDL processing)
- **Apache HttpClient 4.5.14** (for SSL configuration)
- **SLF4J + Logback** (for logging)
- **JUnit 5 + Mockito** (for testing)
- **SpringDoc OpenAPI 2.6.0** (for Swagger API documentation)

## Project Structure

```
src/
├── main/
│   ├── java/
│   │   └── com/prolizwebservices/
│   │       ├── client/
│   │       │   └── OgrenciWebServiceClient.java      # SOAP client with all methods
│   │       ├── config/
│   │       │   └── SSLConfig.java                    # Secure SSL configuration
│   │       ├── controller/
│   │       │   └── ProlizWebServiceController.java   # REST endpoints with validation
│   │       ├── exception/
│   │       │   ├── GlobalExceptionHandler.java       # Centralized error handling
│   │       │   ├── SoapServiceException.java         # SOAP-specific exceptions
│   │       │   └── ValidationException.java          # Validation exceptions
│   │       ├── ProlizWebServicesApplication.java     # Main Spring Boot class
│   │       └── ServletInitializer.java              # WAR deployment support
│   └── resources/
│       ├── application.properties                    # Enhanced configuration
│       └── bindings.xml                             # JAX-WS bindings
└── test/
    └── java/
        └── com/prolizwebservices/
            ├── client/
            │   └── OgrenciWebServiceClientTest.java  # Client unit tests
            └── controller/
                └── ProlizWebServiceControllerTest.java # Controller integration tests
```

## API Documentation

### Interactive Swagger UI
Once the application is running, you can access the interactive API documentation at:

- **Swagger UI**: `http://localhost:8080/proliz/swagger-ui.html`
- **OpenAPI JSON**: `http://localhost:8080/proliz/api-docs`

The Swagger UI provides:
- 📖 **Complete API Documentation** with detailed descriptions
- 🧪 **Interactive Testing** - try endpoints directly from the browser
- 📝 **Request/Response Examples** for all endpoints
- 🏷️ **Organized by Categories**: Authentication, Student Info, Education, Organization
- 🔍 **Search and Filter** capabilities

### API Endpoint Groups
The documentation is organized into logical groups:
1. **Authentication APIs** - Login and password verification
2. **Student Information APIs** - Student data and academic records
3. **Education & Academic APIs** - Courses, schedules, and programs
4. **Organizational APIs** - Faculties, departments, and terms

## API Endpoints

### Authentication Endpoints
- `POST /proliz/api/akademik-personel/sifre-kontrol` - Academic staff password verification
- `POST /proliz/api/ogrenci/sifre-kontrol` - Student password verification

### Distance Education Endpoints
- `GET /proliz/api/uzaktan-egitim/dersler` - List distance education courses
- `GET /proliz/api/uzaktan-egitim/ders/{dersHarID}/ogrenciler` - Students in a course
- `GET /proliz/api/ders/{dersHarID}/ogretim-elemani` - Course instructor information

### Student Information Endpoints
- `GET /proliz/api/ogrenci/{ogrenciNo}` - Student information
- `GET /proliz/api/ogrenci/{ogrenciNo}/ders-kayitlari?donemKodu={kod}` - Student course registrations
- `GET /proliz/api/ogrenci/{ogrenciNo}/notlar?donemKodu={kod}` - Student grades
- `GET /proliz/api/ogrenci/{ogrenciNo}/transkript` - Student transcript

### Organizational Information Endpoints
- `GET /proliz/api/donemler` - Academic terms
- `GET /proliz/api/fakulteler` - Faculties
- `GET /proliz/api/fakulte/{fakulteKodu}/bolumler` - Departments in a faculty
- `GET /proliz/api/bolum/{bolumKodu}/ders-programi?donemKodu={kod}` - Course schedule
- `GET /proliz/api/bolum/{bolumKodu}/sinav-programi?donemKodu={kod}` - Exam schedule

## Configuration

### Application Properties
```properties
# Server Configuration
server.port=8080
server.servlet.context-path=/proliz

# Logging Configuration
logging.level.com.prolizwebservices=INFO
logging.file.name=logs/proliz-web-services.log

# Connection Settings
spring.mvc.async.request-timeout=30000
```

### SSL Configuration
The application uses a custom SSL configuration that:
- Trusts certificates from `gantep.edu.tr` domain
- Validates hostname for security
- Provides connection and read timeouts
- Falls back to insecure mode for development (if needed)

## Building and Running

### Prerequisites
- JDK 17
- Maven 3.x

### Build Commands
```bash
# Clean and compile
mvn clean compile

# Run tests
mvn test

# Package as WAR
mvn package

# Run locally
mvn spring-boot:run
```

### Deployment
- **Standalone**: `java -jar target/ProlizWebServices-0.0.1-SNAPSHOT.war`
- **Tomcat**: Deploy WAR file to servlet container
- **Application URL**: `http://localhost:8080/proliz`
- **Swagger UI**: `http://localhost:8080/proliz/swagger-ui.html`

## Error Handling

The application provides comprehensive error handling with structured JSON responses:

### Validation Error (400 Bad Request)
```json
{
  "timestamp": "2024-01-15T10:30:00",
  "status": 400,
  "error": "Validation Error",
  "message": "ogrenciNo cannot be null or empty",
  "field": "ogrenciNo"
}
```

### SOAP Service Error (503 Service Unavailable)
```json
{
  "timestamp": "2024-01-15T10:30:00",
  "status": 503,
  "error": "SOAP Service Error",
  "message": "Remote service unavailable: Connection timed out",
  "soapAction": "http://tempuri.org/OgrenciBilgileri"
}
```

## Testing

The project includes comprehensive unit and integration tests:
- **Client Tests**: Mock SOAP service responses and error scenarios
- **Controller Tests**: Validate REST endpoints and error handling
- **Integration Tests**: End-to-end testing with Spring Boot Test

Run tests with: `mvn test`

## Security Considerations

- ✅ **SSL/TLS**: Proper certificate validation for the target domain
- ✅ **Input Validation**: All parameters validated before processing
- ✅ **Error Handling**: No sensitive information exposed in error messages
- ✅ **Logging**: Sensitive data excluded from logs
- ⚠️ **Authentication**: Currently no authentication mechanism (consider adding)
- ⚠️ **Rate Limiting**: No rate limiting implemented (consider adding)

## Monitoring and Logging

- **Structured Logging**: JSON-formatted logs with correlation IDs
- **Request Tracing**: Full request/response logging for debugging
- **Error Tracking**: Comprehensive error logging with stack traces
- **Performance Monitoring**: Response time logging for SOAP calls

## Future Improvements

1. **Authentication & Authorization**: Add JWT or OAuth2 support
2. **Caching**: Implement Redis caching for frequently accessed data
3. **Rate Limiting**: Add API rate limiting with Spring Security
4. **Monitoring**: Add Actuator endpoints and Prometheus metrics
5. **Documentation**: Add OpenAPI/Swagger documentation
6. **Circuit Breaker**: Add Hystrix or Resilience4j for fault tolerance
#   P r o l i z W e b S e r v i c e 
 
 
