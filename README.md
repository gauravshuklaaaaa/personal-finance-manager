# Personal Finance Manager

A comprehensive, production-quality RESTful API backend built with Java 17, Spring Boot 3.x, Spring Data JPA, and Spring Security. The application enables users to manage financial transactions, custom categories, savings goals, and generate monthly and yearly analytical financial reports with strict multi-tenant user data isolation and session-based authentication.

---

## 1. Project Overview
The Personal Finance Manager system provides a secure financial API allowing users to:
- Register securely with encrypted password storage.
- Maintain HTTP session-based authentication.
- Perform CRUD operations on income and expense transactions.
- Organize transactions using default categories (Salary, Rent, Food, etc.) or create user-specific custom categories.
- Define savings goals, track real-time goal progress based on net savings, remaining amounts, and percentages.
- Generate aggregated monthly and yearly reports breakdown by category with net savings calculation.
- Guarantee multi-tenant security where each user's financial data is strictly isolated.

---

## 2. Features
- **User Authentication**: Registration, Login, Logout with HTTP Session management and BCrypt hashing.
- **Strict Data Isolation**: IDOR protection preventing access to other users' transactions, categories, goals, or reports.
- **Transaction Management**: Filter by date range, category, type, and sort by newest first. Transaction date is immutable once created.
- **Category Management**: Built-in default categories + per-user custom categories. Prevents deleting referenced categories or global defaults.
- **Savings Goals**: Goal progress calculated automatically via `Total Income - Total Expenses` for active transactions since goal start date.
- **Financial Reports**: Monthly and yearly aggregated reports with category-wise breakdown and net savings.
- **Robust Exception Handling**: Global `@RestControllerAdvice` mapping exceptions to standard HTTP error responses (400, 401, 403, 404, 409).
- **High Test Coverage**: Unit and MockMvc integration tests targeting over 80% code coverage measured via JaCoCo.

---

## 3. Tech Stack
- **Language**: Java 17 / Java 21
- **Framework**: Spring Boot 3.2.3
- **Security**: Spring Security (Session-based Auth, BCrypt)
- **Persistence**: Spring Data JPA, Hibernate
- **Database**: H2 Database (In-Memory for Dev/Tests), PostgreSQL (Production)
- **Validation**: Jakarta Bean Validation
- **Build Tool**: Apache Maven
- **Testing**: JUnit 5, Mockito, Spring Security Test, MockMvc
- **Code Coverage**: JaCoCo Maven Plugin

---

## 4. Architecture
Strict 4-tier Layered Architecture:

```
[ Client ]
    │
    ▼
[ Controller Layer ]   <-- DTOs & Validation
    │
    ▼
[ Service Layer ]      <-- Business Logic & User Isolation
    │
    ▼
[ Repository Layer ]   <-- JPA Repositories & Data Queries
    │
    ▼
[ Database ]           <-- H2 / PostgreSQL
```

- JPA entities are encapsulated and never exposed directly in controllers.
- Data Transfer Objects (DTOs) format all API requests and responses.
- Constructor injection is used across all Spring components.

---

## 5. Database Design
Relational database schema with FK relationships:

- **users**: `id` (PK), `username` (UNIQUE, email), `password` (BCrypt), `full_name`, `phone_number`, `created_at`
- **categories**: `id` (PK), `name`, `type` (INCOME/EXPENSE), `is_custom` (boolean), `user_id` (FK to users, NULL for global default categories). Unique constraint on `(name, user_id)` for custom categories.
- **transactions**: `id` (PK), `amount` (DECIMAL 12,2), `transaction_date`, `category_id` (FK to categories), `description`, `type` (Enum derived from Category), `user_id` (FK to users).
- **savings_goals**: `id` (PK), `goalName`, `target_amount` (DECIMAL 12,2), `target_date`, `start_date`, `user_id` (FK to users).

---

## 6. Authentication
- **Session-Based Authentication**: Implemented via Spring Security HTTP sessions (`JSESSIONID` cookies).
- **Public Endpoints**: `/api/auth/register`, `/api/auth/login`, `/api/health`, `/h2-console/**`.
- **Protected Endpoints**: All `/api/**` endpoints require an active authenticated session.
- **Session Protection**: Session fixation protection enabled (`changeSessionId()`).
- **Logout**: `/api/auth/logout` invalidates session, clears security context, and clears `JSESSIONID` cookie.

