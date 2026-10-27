# ManguDelivery Backend (Spring Boot)

Java 17, Spring Boot 3.2, PostgreSQL 16, JWT authentication.

## Run locally

```bash
# 1. Start PostgreSQL (from the project root)
docker compose up -d

# 2. Start the API
cd backend
export ADMIN_EMAIL=admin@mangu.local
export ADMIN_PASSWORD='change-me-strong-password'
export JWT_SECRET='replace-with-a-random-string-of-at-least-32-characters'
export DB_PASSWORD=mangu   # must match docker-compose.yml for local development
mvn spring-boot:run
```

API base: http://localhost:8080

## Main endpoints

| Method | Path | Who | Purpose |
|---|---|---|---|
| POST | /api/auth/register | Anyone | Create a BUYER, SELLER or RIDER account |
| POST | /api/auth/login | Anyone | Log in with email, password and role |
| POST | /api/auth/logout | Anyone | Client discards token (no server state) |
| GET | /api/me | Logged in | Account status and latest application status |
| POST | /api/partner/application | SELLER, RIDER | Submit or resubmit an application |
| GET | /api/admin/applications?status=PENDING | ADMIN | List applications |
| POST | /api/admin/applications/{id}/approve | ADMIN | Approve |
| POST | /api/admin/applications/{id}/reject | ADMIN | Reject with reason `{ "reason": "..." }` |
| GET | /api/buyer/products | BUYER (active) | Listings from approved sellers |
| GET / POST | /api/seller/products | SELLER (approved) | List own products / create product |
| GET | /api/rider/jobs | RIDER (approved) | Placeholder, returns empty list |
| GET | /api/admin/users | ADMIN | List all accounts |
| POST | /api/admin/users/{id}/suspend | ADMIN | Suspend with `{ \"reason\": \"...\" }` |
| POST | /api/admin/users/{id}/reinstate | ADMIN | Restore status from application history |
| GET | /api/admin/audit | ADMIN | Latest 200 audit entries |

## Notes

- `JWT_SECRET` and `DB_PASSWORD` have no defaults. The server will not start without them.
- Login is limited to 10 attempts per role and email in 15 minutes (in-process; move to Redis before running several instances).
- Validation errors return HTTP 400 with a `message` listing each invalid field.
- `ddl-auto` defaults to `update` for development. Use migrations before production.
- Tests are not yet written; compile and run with Maven once it is installed.
