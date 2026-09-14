# WhatsApp Embedded Signup — local setup

Frontend is `https://localhost:8080` (Vite + `@vitejs/plugin-basic-ssl`). Backend is typically `http://localhost:8002`. Meta cannot reach localhost for webhooks.

Clinic admins use the same **Connect with Meta** button on Practice Settings (personal-practice doctors use Doctor Settings). Manual credential forms stay as fallback on both pages.

## 1. Tunnel the backend (not Vite)

```bash
ngrok http 8002
```

Use the `https://<ngrok-host>` URL. Do **not** `ngrok http 8080` — that tunnels the frontend, not Graph webhooks.

## 2. Meta Developer Dashboard

| Field | Value |
|-------|--------|
| App Domains | `localhost` |
| Login with JavaScript SDK | Yes |
| Allowed Domains for the JavaScript SDK | `https://localhost:8080/` (also add `http://localhost:8080/` if you still open HTTP) |
| Valid OAuth Redirect URIs | `https://localhost:8080/` and `https://www.facebook.com/connect/login_success.html` |
| Facebook Login for Business configuration ID | copy into `VITE_META_CONFIG_ID` |
| Webhook Callback URL | `https://<ngrok-host>/api/v1/whatsapp/webhook` |
| Verify token | same as `WHATSAPP_WEBHOOK_VERIFY_TOKEN` |
| Webhook fields | subscribe `messages` |

## 3. Env

Frontend (`.env` / `.env.devlocal`):

```
VITE_META_APP_ID=<Meta app id>
VITE_META_CONFIG_ID=<Embedded Signup config id>
```

Backend:

```
WHATSAPP_META_APP_ID=<same Meta app id>
WHATSAPP_META_APP_SECRET=<app secret>
WHATSAPP_WEBHOOK_VERIFY_TOKEN=<same verify token as dashboard>
WHATSAPP_API_VERSION=v21.0
```

If Facebook Login refuses HTTP, stay on `https://localhost:8080` and accept the browser self-signed cert warning (`basic-ssl`).

## 4. Smoke

```bash
curl -sS "https://<ngrok-host>/api/v1/whatsapp/webhook?hub.mode=subscribe&hub.verify_token=$WHATSAPP_WEBHOOK_VERIFY_TOKEN&hub.challenge=12345"
```

Must print `12345` as raw text, not JSON.
