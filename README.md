## Interview Scheduler (Spring Boot + MySQL)

### Overview
Automatic interview scheduling system with separate portals for interviewers and candidates. Interviewers set weekly availability and max interviews/week; slots are generated for the next two weeks; candidates select exactly one active slot, update, or cancel. Features clean architecture, cursor pagination, optimistic locking, unified login with role selection, and modern dashboards.

### Tech Stack
- Java 17, Spring Boot 3 (Web, JPA, Validation)
- MySQL (JPA entities, schema via ddl-auto)
- JUnit 5 (service-layer tests)
- HTML/JS UI (vanilla, session-based authentication, modern dark theme)

### Architecture
- **API (controllers)**: `InterviewerController`, `AvailabilityController`, `SlotController`, `BookingController`
- **Application services**: `AvailabilityService`, `SlotGenerationService`, `BookingService`
- **Domain**: Entities (`Interviewer`, `WeeklyAvailability`, `InterviewSlot`, `Booking`), Repositories
- **Infrastructure**: MySQL via Spring Data JPA
- **Error handling**: `GlobalExceptionHandler` with consistent JSON responses and detailed logging
- **Concurrency**: Optimistic locking on slots, transactional checks, unique candidate/slot constraint, one active booking rule
- **Frontend**: Unified login page, separate dashboards for interviewers and candidates

### Key Features

#### Authentication & User Management
- **Unified Login System**: Single entry point (`login.html`) with role selection (Interviewer/Candidate)
- **Interviewer Signup**: Creates account with auto-generated ID (must be saved for future logins)
- **Interviewer Login**: Requires interviewer ID and email verification
- **Candidate Registration**: Simple name and email collection (no backend authentication)
- **Session Management**: Uses `sessionStorage` for client-side session management

#### Interviewer Features
- Create account and receive unique interviewer ID
- Set weekly availability windows (day, time range, slot duration)
- Generate interview slots for the next two weeks
- View and manage bookings
- Update max weekly interviews limit
- View interviewer profile with ID display

#### Candidate Features
- Browse available slots for the next two weeks
- Book exactly one active slot at a time
- Update booking to a different slot
- Cancel booking
- View booking history

### Key API Endpoints

**Interviewer Management**:
- `POST /api/v1/interviewers` — create interviewer (signup)
- `GET /api/v1/interviewers/{id}` — get interviewer by ID (for login verification)
- `PATCH /api/v1/interviewers/{id}/max-weekly-interviews` — update max/week

**Availability**:
- `PUT /api/v1/interviewers/{id}/weekly-availability` — set weekly windows
- `GET /api/v1/interviewers/{id}/weekly-availability` — get weekly availability

**Slots**:
- `POST /api/v1/interviewers/{id}/generate-slots?from&to` — generate concrete slots
- `GET /api/v1/slots?cursor&limit&from&to&interviewerId&hideFull` — list slots (cursor-based)

**Bookings**:
- `POST /api/v1/bookings` — create booking
- `PUT /api/v1/bookings/{id}` — change slot
- `DELETE /api/v1/bookings/{id}` — cancel booking
- `GET /api/v1/bookings/by-candidate?candidateEmail` — get bookings by candidate
- `GET /api/v1/bookings/by-interviewer/{interviewerId}` — get bookings by interviewer

### How to Run Locally

#### Prerequisites
- **Java 17+** (or Java 24 with bytecode target 17)
- **Maven 3.6+**
- **MySQL 8.0+** (running and accessible)

#### Step 1: Verify Prerequisites

**Check Java:**
```bash
java -version
```

**Check Maven:**
```bash
mvn -version
```

**Check MySQL:**
```bash
mysql --version
```

#### Step 2: Create Database

Open MySQL command line or MySQL Workbench and run:

```sql
CREATE DATABASE interview_scheduler CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

**Windows PowerShell (one-liner):**
```powershell
mysql -u root -p -e "CREATE DATABASE IF NOT EXISTS interview_scheduler CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
```

Replace `root` with your MySQL username if different.

#### Step 3: Configure Database Credentials

**Option A: Environment Variables (Recommended)**

**Windows PowerShell:**
```powershell
$env:DB_USERNAME = "your_mysql_username"
$env:DB_PASSWORD = "your_mysql_password"
$env:DB_URL = "jdbc:mysql://localhost:3306/interview_scheduler"
```

**Windows Command Prompt:**
```cmd
set DB_USERNAME=your_mysql_username
set DB_PASSWORD=your_mysql_password
set DB_URL=jdbc:mysql://localhost:3306/interview_scheduler
```

**Option B: Edit `application.yml`**

Edit `src/main/resources/application.yml` and update:
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/interview_scheduler
    username: your_mysql_username 
    password: your_mysql_password
```

