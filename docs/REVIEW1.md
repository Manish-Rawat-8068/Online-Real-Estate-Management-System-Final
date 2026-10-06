# Review 1 Readiness Guide

## Project overview

The Online Real Estate Management System is a role-based application for listing properties, reviewing tenant applications, and handling the rental lifecycle. It is built with Java 26, Jakarta Servlet/JSP, JDBC, MySQL 8, and Maven WAR packaging for Apache Tomcat 10.1.x.

## Features by role

| Role | Demonstration features |
| --- | --- |
| Admin | Summary dashboard; user role and active-state management; listing approval/rejection; system settings; recent activity. |
| Property Manager | Dashboard; create/edit/delete owned listings; application status review; rental agreements; messages to applicant tenants; listing counts. |
| Tenant | Registration and login; approved listing search by city and type; application submission/status; agreement viewing; profile updates; messages; notifications. |
| All signed-in users | Session-protected navigation, account refresh/active-state check, profile, notifications, and logout. |

## Architecture

```text
Browser
  -> Jakarta Servlets (request validation and role checks)
  -> Services (authentication and property workflows)
  -> DAO classes (JDBC, PreparedStatement, transactions)
  -> MySQL 8
  -> JSP views under WEB-INF (not directly addressable)
```

`AuthFilter` checks the current session user against the database and applies the maintenance setting on protected routes. DAOs own SQL and map database rows to models. The notification worker uses a bounded, thread-safe queue and a managed background thread; the application listener shuts it down when Tomcat unloads the webapp.

## Database and relationships

The canonical schema is in `database/schema.sql`. Its tables are:

- `users`: accounts, role, active state, and contact details.
- `properties`: manager-owned listings and approval status. The canonical rent field is `rent`.
- `rental_applications`: tenant-to-property applications and workflow status.
- `rental_agreements`: one agreement per approved application, with dates, rent, and status.
- `messages`: sender/receiver messages.
- `notifications`: per-user updates and read state.
- `activity_logs`: admin-visible recent activity.
- `system_settings`: administrator-managed settings.

Relationships use foreign keys. Applications retain their property and tenant references; agreements refer to an application. Existing installations may have additional legacy columns, including `properties.price` or a required `rental_agreements.property_id`. Startup migration preserves existing rent data, and agreement creation inspects known legacy agreement columns and writes compatible values. The application migration is idempotent. The manual SQL migration checks schema metadata before adding `users.is_active` or the application uniqueness constraint; it reports duplicate agreements instead of deleting data.

For a new database, run `database/schema.sql` in MySQL Workbench. It creates no shared accounts with public passwords. Register accounts through the application; assign demonstration roles only through a trusted local database session. For an existing installation, take a database backup, inspect `database/migration_review1.sql`, and run it once (or again safely if needed). Do not drop or recreate the database as a migration method.

## Setup and run

Requirements: JDK 26, Maven, MySQL 8, and Apache Tomcat 10.1.x.

The database password is read only from `REAL_ESTATE_DB_PASSWORD`. Optional connection overrides are `REAL_ESTATE_DB_USER` (defaults to `root`) and `REAL_ESTATE_DB_URL` (defaults to local `real_estate_db`). Never put the real password into source, command arguments, README files, or Tomcat configuration files. On Windows, the provided helper uses hidden input when needed and passes the secret only to the Tomcat process.

From the project folder:

```powershell
mvn clean test package
powershell -NoProfile -ExecutionPolicy Bypass -File scripts\start-tomcat-securely.ps1
```

Open `http://localhost:8080/real-estate-management/`. The helper targets Java 26 and Tomcat 10.1, migrates supported legacy rent columns without fabricating amounts, deploys the WAR, and configures Tomcat NIO2 for the Windows Java 26 loopback issue documented in the root README.

## Local classroom accounts

Create accounts through the registration page and use passwords chosen privately for the local demonstration. Change a registered account's role to `ADMIN` or `MANAGER` only from a trusted local database session. New passwords are BCrypt-hashed during registration. No shared demo passwords are included in the repository.

## Review 1 test checklist

Run `mvn clean test package`, confirm `target/real-estate-management.war` exists, deploy to Tomcat 10.1 with Java 26, and verify the application URL above. Sign in using locally registered accounts and their private passwords.

