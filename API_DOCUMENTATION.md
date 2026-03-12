# Mini Task Management System - API Documentation

## Base Information

- Base URL: `http://localhost:8080/api/v1`
- Auth type: `Bearer JWT`
- Roles: `USER`, `ADMIN`
- Content type: `application/json`

Use this header for protected endpoints:

```http
Authorization: Bearer <jwt-token>
```

---

## Authentication Endpoints (Public)

### 1) Register User

- **Method:** `POST`
- **Path:** `/auth/register`
- **Access:** Public

Request body:

```json
{
  "firstName": "John",
  "lastName": "Doe",
  "email": "john@example.com",
  "password": "password123",
  "confirmPassword": "password123"
}
```

Validation rules:

- `firstName`: required, 2-50 chars
- `lastName`: required, 2-50 chars
- `email`: required, valid email
- `password`: required, 6-100 chars
- `confirmPassword`: required (must match `password` in service logic)

Success response: `201 Created`

```json
{
  "token": "eyJhbGciOiJIUzUxMiJ9...",
  "type": "Bearer",
  "userId": 1,
  "email": "john@example.com",
  "firstName": "John",
  "lastName": "Doe",
  "role": "USER"
}
```

Common errors:

- `400 Bad Request` - validation error
- `401 Unauthorized` - email already exists or password mismatch (current implementation)

### 2) Login

- **Method:** `POST`
- **Path:** `/auth/login`
- **Access:** Public

Request body:

```json
{
  "email": "john@example.com",
  "password": "password123"
}
```

Success response: `200 OK`

```json
{
  "token": "eyJhbGciOiJIUzUxMiJ9...",
  "type": "Bearer",
  "userId": 1,
  "email": "john@example.com",
  "firstName": "John",
  "lastName": "Doe",
  "role": "USER"
}
```

Common errors:

- `400 Bad Request` - validation error
- `401 Unauthorized` - invalid credentials

---

## Task Endpoints (`USER` and `ADMIN`)

Base path: `/tasks`

### Task model

`TaskRequestDTO` fields:

- `title` (required)
- `description` (optional)
- `status` (required: `TODO`, `IN_PROGRESS`, `DONE`)
- `priority` (required: `LOW`, `MEDIUM`, `HIGH`)
- `dueDate` (required, format `yyyy-MM-dd'T'HH:mm:ss`)

`TaskResponseDTO` fields:

- `id`, `title`, `description`, `status`, `priority`, `dueDate`, `createdAt`, `updatedAt`

### 1) Create Task

- **Method:** `POST`
- **Path:** `/tasks`
- **Access:** `USER`, `ADMIN`

Request body:

```json
{
  "title": "Complete report",
  "description": "Prepare Q1 report",
  "status": "TODO",
  "priority": "HIGH",
  "dueDate": "2026-03-20T18:00:00"
}
```

Success response: `201 Created`

### 2) Get All Tasks (Pagination + Filtering + Sorting)

- **Method:** `GET`
- **Path:** `/tasks`
- **Access:** `USER`, `ADMIN`

Query params:

- `page` default `0`
- `size` default `10`
- `sortBy` default `dueDate` (also supports `priority`)
- `sortDirection` default `asc`
- `status` optional
- `priority` optional

Example:

```http
GET /api/v1/tasks?page=0&size=10&sortBy=dueDate&sortDirection=desc&status=TODO&priority=HIGH
```

Behavior:

- `USER`: sees only own tasks
- `ADMIN`: sees all tasks (with optional filters)

Success response: `200 OK` (Spring `Page<TaskResponseDTO>`)

### 3) Get Task By ID

- **Method:** `GET`
- **Path:** `/tasks/{id}`
- **Access:** `USER`, `ADMIN`

Success response: `200 OK`

Current behavior note:

- This endpoint is owner-scoped in service logic.
- If task exists but belongs to another user, response is `404`.

### 4) Update Task

- **Method:** `PUT`
- **Path:** `/tasks/{id}`
- **Access:** `USER`, `ADMIN`

Request body: same as create

Success response: `200 OK`

Current behavior note:

- Owner-scoped (`404` for non-owner).

### 5) Delete Task

- **Method:** `DELETE`
- **Path:** `/tasks/{id}`
- **Access:** `USER`, `ADMIN`

Success response: `204 No Content`

Current behavior note:

- Owner-scoped (`404` for non-owner).

### 6) Mark Task as Completed

- **Method:** `PATCH`
- **Path:** `/tasks/{id}/complete`
- **Access:** `USER`, `ADMIN`

