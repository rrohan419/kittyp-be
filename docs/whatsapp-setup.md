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
| `app.crypto.secret` | `APP_CRYPTO_SECRET` | AES key material for WhatsApp tokens at rest (prefer dedicated secret, not JWT) |

## Meta prerequisites

1. Meta Business Manager + WhatsApp Business Account (WABA).
2. Cloud API phone number; copy **Phone number ID**.
3. System user with `whatsapp_business_messaging` + permanent token.
4. Approve templates in WhatsApp Manager before production sends.

## Required template: `invoice_receipt`

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

Header media is uploaded at send time (PDF from S3 → Meta media ID).

## Later templates (Phase 2)

- `vaccine_reminder` — text, vars: pet, vaccine, due date
- `checkup_reminder` — text, vars: pet, clinic, date
- `payment_receipt` — document header (similar to invoice)
- `promo_offer` — marketing (stricter Meta approval)

## Per-doctor / per-clinic senders

Settings UI (Doctor Settings / Clinic Settings) asks only for:

| Field | Meta / env name |
|-------|-----------------|
| Phone Number ID | `WHATSAPP_PHONE_NUMBER_ID` |
| Business Account ID (WABA) | `WHATSAPP_BUSINESS_ACCOUNT_ID` |
| Access token (write-only) | `WHATSAPP_TOKEN` |

| Sender | Stored on | Who uses it |
|--------|-----------|-------------|
| Doctor personal | `doctor_profiles.whatsapp_*` | Personal practice invoices |
| Clinic | `clinics.whatsapp_*` | All clinic-portal invoices (shared by clinic doctors) |

API returns `whatsappConfigured`, `phoneNumberId`, `businessAccountId` (never the token). Tokens are encrypted at rest (`app.crypto.secret` / `APP_CRYPTO_SECRET`, falls back to `JWT_SECRET`).

**No platform credential fallback:** invoice sends use only the doctor or clinic sender. Missing entity credentials → clear `503` (even if `WHATSAPP_TOKEN` is set in env). Env `whatsapp.token` / `whatsapp.phone-number-id` are unused for entity sends.

Clinic WhatsApp settings GET/PUT require `ROLE_CLINIC_ADMIN` (staff cannot read Meta IDs).

## Local / no credentials

With `whatsapp.enabled=false` (default), `LoggingWhatsAppService` is active. **Save and Send** still creates the invoice + PDF, then returns **503** with a clear “WhatsApp is not configured” message (invoice is kept). Set `WHATSAPP_ENABLED=true` and Meta credentials on the doctor or clinic in Settings to deliver.
