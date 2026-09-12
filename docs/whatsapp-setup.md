# WhatsApp Cloud API setup (KittyP)

## Env / properties

| Property | Env | Description |
|----------|-----|-------------|
| `whatsapp.enabled` | `WHATSAPP_ENABLED` | `true` when ready to send (default `false`) |
| `whatsapp.api-version` | `WHATSAPP_API_VERSION` | e.g. `v21.0` |
| `whatsapp.invoice-template` | `WHATSAPP_INVOICE_TEMPLATE` | Template name, default `invoice_receipt` |
| `whatsapp.invoice-template-lang` | `WHATSAPP_INVOICE_TEMPLATE_LANG` | Default `en` |
| `whatsapp.vaccine-template` | `WHATSAPP_VACCINE_TEMPLATE` | Phase 2, default `vaccine_reminder` |
| `whatsapp.checkup-template` | `WHATSAPP_CHECKUP_TEMPLATE` | Phase 2, default `checkup_reminder` |
| `whatsapp.promo-template` | `WHATSAPP_PROMO_TEMPLATE` | Phase 2, default `promo_offer` |
| `whatsapp.default-country-code` | `WHATSAPP_DEFAULT_COUNTRY_CODE` | Default `91` for 10-digit IN numbers |
| `whatsapp.meta-app-id` | `WHATSAPP_META_APP_ID` | KittyP Meta App ID (public to FE for Embedded Signup + template sample upload) |
| `whatsapp.meta-app-secret` | `WHATSAPP_META_APP_SECRET` | **Server only** — code exchange for Embedded Signup |
| `whatsapp.embedded-signup-config-id` | `WHATSAPP_EMBEDDED_SIGNUP_CONFIG_ID` | Embedded Signup `config_id` from Meta |
| `whatsapp.webhook-verify-token` | `WHATSAPP_WEBHOOK_VERIFY_TOKEN` | Hub verify token for `/api/v1/webhook/whatsapp` |
| `whatsapp.register-pin` | `WHATSAPP_REGISTER_PIN` | Optional 6-digit PIN for Cloud API phone register |
| `app.crypto.secret` | `APP_CRYPTO_SECRET` | AES key material for WhatsApp tokens at rest (prefer dedicated secret, not JWT) |

Public FE config (never exposes secret): `GET /api/v1/public/whatsapp/embedded-signup-config`
→ `{ enabled, appId, configId, apiVersion }`.

## Embedded Signup (recommended clinic UX)

Clinic admins and personal-practice doctors use **Connect WhatsApp with Meta** on
`/clinic/whatsapp` or `/doctor/whatsapp`. Flow:

1. FE loads Facebook JS SDK with public App ID + Embedded Signup `config_id`
2. Meta returns an auth `code` (+ optional `waba_id` / `phone_number_id` via `postMessage`)
3. BE `POST .../whatsapp/connect/embedded` exchanges the code (App Secret), validates the phone
   belongs to the WABA, subscribes the KittyP app, optionally registers the phone, saves credentials,
   and ensures `invoice_receipt` templates
4. Invoice WhatsApp send is blocked until cached invoice template status is **APPROVED**
5. Meta webhooks `GET|POST /api/v1/webhook/whatsapp` refresh template status on approval/rejection

### Meta app checklist (ops)

1. KittyP Meta App: WhatsApp product + **Facebook Login for Business** + Embedded Signup configuration
2. App domains / redirect allowlist for KittyP FE origin(s)
3. Permissions: `whatsapp_business_management`, `whatsapp_business_messaging`
   (Development mode for your test BM; App Review / Advanced access for other businesses)
4. Server secrets: App ID, App Secret, Embedded Signup Config ID, webhook verify token
5. Subscribe the app’s webhook to the WABA (Embedded Signup does this via `subscribed_apps`)

**Advanced / pilots:** collapsed manual form still accepts Phone Number ID + WABA ID + token.
Manual connect also validates that the phone ID belongs to the WABA.

## Meta prerequisites (manual path)

