# Online Real Estate Management System
**B.Tech AI & ML Java Web Project — Manish Rawat**

## Review 1
This project is a role-based online real-estate management system using Java 26, Jakarta Servlets, JSP, JDBC and MySQL. Maven builds a deployable WAR for Tomcat 10.1.x.

### Roles
- **Admin:** account role/disable management, listing review, system settings, dashboard summary and recent activity.
- **Property Manager:** create/update/delete own listings, review applications, create and update agreements, and message applicant tenants.
- **Tenant:** search approved listings, apply, track status, view agreements, message managers, manage profile and notifications.
- **All roles:** role-scoped dashboards, profile, notifications, and activity-specific communication links.

### Technology
Java 26 • Maven • Jakarta Servlet 6 • JSP • Tomcat 10.1 • MySQL 8 • JDBC • JUnit 5

### Core Java evidence
- **Inheritance:** `Admin`, `Manager`, `Tenant` extend abstract `User`.
- **Polymorphism:** `User.getDashboard()` is overridden by each role.
- **Interface:** `GenericRepository<T>` is implemented by DAO classes.
- **Collections & Generics:** `List<User>`, `List<Property>`, `List<Application>` and generic repository.
- **Exception Handling:** servlet/DAO/service exception handling and `AppException`.
- **Multithreading & Synchronization:** a bounded thread-safe `BlockingQueue` feeds the lifecycle-managed `NotificationWorker` for asynchronous notification persistence.
- **JDBC:** DAO classes use `Connection`, `PreparedStatement`, `ResultSet` and try-with-resources.

## Database
Run `database/schema.sql` in MySQL Workbench for a new database. It creates `real_estate_db` and the required tables without shared demo credentials. Register users through the application; assign roles for local classroom demonstrations only through a trusted local database session. Property rent is represented by the required `properties.rent` column. For an existing database from an earlier copy of this project, run `database/migration_review1.sql` once. Before the migration, check for multiple agreements per application with `SELECT application_id, COUNT(*) FROM rental_agreements GROUP BY application_id HAVING COUNT(*) > 1;`. Resolve any duplicates according to your data-retention requirements before adding the one-agreement-per-application constraint. The migration also adds the account active flag used by admin disable/restore and login. The secure startup helper and application startup listener migrate legacy `properties.price` values to `properties.rent` without inserting fake rent defaults.

The application reads its database password from `REAL_ESTATE_DB_PASSWORD`. Never put a password in source control. Optionally set `REAL_ESTATE_DB_USER` (defaults to `root`) and `REAL_ESTATE_DB_URL` (defaults to the local database URL).

On Windows, the secure startup helper below prompts with hidden input when the variable is not already configured, then supplies it only to the Tomcat process. This avoids putting the password in command history or Tomcat configuration files. For a Windows service, configure the variable for the service account through a protected environment or secret store before restarting it. On macOS/Linux, export it in the Tomcat process environment. Never paste the real value into this README or a command that will be saved in shell history.

New registrations are BCrypt-hashed immediately. This project does not publish or seed shared classroom passwords. For local role testing, register separate accounts and assign `ADMIN` or `MANAGER` only through a trusted local database session; keep the selected passwords private.

## Build & Deploy
```text
mvn clean test package
```
From the project folder, run `powershell -NoProfile -ExecutionPolicy Bypass -File scripts\start-tomcat-securely.ps1`. It reads `REAL_ESTATE_DB_PASSWORD` from the process, user, or machine environment; if absent, it prompts with hidden input. It applies the required existing-database migrations only when needed, stops the old Tomcat process, deploys the WAR and starts Tomcat 10.1 with Java 26. It configures the HTTP connector to use Tomcat NIO2 (with a backup of the original `server.xml`) to avoid Java 26’s failing Windows NIO loopback selector in this launch environment. The password is not saved in the project or Tomcat startup files. Never add the real value to source control, command-line arguments or this README.

Open:
`http://localhost:8080/real-estate-management/`

## Local classroom accounts
Register accounts through the application. For role demonstrations, assign roles to those accounts in your local database using an instructor-controlled session. Do not share account passwords or use classroom accounts outside the local demonstration.

## Final Review 1 Verification (6 October 2026)

The submission WAR was built with Java release 26 (`mvn clean test package`): 2 tests passed, no failures, and `target/real-estate-management.war` was deployed to the local Apache Tomcat 10.1.60 instance. The application root returned HTTP 200 with `text/html; charset=UTF-8`; the login page rendered without a database connection error.

Verified in the deployed application: Admin, Manager, and Tenant login and role dashboards; Logout is visible for each role; Logout returns to the login page, browser Back remains at the login page, and a direct dashboard request after logout is redirected to login. Admin can load the admin control panel; direct `/admin` access by Manager and Tenant returns HTTP 403. Main dashboard, properties, applications, agreements, messages, notifications, and profile pages loaded under all three demo roles. An intentionally nonexistent route returned HTTP 404. No HTTP 500 or database error was observed in the final exercised requests.

Audit-only data cleanup removed the three exact workflow test messages, four exact audit notifications, and the isolated disabled registration-test account. No matching temporary listing remained; legitimate/demo property and account records were preserved. Historical database exceptions in the Tomcat log predate the deployed schema-compatibility fixes; agreement creation/status and notification delivery had been verified after those fixes.

## Review 1 Test Flow
1. Register a tenant account and sign in with the password you chose locally.
2. As manager, create a listing; edit and delete listings from **My Listings**.
3. As admin, approve/reject listings and manage user roles/account access.
4. As tenant, search approved properties and apply once to a listing.
5. As the manager, update the application and use Messages to contact the tenant.
6. Create an agreement from the approved application, then view it as tenant.
7. Verify the notification, profile, system settings, activity log and dashboard summary pages.

## Review 1 Audit Documents

- [Review 1 readiness checklist and rubric mapping](docs/REVIEW1.md)
- [Application architecture and project structure](docs/PROJECT_STRUCTURE.md)
- Fresh database schema: `database/schema.sql`
- Repeat-safe existing database migration: `database/migration_review1.sql`

See `docs/REVIEW1.md` for the verified test record, role/security checks, rubric mapping, and screenshot checklist. The app keeps historical property/application data when a listing has rental history; such listings cannot be hard-deleted.

## Submission preparation status (6 October 2026)

The existing Surefire report records 2 passing password utility tests with no failures. A fresh Maven invocation during GitHub preparation could not start because Windows returned `Access is denied`; Maven was not retried. Treat the prior test/deployment record above as historical evidence, not a fresh build result. `target/` is excluded from Git.

## Suggested Review 1 evidence
Capture screenshots of Login/Register, Manager Dashboard, Add Property, Admin Approval, Tenant Search, Rental Application, database tables and successful Maven build.
