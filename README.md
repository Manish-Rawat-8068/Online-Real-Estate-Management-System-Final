# Online Real Estate Management System
B.Tech AI & ML Java Web Project — Manish Rawat

## Stack
Java 26, Maven, Jakarta Servlet 6, JSP, Tomcat 10.1, MySQL 8, JDBC, BCrypt.

## Roles
ADMIN: user management, approve/reject listings, settings, activity monitoring, reports.
MANAGER: property CRUD, application status, rental agreements, tenant communication, analytics.
TENANT: search/filter properties, apply, agreements, profile, notifications, messaging.

## Setup
1. Create MySQL database by running `database/schema.sql`.
2. Edit `src/main/java/com/manishrawat/realestate/config/DBConfig.java` if your MySQL password differs.
3. From this folder run `mvn clean package`.
4. Copy `target/real-estate-management.war` to Tomcat `webapps`.
5. Start Tomcat 10.1 and open `/real-estate-management/`.

Demo logins: admin@realestate.com/admin123, manager@realestate.com/manager123, tenant@realestate.com/tenant123.

## Rubric evidence
OOP: abstract User + subclasses, interface, polymorphism; Collections/Generics: DAO lists and generic repository; Exception Handling: AppException/Global handling; Threads: NotificationWorker with synchronized queue; JDBC: DBConnection/DAO; Web: Servlets/JSP/filter; CRUD, approvals, applications, agreements, messages, notifications, logs, analytics.
