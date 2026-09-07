# Kittyp — Master Cursor Fix Brief (flawless handoff)

Generated: 2026-09-06 IST (KittyP Bot STA + overnight audit)
Product: https://www.kittyp.in
Backend: https://kittyp-be-production.up.railway.app
Companion docs: KITTYP_AUDIT_NOTES.md, STA_E2E.md, CURSOR_FIX_PACK.md, BUSINESS_GROWTH.md, PERF_TIMINGS.json, PAYMENT_API_SURFACE.md

## 0. How Cursor must work this pack (read first)

You are fixing a production veterinary clinic CRM used in India. Do not assume root causes. For every issue:

1. Reproduce from the steps below (or equivalent in staging).
2. Investigate frontend + backend yourself (network calls, validators, auth/session, feature flags, role checks). Discard any hypothesis in this doc if evidence contradicts it.
3. Fix the real cause; prefer small, reviewable PRs grouped by theme (security → clinical loop → billing → polish → growth).
4. Prove success against the Success criteria for that issue (automated test and/or manual checklist).
5. Do not add exploit PoCs, weaken auth, skip signature checks, or leave Swagger open in prod.
6. Do not invent repo structure — discover it. If FE/BE are separate repos, open separate PRs.
7. Timezone for all “Today” UI: Asia/Kolkata unless the clinic explicitly sets another IANA zone.
8. Passwords/secrets: never commit test passwords or OTP dumps.

**Definition of done for this whole pack:**
The clinical money loop works end-to-end for a verified clinic:
clinic/staff can add client+pet + appointment → doctor can Open chart → Attend → Finish → invoice linked to that visit → Collect payment opens Razorpay → parent can see unpaid/paid records and cancel/reschedule upcoming bookings → security headers + Swagger locked in prod → brand pages are vet CRM not litter → /pricing exists.

If something cannot be fixed without a product decision, implement the recommended default listed under “Product answers” and note the assumption in the PR.

## 1. Answers to the founder’s questions (in this note)

### Q1. What is broken / what should we fix?
See §3 Issue catalog. Highest impact for “make money + trust”:

- Clinic-admin Add client / Add appointment no-ops (staff works on same clinic)
- Doctor Open chart no-op (blocks Attend/Finish)
- Client email validator rejects valid emails
- Today date off-by-one (shows Sep 5 when IST is Sep 6)
- Production Swagger + missing security headers
- Stale litter brand pages + broken OG/manifest
- Doctor email OTP silent failure on throwaway signup
- Parent missing cancel/reschedule and self-pay
- Doctor Sign Out can stick (session bounce to /doctor)

### Q2. Rigorous security findings (defensive only)

| Finding | Evidence | Required fix |
|---|---|---|
| Swagger UI public on prod | GET /swagger-ui/index.html → 200; /v3/api-docs/swagger-config → 200 | Disable SpringDoc in prod |
| /v3/api-docs 500 | Still intended exposure | Same |
| /health + /actuator/health public | 200 | Keep only if Railway needs it; hide details; lock other actuator endpoints |
| Missing CSP, XFO/frame-ancestors, nosniff, Referrer-Policy, Permissions-Policy on Netlify HTML | Header scan | Add `_headers` |
| SPA serves index.html for /.env etc. | 200 HTML | 404 sensitive paths |
| Unauth sensitive API | Clinic/doctor/invoice/razorpay → 401 (good baseline) | Keep; add integration tests |
| CORS rejects evil origin | Observed | Keep |
| Razorpay verify | Endpoints exist; must confirm HMAC signature + amount server-side | Implement/verify; tests with bad signature fail |
| OTP | Rate limit / lockout / no OTP in logs — verify; doctor verify silent-fails | Clear API errors + client toasts |
| Session isolation | Parent Logout visible on doctor signup; multi-role /doctor→/app for admin | Isolate role signup sessions |
| Pending clinic could bill | Billing unlocked while invites locked (earlier) | Lock mutating billing until verified or document intentional |

### Q3. How do we grow the business?
Full plan: BUSINESS_GROWTH.md. Summary Cursor should help ship:

