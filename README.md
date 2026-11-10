# Kittyp Backend (kittyp-be)

Kittyp Backend (`kittyp-be`) is the server-side application powering the Kittyp platform, a modern solution for managing cat litter products, user accounts, orders, and more. Built with Java, Spring Boot, and PostgreSQL, this backend provides robust REST APIs, authentication, and integrations for a seamless e-commerce experience.

---

## Features

- **User Management:** Registration, authentication (JWT), profile management.
- **Product Catalog:** CRUD operations for products, categories, and favorites.
- **Cart & Orders:** Shopping cart, checkout, and order management.
- **Favorites:** User-specific favorite products with pagination and category filtering.
- **Breed catalogue:** `GET /api/v1/public/breeds?species=DOG&search=GSD&pageNumber=1&pageSize=20`
  searches active breed names and aliases case-insensitively. `species` and `search` are optional;
  pages are one-based and page size is capped at 50. Results use the standard `PaginationModel`.
- **Clinic inventory:** `GET /api/v1/clinic/{uuid}/inventory` accepts an optional `search` query parameter for case-insensitive
  partial matches on item name or category. It can be combined with the exact `category` filter and pagination;
  results remain scoped to active items in the selected clinic.
- **Admin Tools:** Role-based access for admin operations.
- **Integrations:** Payment (Razorpay), email (Zoho), AWS S3 for storage.
- **Async & Scheduled Tasks:** Background jobs and scheduled operations.
- **Robust Error Handling:** Centralized exception management.

---

### Vaccine schedule reminders

The pet reminder job also processes active, incomplete vaccine schedules due within the
next seven days. It sends an initial reminder on the first run in that window, and a final
reminder one day before the due date (or on the due date if the earlier run was missed).
Each stage is recorded on the vaccine schedule; completed schedules are excluded.
The existing scheduler cron and time zone settings control when the job runs; like
owner-created reminders, the due-date window uses the server's local date. Push is available for linked
platform users; email and WhatsApp use the existing reminder integrations. Clinic-only
owners are contacted when the pet has exactly one active clinic enrollment. An unavailable
or ambiguous owner is logged and retried while the due date remains in the window.

If Hibernate schema updates are disabled, apply `scripts/add_vaccine_reminder_timestamps.sql`
before deploying. The fields record dispatch attempts, not confirmed delivery: existing
push/email providers may report or handle delivery failures asynchronously, and a retry
after a partial send can produce duplicates.

---

### Breed catalogue deployment

The shared PostgreSQL `breed_master` table stores aliases in a `text[]` column. If Hibernate
schema updates are disabled, apply `scripts/create_breed_master.sql` before starting the
application. Startup creates the case-insensitive species/name index and inserts missing rows
from `src/main/resources/breed-catalog.json`, sourced from the workbook's dog and cat breed
sheets. Existing entries (including inactive entries and edited aliases) are not overwritten.
Startup also adds the aliases column if it is missing. The deployment database user must be
allowed to alter the table, create the index and insert catalogue rows.
The `Breed` entity maps the same column as a non-null list of strings, defaulting to an empty list.

The catalogue is a search aid: pet records continue to store breed as text, so existing and
custom values remain valid. Selecting a result saves its canonical name. Aliases retain the
workbook's explanatory qualifiers; they are search terms, not guarantees of equivalence.

---

## Tech Stack

- **Java 21**
- **Spring Boot 3**
- **PostgreSQL**
- **Maven**
- **Docker**
- **JWT Authentication**
- **Lombok**

---

## Getting Started

### Prerequisites

- Java 21+
- Maven 3.9+
- PostgreSQL database
- Docker (optional, for containerized deployment)

### Environment Variables

The application uses environment variables for configuration. Key variables include:

- `PGUSER`, `PGPASSWORD`, `PGHOST`, `PGPORT`, `PGDATABASE` (PostgreSQL)
- `JWT_SECRET`, `JWT_EXPIRATION`, `JWT_REFRESH_EXPIRATION`
- `RAZORPAY_KEY_ID`, `RAZORPAY_KEY_SECRET`
- `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`, `AWS_S3_REGION`, `AWS_S3_INVOICE_BUCKET_NAME`
- `ZOHO_API_KEY`

Set these in your environment or pass them to Docker as needed.

### Running Locally

1. **Clone the repository:**
   ```sh
   git clone https://github.com/your-org/kittyp-be.git
   cd kittyp-be# kittyp-be