Behavior:

- Sets `status` to `DONE`

Success response: `200 OK`

Current behavior note:

- Owner-scoped (`404` for non-owner).

---

## Admin User Endpoints (`ADMIN` only)

Base path: `/admin/users`

All endpoints in this section require admin token.

### 1) Register Admin

- **Method:** `POST`
- **Path:** `/admin/users/register`

Request body: same as `/auth/register`

Success response: `201 Created`

Response model: `AuthResponse` with `role: ADMIN`

### 2) Get All Users

- **Method:** `GET`
- **Path:** `/admin/users`

Query params:

- `page` default `0`
- `size` default `10`

Success response: `200 OK` (`Page<AuthResponse>`)

### 3) Get User By ID

- **Method:** `GET`
- **Path:** `/admin/users/{userId}`

Success response: `200 OK`

### 4) Change User Role

- **Method:** `PATCH`
- **Path:** `/admin/users/{userId}/role`
- **Query param:** `role=ADMIN|USER`

Example:

```http
PATCH /api/v1/admin/users/5/role?role=ADMIN
```

Success response: `200 OK`

Business rule:

- Cannot demote the last remaining `ADMIN`

### 5) Delete User

- **Method:** `DELETE`
- **Path:** `/admin/users/{userId}`

Success response: `204 No Content`

Business rules:

- Admin cannot delete own account
- Cannot delete the last remaining `ADMIN`

### 6) Get Current Admin Info

- **Method:** `GET`
- **Path:** `/admin/users/me`

Success response: `200 OK`

---

## Security and RBAC Summary

From `SecurityConfig`:

- Public:
  - `POST /api/v1/auth/register`
  - `POST /api/v1/auth/login`
  - `OPTIONS /**`
- `ADMIN` only:
  - `/api/v1/admin/**`
- `USER` or `ADMIN`:
  - task endpoints under `/api/v1/tasks/**`
- Any other `/api/v1/**` requires authentication

Method-level protection:

- `AdminUserController` is annotated with `@PreAuthorize("hasRole('ADMIN')")`

---

## CORS

CORS for API routes allows:

- Origin patterns: `*`
- Methods: `GET, POST, PUT, DELETE, PATCH, OPTIONS`
- Headers: `*`
- Exposed headers: `Authorization`

Applied to `/api/**`.

---

## First-Run Default Admin

On startup, `DefaultAdminInitializer` can create one admin user if none exists.

Config keys (in `application.properties`):

```properties
app.bootstrap.admin.enabled=true
app.bootstrap.admin.first-name=Super
app.bootstrap.admin.last-name=Admin
app.bootstrap.admin.email=admin@gmail.com
app.bootstrap.admin.password=Admin@123
```

Behavior:

- Runs once at app start
- Skips creation if any admin already exists
- Skips creation if configured email already exists as a non-admin user

---

## Error Response Format

### Application/validation errors (`GlobalExceptionHandler`)

```json
{
  "status": 400,
  "message": "title: Title is required",
  "timestamp": "2026-03-12T10:00:00",
  "path": "/api/v1/tasks"
}
```

### Security errors (`SecurityConfig` handlers)

401 example:

```json
{
  "status": 401,
  "message": "Authentication required",
  "path": "/api/v1/tasks"
}
```

403 example:

```json
{
  "status": 403,
  "message": "Access denied",
  "path": "/api/v1/admin/users"
}
```

---

## HTTP Status Codes Used

- `200 OK`
- `201 Created`
- `204 No Content`
- `400 Bad Request`
- `401 Unauthorized`
- `403 Forbidden`
- `404 Not Found`
- `500 Internal Server Error`

---

## Quick cURL Examples

Register user:

```bash
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "firstName": "John",
    "lastName": "Doe",
    "email": "john@example.com",
    "password": "password123",
    "confirmPassword": "password123"
  }'
```

Login:

```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"john@example.com","password":"password123"}'
```

Create task:

```bash
curl -X POST http://localhost:8080/api/v1/tasks \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{
    "title": "Learn Spring Security",
    "description": "Complete JWT module",
    "status": "TODO",
    "priority": "HIGH",
    "dueDate": "2026-03-25T18:00:00"
  }'
```

Admin register admin:

```bash
curl -X POST http://localhost:8080/api/v1/admin/users/register \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <admin_token>" \
  -d '{
    "firstName": "Alice",
    "lastName": "Admin",
    "email": "alice.admin@example.com",
    "password": "password123",
    "confirmPassword": "password123"
  }'
```