- Publish /pricing with INR tiers (see Q5).
- Fix the 90-second loop: appointment → chart → invoice → PDF/WhatsApp.
- Keep pet parents free; monetize clinics.
- India metro GTM: Bengaluru, Hyderabad, Pune first; then Mumbai/Delhi-NCR/Chennai.
- Narrative: replace paper register; GST PDF + UPI/Razorpay day one.
- Kill litter ecommerce leftovers — they destroy trust.

### Q4. Which countries to expand to (order)?
1. India (metros → Tier-2) — now.
2. UAE / GCC (Dubai first) — after India retention + multi-currency/VAT readiness.
3. Singapore / Malaysia — English + pet spend.
4. Later: Saudi, selected SE Asia.

Do not expand geography before the India clinical loop is flawless and /pricing converts.

### Q5. Subscription types & how to earn money?
Monetize clinics (B2B). Parents free.

| Tier | Price (ex-GST) | Gate |
|---|---|---|
| Pilot | ₹0 / 21 days | Visit + WA caps |
| Starter | ₹999/mo or ₹9,999/yr | 5 seats, 200 WA/mo |
| Clinic | ₹2,499/mo or ₹24,999/yr | Unlimited seats, inventory, 1500 WA |
| Hospital | ₹6,999/mo or ₹69,999/yr | Multi-location light, IPD light |
| Enterprise | Custom | Chains |

- Do not gate: consult chart, basic GST invoice PDF, pet/parent create.
- Do gate: seats (early), locations, WhatsApp volume, inventory depth, analytics, IPD.
- No Vetlify-style ₹22.5k registration fee. Use Razorpay Subscriptions + GST invoice to clinic.
- Cursor: implement public /pricing page + feature flags/entitlements scaffold (even if billing webhook is phase 2).

### Q6. Account types tested

| Role | Accounts exercised | Result summary |
|---|---|---|
| Pet parent | throwaway + rrohan419+parent@gmail.com | Signup, pets, book PASS, Jitsi pre-join, no cancel/pay |
| Doctor | throwaway (OTP fail) + jhondoe@gmail.com | Dashboard OK; Open chart FAIL; availability partial |
| Clinic admin | throwaway Audit Night + prashant@gmail.com | Verify gates; Add client/appt FAIL on Prashant; email validator FAIL on Audit Night; Razorpay collect PASS |
| Staff | staff@gmail.com | Add client/appt PASS; Collect payment PASS; Settings denied |
| Super admin | rrohan419@gmail.com (works); spwankhede007@gmail.com invalid w/ Test@123 | Clinics verify, user search PASS |

## 2. Environment & constraints for implementers

- Frontend: Netlify SPA (www.kittyp.in), large main JS (~2.9MB).
- Backend: Railway Spring Boot-style API `/api/v1`.
- Auth: Firebase project kittyp-prod + custom API.
- Payments: Razorpay (checkout.razorpay.com); Test Mode observed.
- Skip SMS in QA; email OTP must work.
- Do not complete live charges in shared test accounts.

## 3. Issue catalog (fix everything)

Priority: P0 ship-blocking / security / trust · P1 core product · P2 polish/growth.

For each: Cursor must investigate; “Likely area” is a hint only.

### P0-01 — Clinic admin Add appointment is a no-op
- Repro: Login prashant@gmail.com → /clinic/appointments → click Add appointment.
- Observed: No modal, spinner, toast; board stays Waitlist/With doctor/Checkout = 0.
- Contrast: Same clinic, staff@gmail.com Add appointment opens full modal.
- Success: Clinic admin opens the same modal staff sees; creating Here-now/scheduled appt for existing pet+doctor persists and appears on board.
- Hint (non-binding): role permission flag, dead onClick, feature gate, or overlay blocker only on admin layout.

### P0-02 — Clinic admin Add client is a no-op
- Repro: Prashant → Clients & Pets → Add client.
- Observed: No form. Staff does get full form.
- Success: Admin can create client+pet; validation messages are accurate.

### P0-03 — Doctor Open chart / appointment row no-op
- Repro: jhondoe@gmail.com → Appointments showing pokey With doctor → Open chart / row click.
- Observed: No navigation/modal; no Attend/Finish.
- Success: Chart opens; Attend available; notes save; Finish completes visit; time not silently overwritten incorrectly.
- Retest after: staff-created appointment (see latest STA addendum).

