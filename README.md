# Kittyp Backend (kittyp-be)

Kittyp Backend (`kittyp-be`) is the server-side application powering the Kittyp platform, a modern solution for managing cat litter products, user accounts, orders, and more. Built with Java, Spring Boot, and PostgreSQL, this backend provides robust REST APIs, authentication, and integrations for a seamless e-commerce experience.

---

## Features

- **User Management:** Registration, authentication (JWT), profile management.
- **Product Catalog:** CRUD operations for products, categories, and favorites.
- **Cart & Orders:** Shopping cart, checkout, and order management.
- **Favorites:** User-specific favorite products with pagination and category filtering.
- **Admin Tools:** Role-based access for admin operations.
- **Integrations:** Payment (Razorpay), email (Zoho), AWS S3 for storage.
- **Async & Scheduled Tasks:** Background jobs and scheduled operations.
- **Robust Error Handling:** Centralized exception management.

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
