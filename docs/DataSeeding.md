## Key Principle: Flyway as the Authority

In our Spring Boot application, **Flyway** is the sole authority for managing the database schema (tables, columns, etc.). Hibernate/JPA is configured to only map entities to the existing schema, not modify it.

| Component | Role | Configuration |
| :--- | :--- | :--- |
| **Flyway** | **Creates and updates the database schema.** | Enabled by `spring.flyway.enabled: true` |
| **Hibernate/JPA** | Reads from and writes to the schema. | Disabled from modifying the schema via `spring.jpa.hibernate.ddl-auto: none` |

---

## Guide to Running the Application Properly

To ensure the application starts correctly and the database schema is built/maintained by Flyway:

### 1. **Configuration Integrity**

* **Ensure Deletion:** The redundant `persistence.xml` file must remain **deleted**.
* **Verify Properties:** Confirm the following settings are present in your `application.yml`:
    ```yaml
    spring:
      jpa:
        hibernate:
          ddl-auto: none 
      flyway:
        enabled: true
    ```

### 2. **Flyway Migration Scripts**

* All schema changes (new tables, column changes) must be created as new, sequential `.sql` scripts in the `src/main/resources/db/migration/` directory (e.g., `V3__add_new_table.sql`).

### 3. **Handling Missing Tables (Repair)**

If you manually delete a table from the database (e.g., in a Docker instance), Flyway's internal history becomes out of sync, causing the application to fail on startup.

To force Flyway to rebuild the missing table:

1.  **Identify the Version:** Find the version number (`V#`) of the migration script that created the missing table (e.g., the `users` table was created in `V2`).
2.  **Delete the History Entry:** In your MySQL console, run a targeted delete query to remove that successful record from Flyway's tracking table:
    ```sql
    DELETE FROM flyway_schema_history WHERE version = '[VERSION_NUMBER]';
    ```
    (e.g., `DELETE FROM flyway_schema_history WHERE version = '2';`)
3.  **Restart the Application:** Flyway will detect the missing history entry, re-run the corresponding script, and recreate the table, allowing the application to start.