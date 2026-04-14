# School ERP Billing Dashboard

## What was added

- Dashboard summary APIs
- School detail APIs using the exact `schools` entity fields
- Plan listing APIs using the exact `plans` entity
- Payment audit trail APIs
- Invoice listing and PDF download APIs
- Automatic invoice generation after successful payment verification
- React admin analytics app for operations teams

## Backend endpoints

- `GET /api/schools/summary`
- `GET /api/schools`
- `GET /api/schools/{schoolCode}`
- `GET /api/schools/{schoolCode}/payments`
- `GET /api/schools/plans/all`
- `GET /api/schools/payments/all`
- `GET /api/invoices`
- `GET /api/invoices/school/{schoolCode}`
- `GET /api/invoices/{invoiceNumber}`
- `GET /api/invoices/{invoiceNumber}/download`
- `POST /api/payment/create-order`
- `POST /api/payment/verify`

## Start backend

1. Install Java 17+
2. Set `JAVA_HOME`
3. Run:

```powershell
.\ASN-Service\mvnw.cmd -f pom.xml spring-boot:run
```

## Start frontend

```powershell
cd frontend
npm install
npm run dev
```

Frontend URL:

- `http://localhost:5173`

Backend URL:

- `http://localhost:8080`

## Full payment flow

1. Your website creates an order and completes payment using the payment APIs
2. `POST /api/payment/verify` updates `payment_audit_trail`
3. Backend updates school license dates and active status
4. Backend generates GST invoice PDF
5. Admin dashboard reads schools, payment audit trail, plans, and invoices
6. Finance or support users can filter by school and download invoice PDFs

## Important assumptions

- School subscriptions are billed within India using CGST 9% + SGST 9%
- Invoice company information is configured in `application.yml`
- Seed schools, plans, and sample payment audit data are inserted automatically when the tables are empty