#### Step 4: Build the Project

```bash
mvn clean package
```

This will:
- Download dependencies
- Compile the code
- Run tests
- Package the JAR file

#### Step 5: Run the Application

**Option A: Using Maven (Recommended for development):**
```bash
mvn spring-boot:run
```

**Option B: Using the JAR file:**
```bash
java -jar target/interview-scheduler-0.0.1-SNAPSHOT.jar
```

You should see:
```
Tomcat initialized with port 8080
Started InterviewSchedulerApplication in X.XXX seconds
```

#### Step 6: Access the Application

- **Home Page**: Open your browser and go to `http://localhost:8080/` (redirects to login)
- **Login Page**: `http://localhost:8080/login.html`
- **API**: Test endpoints at `http://localhost:8080/api/v1/slots`

#### Step 7: Getting Started

1. **For Interviewers**:
   - Go to login page and select "Interviewer"
   - Click "Create Account" and fill in your details
   - **Save your Interviewer ID** - you'll need it to login later
   - After signup, you'll be redirected to the interviewer dashboard
   - Set your weekly availability
   - Generate slots for the next two weeks

2. **For Candidates**:
   - Go to login page and select "Candidate"
   - Enter your name and email (no account creation needed)
   - Browse available slots and book one
   - You can only have one active booking at a time

#### Troubleshooting

**MySQL Connection Error:**
- Verify MySQL is running: `mysql -u root -p`
- Check credentials in environment variables or `application.yml`
- Ensure database `interview_scheduler` exists

**Port 8080 Already in Use:**
- Change port in `application.yml`: `server.port: 8081`
- Or stop the process using port 8080

**Maven Not Found:**
- Install Maven and add it to your system PATH
- Or use Maven Wrapper: `./mvnw` (if available)

**Java Version Issues:**
- Ensure Java 17+ is installed
- For Java 24, the project is configured to compile to Java 17 bytecode

**Login Issues:**
- For interviewers: Ensure you're using the correct interviewer ID (numeric) and matching email
- Check browser console (F12) for error messages
- Verify server is running and accessible

### Pagination
- Cursor-based pagination on `/api/v1/slots` (`cursor`, `limit`) for stable, efficient listing vs. offset.

### Error & Race Handling
- Structured errors via `GlobalExceptionHandler` with detailed logging.
- Optimistic locking on `InterviewSlot` + transactional checks for capacity and weekly limit.
- Unique constraint `(candidateEmail, slot_id)` prevents duplicate bookings for the same slot.
- **One Active Booking Rule**: Candidates can only have one active (future) booking at a time. Attempting to book another slot while having an active booking will result in `AlreadyBookedException` (409 Conflict).

### Testing
- Service-layer JUnit tests: `BookingServiceTest`, `SlotGenerationServiceTest`.

### UI Highlights
- **Unified Login Page**: Role selection (Interviewer/Candidate) with dynamic form rendering
- **Interviewer Dashboard**: Tab-based interface for profile, availability, slot generation, and bookings
- **Candidate Dashboard**: Tab-based interface for browsing slots and managing bookings
- **Modern Design**: Dark theme with gradient backgrounds, smooth transitions, and responsive layout
- **Session Management**: Automatic login checks, logout functionality, and session persistence
- **Real-time Updates**: Immediate UI feedback after actions (booking, updating, cancelling)
- **Error Handling**: User-friendly error messages with detailed feedback

### Deployment (Quick: Railway)
1) Create Railway project; add MySQL service.
2) Set env vars: `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` from Railway MySQL credentials.
3) Deploy from GitHub; build with `mvn package -DskipTests`; start `java -jar target/interview-scheduler-0.0.1-SNAPSHOT.jar`.
4) Test: `https://<your-app>.up.railway.app/api/v1/slots` and `/login.html`.

### Trade-offs
- Pre-generated slots for simplicity and stable pagination (more rows vs. on-the-fly generation).
- Cursor pagination over offset for performance and consistency.
- Optimistic locking preferred for read-mostly workloads; could switch to pessimistic if needed.
- Session-based authentication using `sessionStorage` for simplicity; consider JWT or server-side sessions for production.

### Possible Extensions
- Add email notifications for booking confirmations and reminders.
- Admin seed/reset endpoints; audit logs on booking/cancel.
- More tests (availability validation, controller layer, integration tests).
- Password-based authentication for interviewers.
- JWT token-based authentication for better security.
- Calendar view for slots and bookings.
- Export bookings to CSV/PDF.