1. Meta Business Manager + WhatsApp Business Account (WABA).
2. Cloud API phone number; copy **Phone number ID**.
3. System user with `whatsapp_business_messaging` + permanent token.
4. Approve templates in WhatsApp Manager before production sends.

## Required template: `invoice_receipt`

KittyP **auto-creates** this template on the clinic/doctor WABA when WhatsApp credentials are saved
(`POST /{WABA_ID}/message_templates`). Admins only enter Phone Number ID, WABA ID, and token
(or use Embedded Signup).

**Server requirement:** set `whatsapp.meta-app-id` / `WHATSAPP_META_APP_ID` to your Meta App ID
(developers.facebook.com) so KittyP can upload a sample PDF handle for the DOCUMENT header.

**Token permissions:** `whatsapp_business_messaging` + `whatsapp_business_management`.

**Category:** UTILITY  
**Language:** `en` (or match `whatsapp.invoice-template-lang`)  
**Header:** DOCUMENT  
**Body** (example — body variables must match code order):

```
Hi {{1}},

Here is your treatment invoice from {{2}}.

Pet: {{3}}
Invoice: {{4}}
Amount: ₹{{5}}

Thank you for choosing us.
```

| Var | Source |
|-----|--------|
| 1 | Owner name |
| 2 | Clinic / practice name |
| 3 | Pet name |
| 4 | Invoice number |
| 5 | Grand total (plain number string) |

Header media is uploaded at send time (PDF from S3 → Meta media ID). Template status appears in
WhatsApp Business settings (`templatesReady`, `invoiceTemplateStatus`). Use **Retry template setup**
if creation failed or while waiting for Meta approval.

Cached columns on `clinics` / `doctor_profiles`:
`whatsapp_connection_status`, `whatsapp_connected_at`, `whatsapp_last_verified_at`,
`whatsapp_invoice_template_status`.

## Later templates

- `vaccine_reminder` — text, vars: pet, vaccine, due date
- `checkup_reminder` — text, vars: pet, clinic, date
- `payment_receipt` — document header (similar to invoice)
- `promo_offer` — marketing (stricter Meta approval)

## Per-doctor / per-clinic senders

| Field | Meta / env name |
|-------|-----------------|
| Phone Number ID | `WHATSAPP_PHONE_NUMBER_ID` |
| Business Account ID (WABA) | `WHATSAPP_BUSINESS_ACCOUNT_ID` |
| Access token (write-only) | `WHATSAPP_TOKEN` |

| Sender | Stored on | Who uses it |
|--------|-----------|-------------|
| Doctor personal | `doctor_profiles.whatsapp_*` | Personal practice invoices |
| Clinic | `clinics.whatsapp_*` | All clinic-portal invoices (shared by clinic doctors) |

API returns `whatsappConfigured`, `connectionStatus`, `invoiceTemplateStatus`, `whatsappReadyToSend`,
`phoneNumberId`, `businessAccountId` (never the token). Tokens are encrypted at rest
(`app.crypto.secret` / `APP_CRYPTO_SECRET`, falls back to `JWT_SECRET`).

**Send gate:** credentials configured but invoice template not APPROVED → `503`
“WhatsApp invoice template not approved yet”.

**No platform credential fallback:** invoice sends use only the doctor or clinic sender. Missing entity credentials → clear `503` (even if `WHATSAPP_TOKEN` is set in env). Env `whatsapp.token` / `whatsapp.phone-number-id` are unused for entity sends.

Clinic WhatsApp settings GET/PUT / Embedded connect require `ROLE_CLINIC_ADMIN` (staff cannot read Meta IDs).

## Local / no credentials

With `whatsapp.enabled=false` (default outside local), `LoggingWhatsAppService` is active. Invoice create still saves the PDF; send returns **503**. If clinic/doctor Meta credentials are saved, the error asks you to set `WHATSAPP_ENABLED=true`. If credentials are missing, it asks you to add Phone Number ID + token in Clinic/Doctor settings.