1. Register a new tenant; test invalid details and duplicate email feedback.
2. Sign in as manager; create a listing, edit it, and delete an unused listing.
3. Sign in as admin; approve one listing and reject another; check user management, settings, dashboard counts, and recent activity.
4. Sign in as tenant; filter approved properties by city and type, submit an application, and verify status.
5. As manager, update the application and create an agreement; as tenant, verify the agreement.
6. Send a message in both directions and verify notifications; mark a notification read.
7. Update a profile, log out, verify the session is gone, and verify a tenant cannot open the admin panel or submit manager-only actions.
8. Inspect Tomcat logs after deployment for unexpected `SEVERE` errors. Historical errors before a fix do not by themselves indicate a current failure.

## Java rubric map

| Rubric | Code evidence |
| --- | --- |
| Inheritance and polymorphism | `Admin`, `Manager`, and `Tenant` extend abstract `User`; each role provides its dashboard label. |
| Interfaces | `GenericRepository<T>` is implemented by database DAOs. |
| Exception handling | JDBC resource handling, validation exceptions, servlet error handling, and `AppException`. |
| Collections and generics | `List`, `Map`, `ArrayList`, typed repository and service results. |
| Multithreading and synchronization | Bounded `ArrayBlockingQueue`, a lifecycle-managed worker, and synchronized queue inspection. |
| DAO/JDBC | Dedicated DAO classes use JDBC `PreparedStatement`, try-with-resources, and transactions for multi-step changes. |
| Web and access control | Jakarta Servlets/JSP, `HttpSession`, `AuthFilter`, and servlet role checks. |

## Evidence checklist

- Login/register page and one validation message.
- Manager dashboard and create/edit listing screens.
- Admin approval and rejection, user/settings panels, and activity list.
- Tenant filtered listing, application status, and agreement.
- Message conversation, notification/read state, and profile update.
- `mvn clean test package` success and the Tomcat deployment log.

## Final local verification record (6 October 2026)

### Build and deployment

- `mvn clean test package` completed with **BUILD SUCCESS**, Java release 26, 2 tests passed, and no test failures/errors.
- Generated `target/real-estate-management.war` and deployed it to Apache Tomcat 10.1.60. Tomcat's deployment log confirmed completion.
- `http://localhost:8080/real-estate-management/` returned HTTP 200 and `text/html; charset=UTF-8`; the login page rendered without a database connection error.

### Role, navigation, and logout checks

- Admin, Manager, and Tenant local classroom accounts each authenticated and opened the matching dashboard. Each dashboard showed a visible Logout link. Shared passwords are intentionally omitted from this guide.
- For every role, dashboard, properties, applications, agreements, messages, notifications, and profile loaded without a database error or HTTP 500.
- Admin loaded `/admin` successfully. Manager and Tenant requests to `/admin` returned HTTP 403.
- For each role, Logout returned to the login page; using browser Back stayed on the login page, and a fresh direct request to `/dashboard` after logout redirected to login.
- An intentionally nonexistent route returned the expected HTTP 404.
- Admin maintenance mode was verified earlier in the audit: tenant access was blocked while enabled, the Admin retained access, and maintenance was restored to OFF.

### Workflow and data cleanup

- Manager create/edit/delete flow was verified using an unused audit listing. That listing was deleted; no matching temporary property remained at final cleanup. The approved demo property `Green Valley Apartment` and legitimate property/application data were preserved.
- Admin approval/rejection, user management, system settings, activity view, and dashboard summary were exercised. Tenant registration/login, property filtering, application status, agreement viewing/status, profile save, two-way messages, notifications, and mark-read were also exercised against the configured MySQL database.
- Agreement compatibility for the deployed legacy `property_id` and `monthly_rent` columns and notification compatibility for the required `title` column were fixed and then exercised successfully.
- Final audit cleanup removed exactly three identified test messages, four identified audit notifications, and the isolated disabled registration-test account. No demo Admin, Manager, Tenant, or unrelated project data was deleted.

### Remaining runtime note

Tomcat logs retain historical exceptions from before the agreement/notification schema compatibility fixes were deployed. The final exercised requests after redeployment produced no observed HTTP 500 or database error. The intentionally requested missing-page probe returned HTTP 404. The automated test suite currently contains two password utility tests; servlet workflows were checked manually in the deployed browser against the local database.
