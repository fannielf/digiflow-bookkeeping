# DigiFlow Books

Yksinkertainen kirjanpito- ja laskutussovellus toiminimelle (yhdenkertainen kirjanpito).
Built with Java 21, Spring Boot 3, Thymeleaf and PostgreSQL.

## What it does

- **Ledger (Kirjanpito)** – record income and expenses with gross amount + VAT rate; net and VAT are calculated automatically and each entry gets a sequential voucher (tosite) number per year.
- **Invoicing (Laskut)** – create invoices with sequential numbers and a valid Finnish reference number (viitenumero, 7-3-1 check digit). Print or save as PDF from the browser. Marking an invoice paid records the income in the ledger automatically.
- **VAT report (ALV-yhteenveto)** – quarterly summary of output/input VAT, ready to copy into OmaVero. Based on payment dates (maksuperusteinen alv).
- **Settings (Asetukset)** – your business details for invoices, plus a VAT-registered toggle. When off, invoices are issued at 0 % with the AVL 3 § note.

## Requirements

- JDK 21 (`brew install --cask temurin@21`)
- Maven (`brew install maven`) — or just open the project in IntelliJ IDEA
- Docker Desktop (for PostgreSQL)

## Run it

```bash
cd bookkeeping
mvn spring-boot:run
```

The `spring-boot-docker-compose` dependency starts the Postgres container from
`docker-compose.yml` automatically. Then open <http://localhost:8080>.

First steps in the app: **Asetukset** → fill in your business details →
**Asiakkaat** → add a customer → **Laskut** → create an invoice.

Run the tests:

```bash
mvn test
```

## Project structure

```
src/main/java/fi/digiflow/books/
├── ledger/      Entry, LedgerService (VAT split, voucher numbers)
├── invoicing/   Customer, Invoice, InvoiceLine, FinnishReference (viitenumero)
├── reports/     VatReportService (quarterly OmaVero numbers)
└── settings/    BusinessProfile (single-row settings)
```

Plain Spring MVC + Thymeleaf, no REST API or frontend framework — server-rendered
pages keep it simple and are a solid pattern to know.

## Bookkeeping notes (not tax advice)

- A toiminimi may keep single-entry bookkeeping (yhdenkertainen kirjanpito) if it stays within the small-business limits; this app follows that model.
- Keep the receipts! The app stores the numbers, but you must archive vouchers (receipts/invoices) for 6 years. A simple folder per year with files named by voucher number works.
- Verify current VAT rates and the VAT registration threshold from vero.fi — they change.

## Learning roadmap (ideas for v2)

Each of these is a well-scoped exercise toward junior-engineer skills:

1. **Flyway migrations** – replace `ddl-auto: update` with versioned SQL migrations and `validate`.
2. **More tests** – `@DataJpaTest` for repositories (with Testcontainers), `@WebMvcTest` for controllers.
3. **Receipt attachments** – upload a photo/PDF per ledger entry, store on disk.
4. **PDF generation** – produce the invoice PDF server-side with OpenPDF instead of browser print.
5. **Spring Security** – add a login so you can run this on a small VPS.
6. **Turn off `spring.jpa.open-in-view`** – then fix the lazy-loading issues properly with fetch joins. Great JPA lesson.
