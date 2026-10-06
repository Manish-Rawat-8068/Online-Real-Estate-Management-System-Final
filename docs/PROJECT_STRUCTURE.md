# Project Structure and Data Flow

```text
src/main/java/com/manishrawat/realestate/
  config/       Environment-backed JDBC configuration
  dao/          PreparedStatement-based repositories and table queries
  exception/    Application-level exception type
  filter/       Session refresh, authentication, and maintenance guard
  model/        User roles, properties, and rental applications
  service/      Authentication and property workflow rules
  servlet/      Jakarta Servlet routes and request validation
  thread/       Idempotent compatibility migration and notification worker
  util/         JDBC connection, password hashing, HTML escaping
src/main/webapp/
  index.jsp     Public login and tenant registration
  assets/       Responsive application styles
  WEB-INF/      Servlet config and protected JSP view templates
database/       Fresh-install schema and repeat-safe existing-database migration
docs/           Review 1 checklist, schema notes, rubric mapping, and contribution record
scripts/        Secure Windows Tomcat startup/deploy helper
```

Requests enter role-aware Servlet endpoints. `AuthFilter` refreshes the session user and blocks inactive accounts. The servlet validates the request and calls a service or DAO. DAOs bind user data with `PreparedStatement`, close resources using try-with-resources, and return typed models or collections. Protected JSPs under `WEB-INF` only render request attributes and cannot be requested directly.

The notification queue is bounded and thread-safe; its one consumer performs database writes outside the request thread. `NotificationLifecycle` owns worker start/stop so Tomcat redeploys do not leave the application worker running. Compatibility DDL uses schema metadata and copies legacy rent values before enforcing the canonical column.

Team contribution roles and verified work ownership should be recorded in `docs/TEAM_CONTRIBUTIONS.md` by the project team. The template intentionally contains no assumed names or claims.
