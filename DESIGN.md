## Automatic Interview Scheduling - Design Document

### 1. Overview

**Goal**: Provide an automatic interview scheduling system where interviewers configure weekly availability and a maximum interviews-per-week, and candidates can select exactly one active slot from generated slots for the next two weeks, with support for updating and cancelling bookings.

**Tech Stack**: Java 17, Spring Boot 3, Spring Web, Spring Data JPA, MySQL, JUnit, HTML/JavaScript UI with session-based authentication.

### 2. Architecture

- **Presentation / API layer** (`api`):
  - `InterviewerController`, `AvailabilityController`, `SlotController`, `BookingController`.
  - DTOs for requests/responses and `GlobalExceptionHandler` for consistent error responses.
- **Application / Use-case layer** (`application.service`, `application.exception`):
  - `AvailabilityService`, `SlotGenerationService`, `BookingService`.
  - Domain-specific exceptions extending `DomainException`.
- **Domain layer** (`domain.model`, `domain.repository`):
  - Entities: `Interviewer`, `WeeklyAvailability`, `InterviewSlot`, `Booking`.
  - Repositories: `InterviewerRepository`, `WeeklyAvailabilityRepository`, `InterviewSlotRepository`, `BookingRepository`.
- **Infrastructure**:
  - Spring Boot configuration + MySQL via JPA.
- **Frontend**:
  - Unified login page with role selection (`login.html`).
  - Separate dashboards for interviewers (`interviewer-dashboard.html`) and candidates (`candidate-dashboard.html`).
  - Session-based authentication using `sessionStorage`.

This follows a clean, layered architecture: controllers call services, which operate on domain entities via repositories.

### 3. Data Model / DB Schema

**Interviewer**
- Fields: `id`, `name`, `email (unique)`, `maxWeeklyInterviews`.
- The `id` is auto-generated and serves as the interviewer's login credential.

**WeeklyAvailability**
- Fields: `id`, `interviewer_id (FK)`, `dayOfWeek`, `startTime`, `endTime`, `slotDurationMinutes`.

**InterviewSlot**
- Fields: `id`, `interviewer_id (FK)`, `startTime`, `endTime`, `bookedCount`, `version (@Version)`.
- Indexes for `(interviewer_id, startTime, endTime)` and `(startTime, endTime)`.

**Booking**
- Fields: `id`, `slot_id (FK)`, `candidateName`, `candidateEmail`, `confirmed`.
- Unique constraint: `(candidateEmail, slot_id)` to prevent duplicate bookings per candidate/slot.

### 4. Authentication & User Flow

#### 4.1 Unified Login System

The application uses a single entry point (`login.html`) with a two-step process:

1. **Role Selection**: User selects whether they are an "Interviewer" or "Candidate".
2. **Authentication**: Based on role selection, appropriate login/signup forms are displayed.

#### 4.2 Interviewer Authentication

- **Signup (Create Account)**:
  - Collects: `name`, `email`, `maxWeeklyInterviews`.
  - Creates interviewer via `POST /api/v1/interviewers`.
  - System generates and displays the `interviewer.id` (must be saved by user).
  - Stores credentials in `sessionStorage` and redirects to interviewer dashboard.

- **Login**:
  - Requires: `interviewerId` (numeric) and `email` (for verification).
  - Verifies credentials via `GET /api/v1/interviewers/{id}` and email matching.
  - Stores credentials in `sessionStorage` and redirects to interviewer dashboard.

#### 4.3 Candidate Authentication

- **Register/Login**:
  - Collects: `name` and `email`.
  - No backend authentication required (simplified flow).
  - Stores credentials in `sessionStorage` and redirects to candidate dashboard.

#### 4.4 Session Management

- Both dashboards check `sessionStorage` on load.
- If credentials are missing, user is prompted to go to login page.
- Logout clears `sessionStorage` and redirects to login page.

### 5. Core Flows

#### 5.1 Set Weekly Availability

1. **Request**: `PUT /api/v1/interviewers/{id}/weekly-availability` with an array of weekly windows.
2. **Controller** (`AvailabilityController`):
   - Validates request DTOs.
   - Maps to `AvailabilityService.WeeklyAvailabilityInput`.
3. **Service** (`AvailabilityService.replaceWeeklyAvailability`):
   - Loads interviewer or throws `NotFoundException`.
   - Deletes existing availability rows for interviewer.
   - Creates new `WeeklyAvailability` rows after basic validation.

#### 5.2 Generate Slots for Next Two Weeks

1. **Request**: `POST /api/v1/interviewers/{id}/generate-slots?from&to`.
2. **Controller** (`SlotController.generateSlots`):
   - Computes default `from = today`, `to = from + 14 days` if omitted.
   - Invokes `SlotGenerationService.generateSlotsForInterviewer`.
