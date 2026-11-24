# WhatsApp Cloud API setup (KittyP)

## Env / properties

| Property | Env | Description |
|----------|-----|-------------|
| `whatsapp.enabled` | `WHATSAPP_ENABLED` | `true` when ready to send (default `false`) |
| `whatsapp.token` | `WHATSAPP_TOKEN` | Server-only permanent token for Kittyp's shared WABA |
| `whatsapp.phone-number-id` | `WHATSAPP_PHONE_NUMBER_ID` | Kittyp's shared Cloud API phone number ID |
| `whatsapp.business-account-id` | `WHATSAPP_BUSINESS_ACCOUNT_ID` | Kittyp's shared WhatsApp Business Account ID (WABA) |
| `whatsapp.api-version` | `WHATSAPP_API_VERSION` | e.g. `v21.0` |
| `whatsapp.invoice-template` | `WHATSAPP_INVOICE_TEMPLATE` | Template name, default `invoice` |
| `whatsapp.invoice-template-lang` | `WHATSAPP_INVOICE_TEMPLATE_LANG` | Default `en` |
| `whatsapp.vaccine-template` | `WHATSAPP_VACCINE_TEMPLATE` | Default `vaccine_reminder` |
| `whatsapp.checkup-template` | `WHATSAPP_CHECKUP_TEMPLATE` | Default `checkup_reminder` |
| `whatsapp.promo-template` | `WHATSAPP_PROMO_TEMPLATE` | Default `promo_offer` |
| `whatsapp.appointment-template` | `WHATSAPP_APPOINTMENT_TEMPLATE` | Optional approved Kittyp WABA text-template name |
| `whatsapp.default-country-code` | `WHATSAPP_DEFAULT_COUNTRY_CODE` | Default `91` for 10-digit IN numbers |
| `whatsapp.meta-app-id` | `WHATSAPP_META_APP_ID` | Kittyp Meta App ID; used for uploading a sample invoice PDF when submitting a document template |
| `whatsapp.meta-app-secret` | `WHATSAPP_META_APP_SECRET` | **Server only** — legacy practice-owned Embedded Signup flow |
| `whatsapp.embedded-signup-config-id` | `WHATSAPP_EMBEDDED_SIGNUP_CONFIG_ID` | Legacy practice-owned Embedded Signup `config_id` |
| `whatsapp.webhook-verify-token` | `WHATSAPP_WEBHOOK_VERIFY_TOKEN` | Hub verify token for `/api/v1/webhook/whatsapp` |
| `whatsapp.register-pin` | `WHATSAPP_REGISTER_PIN` | Optional 6-digit PIN for Cloud API phone register |
| `app.crypto.secret` | `APP_CRYPTO_SECRET` | AES key material for legacy practice-owned WhatsApp tokens at rest |

The shared sender credentials are never returned to the frontend. Its token, Phone Number ID,
and WABA ID must identify the same Kittyp-owned Cloud API number.

If Meta reports that `message_templates` is a nonexisting field, check that
`WHATSAPP_BUSINESS_ACCOUNT_ID` is the WhatsApp Business Account ID shown in WhatsApp Manager,
not the Meta Business Manager ID, app ID, or phone number ID. The token must also have access
to that WABA and the `whatsapp_business_management` permission.

## Kittyp shared sender setup

Configure one Kittyp-owned Meta Business Manager/WABA and Cloud API number:

1. Set `WHATSAPP_TOKEN`, `WHATSAPP_PHONE_NUMBER_ID`, and `WHATSAPP_BUSINESS_ACCOUNT_ID` in the
   backend environment.
2. Grant the permanent token `whatsapp_business_messaging` and `whatsapp_business_management`.
3. Set `WHATSAPP_ENABLED=true` only after the account and approved templates are ready.
4. Approve the required templates in WhatsApp Manager. The Messaging page reads the approved
   templates directly from this WABA.

Clinic admins and personal-practice doctors do not connect their own WhatsApp accounts. Previously
saved clinic/doctor credentials and Embedded Signup endpoints remain only for legacy compatibility;
they are not used to send outbound communications.

## Required template: `invoice`

Kittyp sends all clinic and personal-practice invoice PDFs through this approved DOCUMENT template
on the shared Kittyp WABA. The backend checks that the configured name/language is currently
approved before uploading and sending the PDF. Approve the template on Kittyp's WABA before use.

`whatsapp.invoice-template` and `whatsapp.invoice-template-lang` configure the name and language.
The default template name is `invoice`.
If Kittyp submits the template, `whatsapp.meta-app-id` / `WHATSAPP_META_APP_ID` must be set so it
can upload a sample PDF handle for Meta's DOCUMENT header.

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

The PDF from S3 is uploaded to Meta using Kittyp's sender credentials and attached using this
DOCUMENT header. The template is not offered in the generic text composer.

Legacy per-practice cached WhatsApp status columns do not determine outbound readiness.

## Approved messaging templates

The Messaging page loads approved/active text or document templates from Kittyp's WABA. Text-template
parameters are entered in the order of the numbered placeholders in the approved body.
Document-header templates (including `invoice`) remain available to Billing, which attaches the
generated PDF; they cannot be sent from the generic composer.

Reminder templates must be approved on Kittyp's WABA:

**`vaccine_reminder`**

```
Hello {{1}}, this is a reminder from {{3}} that {{2}} is due for {{4}} on {{5}}.

Please contact the clinic to schedule the vaccination.

If this vaccination has already been completed or an appointment is already booked, please ignore this reminder.
```

**`checkup_reminder`**

```
Hello {{1}}, following {{2}}'s visit to {{3}} on {{4}}, the veterinarian recommended a follow-up check-up around {{5}}.

Please contact the clinic to arrange the visit.

If this follow-up is already booked, you can ignore this reminder.
```

## Automatic reminder delivery

Vaccine schedules and pet reminders are delivered by the dedicated pet-reminder scheduler at
10:00 AM in `Asia/Kolkata` (`kittyp.reminders.cron` can override the time). The generic
Messaging dispatcher does not claim these automation records, preventing duplicate or
template-less sends. Automation sends reserve and settle channel credits through the same
wallet/ledger as manually composed messages, and successful sends remain in Message History.
Email uses the 2,000-message monthly free allowance before paid credits. Failed deliveries
release reserved credits and remain eligible for the next scheduler run
instead of being marked complete. Clinic-wide pause and per-automation settings still apply.

## Sender ownership and readiness

| Setting | Purpose |
|---------|---------|
| `WHATSAPP_TOKEN` | Kittyp's server-side permanent access token |
| `WHATSAPP_PHONE_NUMBER_ID` | Shared Cloud API sender number |
| `WHATSAPP_BUSINESS_ACCOUNT_ID` | WABA used to fetch templates and verify approval |
| `WHATSAPP_ENABLED` | Global delivery kill switch |

Invoice sends require the configured approved DOCUMENT template; text sends require a currently
approved text template and a matching parameter count. Missing sender configuration, disabled
delivery, or missing approval returns a clear service error. Practice-owned credentials stored
on `clinics` or `doctor_profiles` are not consulted for outbound messages.

## Local / no credentials

With `whatsapp.enabled=false` (default), `LoggingWhatsAppService` is active and sends return **503**.
Invoice creation still saves the PDF. To enable sends, Kittyp operators must configure the shared
sender credentials and set `WHATSAPP_ENABLED=true`; practices do not need to configure a sender.
