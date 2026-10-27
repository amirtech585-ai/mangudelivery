# ManguDelivery — Implementation Decisions

Based on: ManguDelivery Version 4.0 Project Documentation.

## 1. Account model: one account per role (confirmed by owner)

- A person may hold several accounts, one per role (Buyer, Seller, Rider).
- A seller or rider can log out and register a new Buyer account (or any other role).
- Uniqueness is enforced on (email, role). The same email may be used for different roles,
  so login requires the role: POST /api/auth/login { email, password, role }.
- No account changes role in place. This keeps Section 7.4 of the documentation intact:
  a role never changes because of a local setting or an edited app.
- Logout is client-side: the app discards its token. Server-side token revocation is not
  implemented yet (see Section 4).

## 2. Approval rules (unchanged from documentation)

- Buyers: ACTIVE immediately.
- Sellers and riders: PENDING_APPROVAL until an admin approves. Only ACTIVE accounts may
  create products (seller) or see jobs (rider). Every business endpoint re-checks the
  account status in the database, not only the token.
- Rejected applications keep the reason. The applicant may resubmit via
  POST /api/partner/application.

## 3. Security baseline

- The backend is the only component with database access.
- Every protected endpoint is restricted by role in SecurityConfig and by account status
  in the service layer.
- Admin accounts are created at startup from environment variables (ADMIN_EMAIL,
  ADMIN_PASSWORD). They cannot be self-registered.
- Passwords are hashed with BCrypt. JWTs are signed with JWT_SECRET (at least 32 bytes).

## 4. Status against documentation roadmap (Section 16)

| Area | Status | Documentation phase |
|---|---|---|
| Auth, roles, approvals, basic products | Foundation implemented | 2–3 |
| Admin: application review | Foundation implemented | 4 |
| Mobile: register, login, logout, status screens, role home screens | Foundation implemented | 5 |
| Buyer: cart, checkout, orders | Not implemented | 6 |
| Seller: order processing, inventory, earnings | Not implemented | 7 |
| Rider: dispatch, job acceptance, GPS, PIN delivery | Stub endpoint only | 8 |
| Payments (MTN/Airtel/card/cash), webhooks | Not implemented | 9 |
| WebSockets / FCM push | Not implemented (admin polls every 15 s) | 9 |
| Token revocation, admin MFA | Not implemented | 3 / 13 |
| Audit log viewer, disputes, settlements | Audit writes only; no UI | 6 / 9 |

## 4a. Change log

| Date | Change | Review issue |
|---|---|---|
| 24 Oct 2026 | JWT secret and DB password no longer have defaults; startup fails without them | I01, I08 |
| 24 Oct 2026 | Approval and rejection require the account to be PENDING_APPROVAL | I02 |
| 24 Oct 2026 | Product price minimum is UGX 1; mobile validates price and stock before sending | I03 |
| 24 Oct 2026 | Field-level validation messages returned as HTTP 400 | I07 |
| 24 Oct 2026 | Registration conflict message is generic | I09 |
| 24 Oct 2026 | Login rate limited to 10 attempts per 15 minutes per role and email | I06 |
| 24 Oct 2026 | Anonymous requests to protected routes return 401 | I10 |
| 24 Oct 2026 | Rejected sellers and riders can correct and resubmit in the mobile app | Gap G20 |
| 24 Oct 2026 | Administrator can suspend and reinstate accounts; account list and audit log in admin web | Gaps G11, G13 |

| 24 Oct 2026 | Order loop built: cart and checkout (cash on delivery, one seller per order), seller accept/reject/ready, rider accept/release/pickup/on-the-way/PIN delivery/fail, admin cancel, order history, and ledger entries on completion | Phase 6-8 foundation |

Cash on delivery is the only payment method. Commission (5%), service fee (UGX 1,000), and delivery fee (UGX 3,000) are configurable through environment variables and are proposed defaults until the owner confirms them (Appendix A, D01).

Not yet changed: I04 (migrations), I05 (token revocation), I11, I12, zones and dispatch by zone, push notifications, GPS, and digital payments.

## 5. Open items from documentation Appendix A

Payment provider credentials, map provider, hosting, file storage, commission amounts,
and privacy retention rules are still undecided. None are hard-coded.