3. **Service** (`SlotGenerationService`):
   - Loads interviewer; fetches availability for interviewer.
   - For each date in range:
     - Filters weekly availabilities matching that day-of-week.
     - Splits the window into slot-sized chunks (`slotDurationMinutes`).
     - For each chunk, checks for an existing slot in the same time range and only inserts if none exist.

#### 5.3 Candidate Selects Slot (Create Booking)

1. **Request**: `POST /api/v1/bookings` with `slotId`, `candidateName`, `candidateEmail`.
2. **Controller** (`BookingController.create`):
   - Validates request.
   - Delegates to `BookingService.createBooking`.
3. **Service** (`BookingService.createBooking` within a transaction):
   - **One Active Booking Rule**: Validates that the candidate doesn't already have an active (future) booking. Throws `AlreadyBookedException` if they do.
   - Loads `InterviewSlot` with optimistic lock (`findWithLockingById`).
   - Calls `validateWeeklyAndCapacity(slot)`:
     - Computes the calendar week (Monday–Sunday) for the slot date.
     - Uses `BookingRepository.countBySlot_Interviewer_IdAndSlot_StartTimeBetween` to get weekly total.
     - Compares with `interviewer.maxWeeklyInterviews`, throws `WeeklyLimitExceededException` if exceeded.
     - Checks `slot.bookedCount` against slot capacity (currently `1`), throws `SlotFullyBookedException` if full.
   - Increments `slot.bookedCount`, creates and saves a `Booking`.
   - If an `OptimisticLockException` occurs at flush time, translates to `SlotFullyBookedException`.

#### 5.4 Candidate Updates Slot (Change Booking)

1. **Request**: `PUT /api/v1/bookings/{bookingId}` with `newSlotId`.
2. **Controller** (`BookingController.updateSlot`):
   - Forwards to `BookingService.updateBookingSlot`.
3. **Service**:
   - Loads `Booking` or throws `NotFoundException`.
   - Loads both old and new slots with optimistic locks.
   - Decrements `bookedCount` on old slot (if > 0).
   - Validates weekly limit and capacity for new slot via `validateWeeklyAndCapacity`.
   - Increments `newSlot.bookedCount` and updates booking's `slot`.
   - Propagates `OptimisticLockException` as `SlotFullyBookedException`.

#### 5.5 Cancel Booking

1. **Request**: `DELETE /api/v1/bookings/{bookingId}`.
2. **Service**:
   - Loads booking, loads slot with lock.
   - Decrements `slot.bookedCount` when positive.
   - Deletes booking.

### 6. Slot Listing & Pagination

**API**: `GET /api/v1/slots`
- Query params:
  - `interviewerId` (optional filter).
  - `from`, `to` (optional date-time window; default now..now+14 days).
  - `cursor` (last seen `slotId`, default 0).
  - `limit` (page size; validated 1–100).

**Flow**:
1. Controller computes effective `from`/`to`/`cursor`.
2. Calls `InterviewSlotRepository.findUpcomingSlotsAfterCursor(from, to, cursor, PageRequest.of(0, limit))`.
3. Filters by `interviewerId` in-memory (for simplicity) if provided.
4. Maps to `SlotResponse` with `availableCapacity = max(0, 1 - bookedCount)`.
5. Computes `nextCursor` as the last slot id and `hasMore` based on remaining count.

**Pagination Strategy**:
- Uses **cursor-based pagination** over `slot.id` for better performance and stability compared to large OFFSET queries.
- This avoids page drift when new slots are inserted or removed during pagination.

### 7. Dashboard Features

#### 7.1 Interviewer Dashboard

- **Profile Tab**:
  - Displays interviewer information: ID, name, email, max weekly interviews.
  - Allows updating max weekly interviews.

- **Availability Tab**:
  - Set weekly availability windows (day of week, start time, end time, slot duration).
  - View current availability settings.

- **Generate Slots Tab**:
  - Generate slots for the next two weeks based on availability.
  - View generation status and slot counts.

- **Bookings Tab**:
  - View all bookings for the interviewer.
  - See candidate details, slot times, and confirmation status.

#### 7.2 Candidate Dashboard

- **Available Slots Tab**:
  - Browse available slots for the next two weeks.
  - Filter by interviewer (optional).
  - Book a slot (only one active booking allowed at a time).

- **My Bookings Tab**:
  - View current and past bookings.
  - Update booking to a different slot.
  - Cancel booking.

- **Booking Rules**:
  - Candidates can only have **one active booking** at a time.
  - Once a slot is booked, booking buttons are disabled until the current booking is cancelled or completed.

### 8. Error Handling

**Global handler** (`GlobalExceptionHandler`) returns a consistent error payload:

```json
{
  "timestamp": "2025-12-02T10:00:00Z",
  "status": 409,
  "error": "Conflict",
  "code": "SLOT_FULLY_BOOKED",
  "message": "Slot 10 is fully booked.",
  "path": "/api/v1/bookings"
}
```