---

## 7. API Endpoints

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| `POST` | `/api/auth/register` | Register new user | No |
| `POST` | `/api/auth/login` | Authenticate user & start session | No |
| `POST` | `/api/auth/logout` | End session & logout | Yes |
| `GET` | `/api/categories` | Get all visible categories | Yes |
| `POST` | `/api/categories` | Create custom category | Yes |
| `DELETE` | `/api/categories/{name}` | Delete custom category | Yes |
| `POST` | `/api/transactions` | Create financial transaction | Yes |
| `GET` | `/api/transactions` | Get transactions (filtered/sorted) | Yes |
| `PUT` | `/api/transactions/{id}` | Update transaction (date immutable) | Yes |
| `DELETE` | `/api/transactions/{id}` | Delete transaction | Yes |
| `POST` | `/api/goals` | Create savings goal | Yes |
| `GET` | `/api/goals` | Get all savings goals with progress | Yes |
| `GET` | `/api/goals/{id}` | Get goal by ID | Yes |
| `PUT` | `/api/goals/{id}` | Update savings goal | Yes |
| `DELETE` | `/api/goals/{id}` | Delete savings goal | Yes |
| `GET` | `/api/reports/monthly/{year}/{month}` | Monthly category breakdown & net savings | Yes |
| `GET` | `/api/reports/yearly/{year}` | Yearly category breakdown & net savings | Yes |
| `GET` | `/api/health` | Health check endpoint | No |

---

## 8. Request/Response Examples

### User Registration
`POST /api/auth/register`
Request:
```json
{
  "username": "user@example.com",
  "password": "password123",
  "fullName": "John Doe",
  "phoneNumber": "+1234567890"
}
```
Response (`201 Created`):
```json
{
  "message": "User registered successfully",
  "userId": 1
}
```

### Create Transaction
`POST /api/transactions`
Request:
```json
{
  "amount": 50000.00,
  "date": "2024-01-15",
  "category": "Salary",
  "description": "January Salary"
}
```
Response (`201 Created`):
```json
{
  "id": 1,
  "amount": 50000.00,
  "date": "2024-01-15",
  "category": "Salary",
  "description": "January Salary",
  "type": "INCOME"
}
```

### Create Savings Goal
`POST /api/goals`
Request:
```json
{
  "goalName": "Emergency Fund",
  "targetAmount": 5000.00,
  "targetDate": "2026-01-01",
  "startDate": "2025-01-01"
}
```
Response (`201 Created`):
```json
{
  "id": 1,
  "goalName": "Emergency Fund",
  "targetAmount": 5000.00,
  "targetDate": "2026-01-01",
  "startDate": "2025-01-01",
  "currentProgress": 1000.00,
  "progressPercentage": 20.0,
  "remainingAmount": 4000.00
}
```

### Monthly Report
`GET /api/reports/monthly/2024/1`
Response (`200 OK`):
```json
{
  "month": 1,
  "year": 2024,
  "totalIncome": {
    "Salary": 3000.00,
    "Freelance": 500.00
  },
  "totalExpenses": {
    "Food": 400.00,
    "Rent": 1200.00,
    "Transportation": 200.00
  },
  "netSavings": 1700.00
}
```

---

## 9. Validation Rules
- `username`: Valid email format, unique per system.
- `password`: Non-blank, minimum 6 characters.
- `phoneNumber`: Valid phone format (regex `^\+?[0-9]{7,15}$`).
- `amount`: Positive (`> 0.00`), double precision avoided via `BigDecimal`.
- `date`: `YYYY-MM-DD`, cannot be in the future for transactions.
- `targetDate`: Must be in the future for savings goals.
- `category`: Must exist and be accessible to current user. Cannot delete category referenced by existing transactions. Default categories (Salary, Food, Rent, etc.) cannot be deleted or modified.
- `transaction update`: The `date` field is strictly immutable and cannot be changed.

---

## 10. Error Handling
Consistent JSON error response payload:
```json
{
  "timestamp": "2024-01-15T10:00:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Amount must be positive",
  "path": "/api/transactions"
}
```

HTTP Status Codes:
- `400 Bad Request`: Input validation failed, immutable field modification attempt, invalid date range.
- `401 Unauthorized`: Unauthenticated request or invalid credentials.
- `403 Forbidden`: Attempting to access/modify another user's resource (IDOR prevention).
- `404 Not Found`: Requested resource does not exist.
- `409 Conflict`: Duplicate username or custom category name.

