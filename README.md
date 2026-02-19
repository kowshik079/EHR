<<<<<<< HEAD
# EHR
=======
# EHR Demo Project

This project is a Spring Boot based EHR (Electronic Health Record) demo. It focuses on three main ideas:

1. **Secure handling of Aadhaar numbers** using AES‑GCM (encryption, decryption, masking).
2. **Medical report upload and processing** (PDF upload, text extraction, storing metadata).
3. **Role Based Access Control (RBAC)** for:
   - `ADMIN`
   - `DOCTOR`
   - `DIAGNOST`
   - `PATIENT`

Below is a conceptual overview so you can understand and explain the project easily.

---

## 1. Architecture Overview

The application is a typical Spring Boot 3 app with these layers:

- **Controllers** (REST API endpoints)
  - `AadhaarController` → `/api/aadhaar/**`
  - `MedicalReportController` → `/api/reports/**`
  - `SecurityDemoController` → `/api/**/ping` test endpoints

- **Services** (business logic)
  - `AadhaarEncryptionService` → AES‑GCM encryption/decryption/masking of Aadhaar
  - `MedicalReportService` → validate PDFs, extract text, encrypt patientId, save reports

- **Model / Entity**
  - `MedicalReport` → JPA entity representing one uploaded PDF report

- **Repository**
  - `MedicalReportRepository` → Spring Data JPA, CRUD for `MedicalReport`

- **Security**
  - `SecurityConfig` → Basic Auth + RBAC with in‑memory users

---

## 2. Security & RBAC

RBAC is implemented in `SecurityConfig` using Spring Security with **Basic Auth**.

### Users and Roles

Configured users (in‑memory):

- `admin` / `admin123` → ROLE_ADMIN
- `doctor` / `doctor123` → ROLE_DOCTOR
- `diagnost` / `diagnost123` → ROLE_DIAGNOST
- `patient1` / `patient123` → ROLE_PATIENT

### Access Rules (simplified)

From `SecurityConfig`:

- **Public**:
  - `GET /api/public/**` → no auth needed, always allowed.

- **Medical reports**:
  - `POST /api/reports/upload` → roles: DIAGNOST, ADMIN
  - `GET /api/reports` → roles: DOCTOR, ADMIN
  - `GET /api/reports/{patientId}` → roles: PATIENT, DOCTOR, ADMIN
  - `DELETE /api/reports/{id}` → roles: PATIENT, ADMIN

- **Aadhaar utilities**:
  - `/api/aadhaar/**` → any authenticated user (any valid login)

All other endpoints require authentication.

Authentication style: **Basic Auth** (username/password) on every protected request.

---

## 3. Aadhaar Handling

### AadhaarEncryptionService

Service responsible for:

- `encrypt(String aadhaar)` → AES‑GCM encryption, outputs Base64(IV + ciphertext + tag)
- `decrypt(String encrypted)` → reverse of encrypt
- `maskAadhaar(String aadhaar)` → returns `XXXX-XXXX-1234`
- `formatAadhaar(String aadhaar)` → returns `1234-5678-9012`

AES‑GCM is used because it provides **authenticated encryption** (confidentiality + integrity) and avoids padding oracle issues associated with AES‑CBC.

### Where Aadhaar is used

- At the **API level**, `patientId` is treated as the **plain Aadhaar number** (12 digits).
- In the **database**, `patientId` is stored **encrypted** using `AadhaarEncryptionService`.

This is handled inside `MedicalReportService`:

- On upload:
  - Take plain Aadhaar from `patientId`.
  - Encrypt it.
  - Store encrypted value in `MedicalReport.patientId`.

- On list by patient:
  - Receive plain Aadhaar from path (`/api/reports/{patientId}`).
  - Encrypt it.
  - Query DB by encrypted value.

So: **API sees plain Aadhaar, DB sees only encrypted Aadhaar.**

---

## 4. Medical Report Upload Flow

### Entity: `MedicalReport`

Each uploaded report row contains:

- File info: `id`, `fileName`, `originalFileName`, `fileSize`, `mimeType`
- Integrity: `checksum` (SHA‑256 of file)
- Content: `extractedText`, `normalizedText`, `pageCount`
- Metadata: `uploadedAt`, `uploadedBy`, `reportType`, `reportDate`
- Patient link: `patientId` (encrypted Aadhaar)

### Upload Steps

1. **Insomnia (or client) sends**:
   - Method: `POST`
   - URL: `http://localhost:8080/api/reports/upload`
   - Auth: Basic Auth (e.g. `diagnost/diagnost123`)
   - Body: Multipart Form
     - `file` = PDF file
     - `patientId` = Aadhaar e.g. `123456789012`

2. **Security layer** checks:
   - Path + method
   - Only DIAGNOST or ADMIN can upload.