Mappings:
- `NotFoundException` → 404.
- `AlreadyBookedException` → 409 (candidate already has an active booking).
- Other `DomainException` subclasses → 409 (conflict).
- Validation exceptions / invalid arguments → 400.
- Unexpected exceptions → 500 with detailed error message and stack trace logging.

**Frontend Error Handling**:
- Improved error message parsing and display.
- User-friendly messages for different HTTP status codes.
- Console logging for debugging.

### 9. Race Condition Handling

**Problem**: Many candidates may try to book the same slot or exceed weekly limits simultaneously.

**Mechanisms**:
- **Optimistic locking**:
  - `@Version` on `InterviewSlot` and `@Lock(OPTIMISTIC)` in repository ensure that concurrent updates to the same slot row produce `OptimisticLockException` for losers.
- **Transactional boundaries**:
  - Booking creation, updating, and cancellation are all `@Transactional`, so checks (weekly limit and capacity) and updates run atomically.
- **DB constraints**:
  - `UNIQUE(candidateEmail, slot_id)` prevents duplicate bookings by the same candidate for the same slot.
- **One Active Booking Rule**:
  - `BookingRepository.countByCandidateEmailAndSlot_StartTimeAfter` ensures candidates can only have one active booking at a time.

Flow under contention:
1. Multiple transactions load the same slot.
2. Each evaluates weekly limit and capacity.
3. Only the first to commit succeeds; subsequent commits detect version mismatch and throw, which is translated into a domain-level conflict error.

### 10. API Endpoints Summary

**Interviewer Management**:
- `POST /api/v1/interviewers` - Create interviewer (signup)
- `GET /api/v1/interviewers/{id}` - Get interviewer by ID (login verification)
- `PATCH /api/v1/interviewers/{id}/max-weekly-interviews` - Update max weekly interviews

**Availability**:
- `PUT /api/v1/interviewers/{id}/weekly-availability` - Set weekly availability
- `GET /api/v1/interviewers/{id}/weekly-availability` - Get weekly availability

**Slots**:
- `POST /api/v1/interviewers/{id}/generate-slots?from&to` - Generate slots
- `GET /api/v1/slots?cursor&limit&from&to&interviewerId&hideFull` - List slots (cursor-based)

**Bookings**:
- `POST /api/v1/bookings` - Create booking
- `PUT /api/v1/bookings/{id}` - Update booking slot
- `DELETE /api/v1/bookings/{id}` - Cancel booking
- `GET /api/v1/bookings/by-candidate?candidateEmail` - Get bookings by candidate
- `GET /api/v1/bookings/by-interviewer/{interviewerId}` - Get bookings by interviewer

### 11. Trade-offs

- **Pre-generated slots vs on-the-fly computation**:
  - Pre-generating slots simplifies booking logic and allows easy pagination and indexing, at the cost of more rows.
  - On-the-fly computation would save storage but complicate concurrency and search.
  - For this implementation, pre-generated slots are chosen for clarity and easier reasoning about availability.

- **Cursor pagination vs offset pagination**:
  - Offset pagination (`page`, `size`) is easier to use, but slow for large datasets and unstable when data changes.
  - Cursor pagination scales better and keeps page boundaries stable, at the cost of slightly more client-side logic.

- **Optimistic vs pessimistic locking**:
  - Optimistic locking scales better with read-mostly workloads and is easy to implement with JPA `@Version`.
  - Pessimistic locking (e.g. `FOR UPDATE`) could avoid retries but is more likely to create contention and deadlocks under high load.
  - Here, optimistic locking is chosen for simplicity and performance.

- **Session-based vs token-based authentication**:
  - Current implementation uses `sessionStorage` for simplicity (no backend session management).
  - For production, consider JWT tokens or server-side sessions for better security.

### 12. Testing Strategy

- **Unit tests (JUnit)**:
  - Test services (`BookingService`, `SlotGenerationService`, `AvailabilityService`) with mocks for repositories to verify business rules.
  - Focus on edge cases: full slots, weekly limit exceeded, invalid availability ranges, one active booking rule.

- **Integration tests**:
  - Use `@SpringBootTest` or `@DataJpaTest` to validate repository queries and JPA mappings.
  - Optionally use MockMVC to test controller + validation + error responses end-to-end.

### 13. UI Structure

**Pages**:
- `index.html` - Home page (redirects to login)
- `login.html` - Unified login page with role selection
- `interviewer-dashboard.html` - Interviewer dashboard with tabs for profile, availability, slots, and bookings
- `candidate-dashboard.html` - Candidate dashboard with tabs for available slots and bookings

**Features**:
- Modern dark-themed UI with gradient backgrounds
- Tab-based navigation
- Real-time updates after actions
- Confirmation banners and loading states
- Responsive design
- Session-based authentication checks