### P0-04 — Client email validator false negative
- Repro: Audit Night Clinic → Add client with walkin2@gmail.com or sta.walkin2@example.com.
- Observed: “Valid email is required.”
- Success: RFC-valid emails accepted; only truly invalid rejected; show field-level error text matching server.

### P0-05 — Calendar “Today” wrong (Asia/Kolkata)
- Repro: Parent/doctor dashboards on IST Sunday 2026-09-06.
- Observed: “Saturday, September 5” (parent); doctor week highlights Sat Sep 5 while slots can say Sun Sep 6.
- Success: “Today” matches Asia/Kolkata civil date; appointment day filters align.

### P0-06 — Disable production Swagger / api-docs
- Repro: Open backend swagger-ui URL (see audit).
- Success: 401/404 in prod; config flag off; CI check fails if re-enabled.

### P0-07 — Netlify security headers
- Success: CSP (after host inventory), X-Content-Type-Options: nosniff, frame ancestors, Referrer-Policy, Permissions-Policy on HTML; HSTS retained.

### P0-08 — SPA 404 for sensitive paths
- Success: /.env, /.git/HEAD etc. → real 404, not index.html.

### P0-09 — Stale litter branding
- Pages: /about, /contact, mailto @kittyp.com vs @kittyp.in, US phone, personal socials.
- Success: Copy is India vet CRM; contact uses @kittyp.in; no litter ecommerce narrative.

### P0-10 — Manifest + OG image
- Observed: Manifest “Eco-Friendly Pet Products”; og:image /catlitter HTML.
- Success: Vet CRM manifest; OG image is a real image URL; remove personal gmail from meta author.

### P0-11 — Doctor email OTP silent failure
- Repro: Clean session doctor signup → Send Email OTP → enter code → Verify.
- Observed: Stays on page; no toast; Verify enabled while OTP empty; parent Logout can appear.
- Success: Invalid/expired OTP shows error; success advances; Verify disabled until 6 digits; no foreign session chrome.

### P0-12 — Doctor Sign Out stuck
- Repro: From doctor portal Sign Out / Logout; visit /login.
- Observed: Can bounce back to /doctor; blocks role switching.
- Success: Sign Out clears Firebase + API tokens; /login stays logged out; switching roles works in one browser.

### P1-13 — Operating hours “Hours not set” / hours route 404
### P1-14 — Doctor availability day picker stuck
### P1-15 — Invoice Bruno sample prefill
### P1-16 — Parent cancel / reschedule missing
### P1-17 — Parent self-pay missing
### P1-18 — Weight “X kg kg”
### P1-19 — Pet parent signup forces /login
### P1-20 — Pending-verification billing unlock
- Success: Mutating invoice/pay APIs match invite/appointment locks until verified (or documented exception with admin audit).

### P1-21 — Doctors list empty-state lie
### P1-22 — Staff vs admin capability asymmetry
- Related to P0-01/02. Confirm intended RBAC matrix in code + tests. Staff Collect payment may be intentional — if not, restrict.

### P1-23 — Saving… + Sending… flash on invoice draft
### P1-24 — Attend overwrites scheduled time
### P1-25 — Clinic OTP email greets “Doctor Applicant”
### P1-26 — Articles empty state / nav
### P1-27 — Bundle size ~2.9MB main JS
### P1-28 — robots/sitemap ecommerce leftovers
### P1-29 — Verified badge vs Gov ID pending (doctor)
### P1-30 — Staff invite UX
### P1-31 — New-patient appointment modal crash (intermittent)
### P1-32 — Admin multi-role routing /doctor → /app
### P1-33 — Future-dated visit in Completed history
### P1-34 — Razorpay verify-payment integrity
- Success: Unit/integration tests: tampered signature/amount rejected; only server amount charged.

### P2-35 — Public /pricing page
### P2-36 — Brand social links not personal handles
### P2-37 — Duplicate `<link rel="manifest">`
### P2-38 — Duplicate staff user rows in admin search
### P2-39 — Client list 0→2 loading flicker
### P2-40 — Phone OTP gates doctor completion — staging bypass flag for QA (never in prod without authz).

## 4. Suggested PR breakdown (Cursor)