---

## 11. Local Setup
Prerequisites:
- Java 17 or higher
- Maven 3.8+

Clone the repository and build:
```bash
mvn clean install
```

---

## 12. Environment Variables
See `.env.example` for details:
- `DATABASE_URL`: PostgreSQL connection string (Production)
- `DB_USERNAME`: Database username
- `DB_PASSWORD`: Database password
- `PORT`: Server port (default: 8080)

---

## 13. Running the Application
Run locally with default H2 in-memory profile:
```bash
mvn spring-boot:run
```
Application starts at `http://localhost:8080`.
H2 Console available at `http://localhost:8080/h2-console`.

---

## 14. Running Tests
Run unit and integration tests:
```bash
mvn clean test
```

---

## 15. Code Coverage
JaCoCo Maven plugin is configured with an 80% instruction coverage verification rule.
Generate HTML report:
```bash
mvn test jacoco:report
```
Report generated at `target/site/jacoco/index.html`.

---

## 16. API Testing
A complete Postman collection is included in the project root:
- `Personal_Finance_Manager.postman_collection.json`
Import this file into Postman to test all API endpoints with session cookies.

---

## 17. Deployment
The application is deployment-ready for Render, Heroku, or any Docker/K8s platform.
Includes:
- `Dockerfile` (Multi-stage build using Eclipse Temurin Java 17)
- `render.yaml` (Render Blueprint definition for Spring Boot + PostgreSQL)

---

## 18. Render Deployment Instructions
1. Push project repository to GitHub.
2. Log into Render dashboard (https://render.com).
3. Select **New + -> Blueprint**.
4. Connect your GitHub repository.
5. Render will automatically detect `render.yaml`, provision a free PostgreSQL database and deploy the Web Service.

---

## 19. Security Decisions
- **Session Authentication**: Used Spring Security HTTP Sessions with JSESSIONID cookies per explicit requirements.
- **CSRF Configuration**: CSRF protection is disabled for REST endpoints since session cookies are intended for API consumers, and endpoints validate content type and credentials explicitly.
- **User Data Isolation**: Every SQL query and service operation checks `user_id` against `SecurityContextHolder` to prevent IDOR attacks.

---

## 20. Design Decisions
- **Global vs Custom Categories**: Global default categories have `user_id = NULL`. Custom categories store `user_id`. Queries combine `user_id = NULL OR user_id = :currentUser`.
- **Goal Progress Calculation**: Goal savings are computed dynamically using net savings (`Income - Expense`) from non-deleted transactions created on or after the goal's `startDate`. Edge cases like negative savings or zero targets return clean formatted numeric values without NaN or Infinity.

---

## 21. Project Structure
```
src/main/java/com/example/personalfinancemanager/
├── PersonalFinanceManagerApplication.java
├── config/
│   ├── DataInitializer.java
│   └── SecurityConfig.java
├── controller/
│   ├── AuthController.java
│   ├── CategoryController.java
│   ├── HealthController.java
│   ├── ReportController.java
│   ├── SavingsGoalController.java
│   └── TransactionController.java
├── dto/
│   ├── ErrorResponse.java
│   ├── auth/
│   ├── category/
│   ├── goal/
│   ├── report/
│   └── transaction/
├── entity/
│   ├── Category.java
│   ├── SavingsGoal.java
│   ├── Transaction.java
│   ├── TransactionType.java
│   └── User.java
├── exception/
│   ├── ConflictException.java
│   ├── ForbiddenException.java
│   ├── GlobalExceptionHandler.java
│   ├── ResourceNotFoundException.java
│   ├── UnauthorizedException.java
│   └── ValidationException.java
├── repository/
│   ├── CategoryRepository.java
│   ├── SavingsGoalRepository.java
│   ├── TransactionRepository.java
│   └── UserRepository.java
├── security/
│   ├── CustomUserDetailsService.java
│   └── UserPrincipal.java
└── service/
    ├── CategoryService.java
    ├── ReportService.java
    ├── SavingsGoalService.java
    ├── TransactionService.java
    └── UserService.java
```

---

## 22. Future Improvements
- Add JWT token authentication option for mobile clients.
- Add recurring automated transaction support.
- Implement CSV/PDF financial statement export.
