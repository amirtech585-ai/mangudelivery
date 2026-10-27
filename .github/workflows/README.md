# ManguDelivery

A marketplace and delivery platform for Masaka City, Uganda. Built from the
ManguDelivery Version 4.0 Project Documentation.

## Structure

| Folder | What it is | Technology |
|---|---|---|
| `backend/` | Single API and business-logic layer. The only component that touches the database. | Java 17, Spring Boot 3.2, PostgreSQL |
| `admin-web/` | Standalone administrator platform (approvals, monitoring). | React 18, Vite |
| `mobile-app/` | One unified app with Buyer, Seller, and Rider roles. | Flutter / Dart |
| `docs/DECISIONS.md` | Implementation decisions and roadmap status. | |
| `docker-compose.yml` | Local PostgreSQL. | |

## Quick start

1. **Database**: `docker compose up -d`
2. **Backend**: see `backend/README.md`. Set `ADMIN_EMAIL`, `ADMIN_PASSWORD`, and `JWT_SECRET`.
3. **Admin web**: `cd admin-web && npm install && npm run dev` then open http://localhost:5173
4. **Mobile app**: see `mobile-app/README.md`

## Account rules

- Buyers register and use the app at once.
- Sellers and riders register, then wait for administrator approval.
- A person can hold one account per role. A seller can log out and create a buyer account.
- Accounts for different roles are separate. Login requires the role.

## Status

This is a foundation, not a finished product. See `docs/DECISIONS.md` section 4 for what is implemented
and which roadmap phases remain (orders, dispatch, payments, real-time events).

Verification: the backend, admin web, and mobile code were written but not compiled or run in the
build environment, which had no Maven, Flutter, or network access. Run the build steps above and fix any
issues before relying on the code.