- **PR A — Security & trust (backend + Netlify):** P0-06, P0-07, P0-08, P1-34, actuator lockdown.
- **PR B — Brand & SEO (frontend):** P0-09, P0-10, P1-26, P1-28, P2-35, P2-36, P2-37.
- **PR C — Clinical loop (frontend + backend):** P0-01, P0-02, P0-03, P0-04, P0-05, P1-13, P1-14, P1-24, P1-31.
- **PR D — Auth/session/OTP:** P0-11, P0-12, P1-19, P1-25, P1-32, P2-40.
- **PR E — Billing & parent money:** P1-15, P1-16, P1-17, P1-20, P1-23, P1-22 RBAC tests.
- **PR F — Polish:** P1-18, P1-21, P1-29, P1-30, P1-33, P1-27, P2-38, P2-39.

## 5. Ready-to-paste Cursor agent prompts

### Prompt — Frontend
You are fixing Kittyp (www.kittyp.in) frontend. Read CURSOR_MASTER_FIX.md in the audit pack (or pasted issues).

Rules:
- Do NOT assume root causes in the doc. Reproduce, inspect code/network, then fix.
- Make it a success against each issue’s Success criteria.
- Priority order: PR C clinical loop (Add appointment/client for CLINIC ADMIN, doctor Open chart, email validator, Today Asia/Kolkata), then PR D auth/logout/OTP UX, then PR E billing/parent cancel+pay, then PR B brand/pricing, then polish.
- Add regression tests where the stack allows (RTL/Playwright/Cypress — use what exists).
- Do not weaken auth. Do not commit secrets.
- Open a clean PR with checklist mapping issue IDs → commits → how verified.

### Prompt — Backend
You are fixing Kittyp production API (Railway). Read CURSOR_MASTER_FIX.md.

Rules:
- Do NOT assume root causes. Verify with tests.
- Disable SpringDoc/Swagger and lock actuator in prod.
- Ensure OTP verify returns clear 4xx + message; rate limits.
- Ensure Razorpay verify-payment checks signature AND amount server-side; reject tampers.
- Ensure role/permission for clinic admin vs staff for appointments/clients is correct and tested (admin must not be weaker than staff).
- Pending-verification clinic: align mutating invoice/pay with product policy (default: lock until verified).
- Timezone: store Instant/Offset correctly; “clinic day” uses Asia/Kolkata unless overridden.
- PR with issue ID checklist and tests green.

## 6. Manual QA checklist (post-fix)

- [ ] Clinic admin Add client + Add appointment (Prashant or equivalent)
- [ ] Staff same flows still work
- [ ] Doctor Open chart → Attend → note → Finish
- [ ] Invoice tied to that pet/visit (not Bruno)
- [ ] Collect payment → Razorpay Test Mode
- [ ] Parent book + cancel/reschedule
- [ ] Parent weight shows single kg
- [ ] Today = Asia/Kolkata
- [ ] Valid gmail accepted on Add client
- [ ] Doctor Sign Out then login as other role
- [ ] Swagger UI blocked in prod
- [ ] Security headers present
- [ ] /about /contact /pricing correct
- [ ] Doctor email OTP success path on staging

## 7. Latest extra testing addendum (2026-09-06 ~15:37 IST)

### Staff → doctor critical path

| Step | Result | Detail |
|---|---|---|
| Staff creates Here-now appt for pokey + Jhon Doe | PASS | Appears With doctor, time 3:10 AM |
| Staff Sign Out | PASS | Session cleared |
| Doctor sees appointment | PASS | “Routine visit: pokey · Cat”, With doctor, 3:10 AM |
| Doctor Open chart / row click | FAIL | No chart; one stale DOM error observed |
| Doctor note + Finish | BLOCKED | Could not run |
| Doctor Sign Out | FAIL | Stays authenticated; /login and /login?logout=1 redirect to doctor portal |

Implication for Cursor: P0-03 is not “missing appointment data” — appointment exists and is With doctor after staff create. Fix chart navigation/handlers. P0-12 logout is confirmed blocker for multi-role QA and real multi-account browsers.

Do not assume the 3:10 AM display is correct wall-clock; investigate timezone (P0-05) separately from Open chart.

### Success gate for clinical loop PR
1. Staff (or clinic admin) creates Here-now appt → visible With doctor.
2. Doctor Open chart works on that row.
3. Attend → note → Finish works.
4. Sign Out returns to logged-out /login and stays there on refresh.