3. **MedicalReportController.upload** receives:
   - File, `patientId`, optional `reportType`, `reportDate`.
   - Authentication (logged in username → `uploadedBy`).
   - Delegates to `MedicalReportService.upload`.

4. **MedicalReportService.upload**:
   - Validates file (not empty, size limit, extension `.pdf`).
   - Encrypts `patientId` using `AadhaarEncryptionService.encrypt`.
   - Computes `checksum` with SHA‑256.
   - Extracts text with PDFBox (full report text).
   - Normalizes text for reliable regex/search.
   - Constructs a `MedicalReport` object.
   - Saves it via `MedicalReportRepository` to the DB.

5. **Controller returns** a small JSON with key info (id, fileName, patientId (encrypted), uploadedBy).

---

## 5. Listing & Deleting Reports

### 5.1 List All Reports (DOCTOR, ADMIN)

- `GET /api/reports`
- Roles allowed: DOCTOR, ADMIN.
- Controller → `MedicalReportService.listAll()` → `repository.findAll()` → JSON list.

### 5.2 List Reports by Patient (Aadhaar)

- `GET /api/reports/{patientId}`
  - `{patientId}` is **plain Aadhaar** in the URL.
- Controller:
  - For PATIENT role, checks that the username matches `patientId` (simplistic ownership rule).
  - Calls `service.listByPatientId(patientId)`.
- Service:
  - Encrypts Aadhaar.
  - Calls `repository.findByPatientId(encrypted)`.
  - Returns all matching reports.

### 5.3 Delete Report (PATIENT own, ADMIN any)

- `DELETE /api/reports/{id}`
- Controller:
  - Loads report by id.
  - Checks:
    - Is user ADMIN? → can delete any.
    - Is user PATIENT and does username match report.patientId? → delete allowed.
  - Otherwise 403 Forbidden.
  - Calls `service.deleteById(id)`.

---

## 6. AadhaarController Endpoints

These are utility/demo endpoints under `/api/aadhaar`:

- `POST /api/aadhaar/encrypt`
  - Body: `{ "aadhaar": "123456789012" }`
  - Returns: `{ "encrypted": "...", "masked": "XXXX-XXXX-9012" }`

- `POST /api/aadhaar/decrypt`
  - Body: `{ "encrypted": "..." }`
  - Returns: `{ "aadhaar": "123456789012", "formatted": "1234-5678-9012" }`

- `POST /api/aadhaar/mask`
  - Body: `{ "aadhaar": "123456789012" }`
  - Returns: `{ "masked": "XXXX-XXXX-9012" }`

These endpoints are for **testing and understanding** Aadhaar encryption, and share the same logic that is used when encrypting `patientId` in `MedicalReportService`.

---

## 7. Testing in Insomnia (Quick Guide)

### Start the App

```bash
cd /Users/Z00GVGQ/Downloads/EHR
./gradlew bootRun
```

### Test Public Ping

- `GET http://localhost:8080/api/public/ping`
- No auth needed.

### Test Role Pings (RBAC)

- Admin ping:
  - `GET http://localhost:8080/api/admin/ping`
  - Auth: Basic `admin` / `admin123`
- Doctor ping:
  - `GET http://localhost:8080/api/doctor/ping`
  - Auth: `doctor` / `doctor123`
- Diagonist ping:
  - `GET http://localhost:8080/api/diagonist/ping`
  - Auth: `diagnost` / `diagnost123`
- Patient ping:
  - `GET http://localhost:8080/api/patient/ping`
  - Auth: `patient1` / `patient123`

### Test Aadhaar Encrypt/Decrypt

- Encrypt:
  - POST `http://localhost:8080/api/aadhaar/encrypt`
  - Auth: Basic (e.g. admin/admin123)
  - Body JSON: `{ "aadhaar": "123456789012" }`
- Decrypt:
  - POST `http://localhost:8080/api/aadhaar/decrypt`
  - Auth: Basic
  - Body JSON: `{ "encrypted": "<value from encrypt>" }`

### Test Upload Report (DIAGNOST)

- POST `http://localhost:8080/api/reports/upload`
- Auth: Basic `diagnost` / `diagnost123`
- Body: Multipart Form
  - `file` → File: select a PDF
  - `patientId` → Text: `123456789012`

### Test View Reports (DOCTOR)

- GET `http://localhost:8080/api/reports`
- Auth: Basic `doctor` / `doctor123`

### Test View by Patient (Doctor/Admin)

- GET `http://localhost:8080/api/reports/123456789012`
- Auth: `doctor` / `doctor123` or `admin` / `admin123`

### Test Delete (Admin)

- DELETE `http://localhost:8080/api/reports/{id}`
- Auth: `admin` / `admin123`

---

This README is meant to give you a **clear, theory-first picture** of how your project works so you can revisit it later and quickly remember the whole flow.

>>>>>>> 881abf8 (final code)
