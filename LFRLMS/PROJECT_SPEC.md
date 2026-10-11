# PROJECT_SPEC.md — LFRLMS (v2)

Luxury Fleet Rental & Leasing Management System — Java Swing + JDBC + MySQL. This file is the single source of truth; it replaces any earlier version.

## 1. What we are building

A desktop application (Java Swing) backed by MySQL 8 for a luxury car rental and leasing business. It manages the fleet, individual and corporate customers, rentals (including future reservations), leases, payments, maintenance and drivers, and produces reports.

It is a 2nd-year student project. It must be **correct, readable and explainable**, not enterprise-grade.

## 2. Marking rubric the project must satisfy

**Java mini project (10 marks)**

| Rubric item | Marks | How the project shows it |
|---|---|---|
| Problem analysis and design | 1 | `docs/` contains the SRS summary, ER diagram, schema, and a class diagram |
| Inheritance | 1 | `Customer` (abstract) → `IndividualCustomer`, `CorporateCustomer`; `Contract` (abstract, implements `Billable`) → `Rental`, `Lease`; `Driver` is a separate class |
| Collections | 1 | `ArrayList`, `Vector` (table rows), `HashMap`, `TreeMap`, `HashSet`, queue, `Comparator`, `Iterator` — each used for a real purpose (see section 4) |
| Exception handling | 1 | Custom checked exception hierarchy, `try/catch/finally`, try-with-resources, exception chaining |
| JDBC CRUD | 2 | Full create/read/update/delete through DAO classes with `PreparedStatement`, a real Java transaction, and `CallableStatement` calls to stored procedures and functions |
| GUI | 1 | Swing application with tabs, tables, dialogs, event listeners, layout managers |
| Thread | 1 | Background loading thread, alert-monitor thread, clock thread, with safe start/stop |
| Viva | 1 | `docs/CONCEPTS.md` and `docs/WALKTHROUGH.md` make every concept easy to point to |

**DBMS mini project** needs: a proper multi-filter search system with date-range availability, pagination and sorting; a proper display layer (tables, detail views, dashboard, reports); and the SQL working behind it made visible: schema with constraints, views, **functions, stored procedures, triggers**, a transaction, indexes, demo queries and `EXPLAIN`.

## 3. Hard technical constraints

- Java 17, **Swing only** (no JavaFX).
- JDBC with the MySQL Connector/J 8.x jar placed in `lib/`. MySQL 8.0.16 or newer (CHECK constraints must be enforced).
- **No** Maven/Gradle, Spring, Hibernate/JPA, Lombok or any ORM. Optional: a FlatLaf jar in `lib/` for a modern look; if it is missing, fall back to the system look and feel.
- JDBC URL: `jdbc:mysql://localhost:3306/lfrlms?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Kolkata`
- Credentials are read from `config/db.properties` using `java.util.Properties`. Provide `config/db.properties.example` and add the real file to `.gitignore`.
- Must compile and run from the command line too (the README gives both Windows and Linux/Mac commands using `javac -cp "lib/*"` and `java -cp`).
- Load the driver with `Class.forName("com.mysql.cj.jdbc.Driver")` and a comment explaining why.

## 4. Code-level rules (2nd-year level)

- Simple, readable code. One class per file. Methods around 40 lines or fewer. Descriptive names.
- Every class starts with a short header comment: its purpose and which concept it demonstrates (for example "Concept: Inheritance"). Comment the *why*, not the obvious.
- Use only what a 2nd-year Java lab covers: classes, constructors (default, parameterized, copy), method overloading, static members, wrapper classes, encapsulation, `ArrayList`/`Vector`/`HashMap`/`TreeMap`/`HashSet`, `StringBuffer`, inheritance with `super`, abstract classes, interfaces, polymorphism, `final`, inner and anonymous classes, packages, custom exceptions, `Thread`/`Runnable`/`synchronized`/`volatile`, Swing with listeners, JDBC.
- **Avoid:** streams, `Optional`, records, sealed classes, `var`, reflection, executors, `CompletableFuture`, `SwingWorker`, Lombok, Java enums (use `public static final String` constants in a `Constants` class). Lambdas only for one-line listeners; otherwise use named or anonymous inner classes.
- Show these naturally where they fit, not forced:
  - **Constructors:** default, parameterized and copy. The edit dialog works on a copy and saves only on OK.
  - **Overloading:** `VehicleDao.search(...)` variants, `calculateCharge(...)` variants, `Validator.isValid(...)` variants.
  - **Static members and `final`:** `DBConnection.getConnection()`, `Constants`, `QueryLog`, constants declared `static final`.
  - **Wrapper classes:** nullable columns map to `Integer`/`BigDecimal`; form input uses `Integer.parseInt` and `new BigDecimal(String)`.
  - **StringBuffer:** builds the dynamic `WHERE` clause and the printable rental summary.
  - **Inheritance:** `super(...)` calls in subclass constructors; `Contract` holds the fields shared by `Rental` and `Lease`.
  - **Inner/anonymous classes:** an anonymous `Comparator` for sorting vehicles by rate; anonymous or inner listener classes in the GUI; one private static nested class (for example a table row holder).
  - **Collections:** a `HashMap<Integer,String>` lookup cache for brand/category/branch names; a `TreeMap<String,BigDecimal>` for revenue by month; a `HashSet` for unique values; an `Iterator` to remove expired alerts; `Vector<Vector<Object>>` for `DefaultTableModel` rows; a capped `LinkedList` queue for the query log.
- No empty `catch` blocks. `printStackTrace()` is never the only handling: log it and show a friendly message.
- Money is `BigDecimal`, never `double`. Dates are `java.time.LocalDate`/`LocalDateTime`, converted to and from `java.sql` types **only inside DAOs**.
- Layering is strict: the GUI never contains SQL, DAOs never show dialogs, services hold the business rules.
- **Exceptions:** `LfrlmsException extends Exception` is the base. Subclasses: `ValidationException`, `VehicleNotAvailableException`, `InvalidDateRangeException`, `DuplicateRecordException`, `IneligibleCustomerException`, `RecordInUseException`, `DataAccessException` (wraps `SQLException`). Show multi-catch, `finally`, try-with-resources and exception chaining at least once each.

## 5. Folder structure

```
LFRLMS/
├─ lib/                    mysql-connector-j-8.x.jar (and optional flatlaf jar)
├─ config/                 db.properties.example
├─ sql/                    01_schema.sql  02_functions_procedures.sql  03_views.sql
│                          04_triggers.sql  05_seed.sql  06_demo_queries.sql
│                          07_negative_tests.sql        (run in numeric order)
├─ src/lfrlms/
│   ├─ model/              Customer(abstract), IndividualCustomer, CorporateCustomer, Driver,
│   │                      Brand, VehicleCategory, Branch, Vehicle, Contract(abstract),
│   │                      Rental, Lease, Payment, Maintenance, Billable(interface)
│   ├─ exception/          (hierarchy in section 4)
│   ├─ util/               DBConnection, Constants, Validator, DateUtil, QueryBuilder,
│   │                      QueryLog, LookupCache, CsvExporter, AppLogger
│   ├─ dao/                one DAO per table + DashboardDao, ReportDao
│   ├─ service/            one per module, AvailabilityService, BillingService, AlertService
│   ├─ thread/             ClockThread, AlertMonitor, BackgroundTask
│   ├─ gui/                MainFrame, one panel per module, form dialogs, table models, renderers
│   └─ Main.java
├─ docs/                   CONCEPTS.md, WALKTHROUGH.md, TESTING.md, CLASS_DIAGRAM.md
└─ README.md
```

## 6. Database specification (12 tables, InnoDB, utf8mb4)

General rules: integer `AUTO_INCREMENT` primary keys; snake_case names; **do not write any `ON DELETE` or `ON UPDATE` clauses** (MySQL forbids CHECK constraints on columns that have CASCADE/SET NULL actions); `created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP` and `updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP` on customer, vehicle, rental, lease, payment, maintenance.

- **brand**(brand_id, brand_name UNIQUE NOT NULL)
- **vehicle_category**(category_id, category_name UNIQUE NOT NULL, description)
- **branch**(branch_id, branch_name UNIQUE NOT NULL, street, city, pincode CHAR(6), phone)
- **customer**(customer_id, customer_type ENUM('Individual','Corporate') NOT NULL, first_name, last_name (for a corporate customer this is the authorised contact person), email UNIQUE, phone, street, city, pincode)
- **individual_customer**(customer_id PK+FK, date_of_birth DATE NOT NULL, **license_no UNIQUE NOT NULL, license_expiry DATE NOT NULL**) — age is derived, never stored
- **corporate_customer**(customer_id PK+FK, company_name NOT NULL, gst_no CHAR(15) UNIQUE NOT NULL) — a company holds no driving licence
- **driver**(driver_id, first_name, last_name, phone, license_no UNIQUE NOT NULL, status ENUM('Active','Inactive'))
- **vehicle**(vehicle_id, registration_no UNIQUE NOT NULL, brand_id FK, category_id FK, branch_id FK, model, model_year SMALLINT CHECK 2000–2100, rental_rate DECIMAL(10,2) NULL (per day), lease_rate DECIMAL(10,2) NULL (per month), status ENUM('Available','Reserved','Rented','Leased','Maintenance','Inactive') DEFAULT 'Available'; CHECK that at least one of rental_rate and lease_rate is NOT NULL; both rates CHECK >= 0)
- **rental**(rental_id, customer_id FK, vehicle_id FK, driver_id FK NULL, scheduled_start_datetime NOT NULL, expected_return_datetime NOT NULL, actual_pickup_datetime NULL, actual_return_datetime NULL, odometer_out INT NULL, odometer_in INT NULL, condition_out VARCHAR(255) NULL, condition_in VARCHAR(255) NULL, daily_rate_applied DECIMAL(10,2) NOT NULL, charges DECIMAL(10,2) NULL, status ENUM('Confirmed','Active','Completed','Cancelled') NOT NULL DEFAULT 'Confirmed')
  CHECKs: expected_return > scheduled_start; status NOT IN ('Active','Completed') OR (actual_pickup_datetime IS NOT NULL AND odometer_out IS NOT NULL); status <> 'Completed' OR (actual_return_datetime IS NOT NULL AND odometer_in IS NOT NULL AND charges IS NOT NULL); odometer_in IS NULL OR odometer_in >= odometer_out; actual_return_datetime IS NULL OR actual_return_datetime >= actual_pickup_datetime.
  Derived, never stored: km_driven = odometer_in − odometer_out; rental_days.
- **lease**(lease_id, customer_id FK, vehicle_id FK, start_date NOT NULL, end_date NOT NULL, monthly_charge DECIMAL(10,2) NOT NULL, deposit DECIMAL(10,2) NOT NULL DEFAULT 0, status ENUM('Scheduled','Active','Completed','Terminated'); CHECK end_date > start_date)
- **payment**(payment_id, rental_id FK NULL, lease_id FK NULL, amount DECIMAL(10,2) CHECK > 0, method ENUM('Cash','Card','Bank Transfer','UPI'), payment_date DATETIME DEFAULT CURRENT_TIMESTAMP, status ENUM('Success','Pending','Failed','Refunded'); **CHECK ((rental_id IS NULL) <> (lease_id IS NULL))**)
- **maintenance**(maintenance_id, vehicle_id FK, service_date DATE NOT NULL, service_type, description, cost DECIMAL(10,2) CHECK >= 0, next_service_date DATE NULL, completed_date DATE NULL, status ENUM('In Progress','Complete'))

Four things go beyond the ER diagram and are required for the rules to work: `rental.expected_return_datetime` (an active rental otherwise has no end, so overlap checks fail), `rental.daily_rate_applied` (a snapshot, so a later rate change does not rewrite history), `maintenance.completed_date`, and the move of the licence columns to `individual_customer`.

**Indexes:** `(vehicle_id, scheduled_start_datetime, expected_return_datetime)` on rental; `vehicle(status)`; `customer(last_name, first_name)`; `payment(payment_date)`; unique indexes as listed.

**Charge formulas (single source of truth: the SQL functions; Java uses the same formula for previews):**
- Rental charge = `GREATEST(1, CEIL(TIMESTAMPDIFF(HOUR, start, end) / 24)) × daily_rate_applied`. For a completed rental use actual pickup and actual return. For an estimate use scheduled start (or actual pickup if started) and expected return, and for an Active rental use `GREATEST(expected_return_datetime, NOW())` as the end.
- Lease total = `monthly_charge × GREATEST(1, TIMESTAMPDIFF(MONTH, start_date, DATE_ADD(end_date, INTERVAL 1 DAY)))`.
- Outstanding balance = charge or total minus the sum of payments with status `Success`. The lease deposit is tracked separately and is excluded. Confirmed and Cancelled rentals are not billable.

**Functions (`02_functions_procedures.sql`)** — declare every function `DETERMINISTIC` or `READS SQL DATA`, otherwise MySQL raises error 1418 when binary logging is on:
- `fn_rental_charge(p_start DATETIME, p_end DATETIME, p_daily_rate DECIMAL(10,2))` returns DECIMAL(10,2)
- `fn_lease_total(p_monthly DECIMAL(10,2), p_start DATE, p_end DATE)` returns DECIMAL(10,2)
- `fn_customer_outstanding(p_customer_id INT)` returns DECIMAL(10,2) — rentals (Active/Completed) and leases (Active/Completed) minus successful payments

**Stored procedures (same file), called from Java with `CallableStatement`:**
- `sp_available_vehicles(p_from DATETIME, p_to DATETIME, p_branch_id INT, p_category_id INT)` — NULL for "any"; returns vehicles free for the whole period using the availability rule in 7.4; used by report R-01 and the DBMS Insights tab
- `sp_complete_rental(p_rental_id INT, p_return_dt DATETIME, p_odometer_in INT, p_condition_in VARCHAR(255))` — inside `START TRANSACTION`: checks the rental is Active, validates the odometer and time, computes the charge with `fn_rental_charge`, sets the rental to Completed, then `COMMIT`; an `EXIT HANDLER FOR SQLEXCEPTION` does `ROLLBACK` and `RESIGNAL`. It changes only the rental; the trigger then frees the vehicle.

**Views (`03_views.sql`, may call the functions):** `vw_vehicle_overview` (vehicle joined with brand, category, branch), `vw_available_fleet` (vehicles free right now), `vw_active_leases`, `vw_rental_balance`, `vw_lease_balance`.

**Triggers (`04_triggers.sql`):**
- `trg_rental_after_insert`: when a rental is inserted as `Active`, set the vehicle to `Rented`.
- `trg_rental_after_update`: when a rental goes `Confirmed → Active`, set the vehicle to `Rented`; when it goes `Active → Completed`, set the vehicle to `Available`.
- The payment exactly-one rule is enforced by the CHECK constraint, not by a trigger.

Routine and trigger scripts use `DELIMITER` and are run from the mysql client or Workbench, never from JDBC.

## 7. Business rules (services enforce them, each with its own exception)

1. Vehicle registration numbers are unique and match `^[A-Z]{2}[0-9]{2}[A-Z]{1,3}[0-9]{4}$`.
2. Expected return must be later than the scheduled start, actual return later than actual pickup, lease end later than lease start. Actual pickup and return stay empty until those events happen.
3. A vehicle in `Maintenance` or `Inactive` cannot be rented or leased.
4. **Availability.** A vehicle is available for a period `[from, to)` only if none of these overlap it (overlap means `start < to AND end > from`, strict inequalities so back-to-back periods are allowed):
   - rentals with status `Confirmed`, from `scheduled_start_datetime` to `expected_return_datetime`;
   - rentals with status `Active`, from `actual_pickup_datetime` to `expected_return_datetime`. If `expected_return_datetime` has already passed (the rental is overdue) it is **open-ended**: it blocks every period until the vehicle is returned. Do NOT model this as `end = NOW()`: with strict inequalities a search starting now or later would not conflict. In SQL: `r.status = 'Active' AND r.actual_pickup_datetime < :to AND (r.expected_return_datetime > :from OR r.expected_return_datetime <= NOW())`;
   - leases with status `Scheduled` or `Active`, from `start_date` to the day after `end_date`;
   - in-progress maintenance (blocks from `service_date` until it is completed).

   When starting a Confirmed rental, ignore that rental itself in the check. New Confirmed rentals may not start in the past (5-minute grace).
5. A rental or lease must reference an existing customer and vehicle. A rental needs the vehicle's `rental_rate`; a lease needs its `lease_rate`. The rate is copied into `daily_rate_applied` when the rental is created.
6. **Eligibility.** An individual customer needs licence details on file and `license_expiry` on or after the **end** of the rental or lease, not just today. A corporate customer holds no licence, so **every rental for a corporate customer must have an Active chauffeur assigned**. Corporate leases need no chauffeur (assumption: the company's staff use the car).
7. Payment amount > 0. Exactly one of rental or lease. Only `Success` payments count. A payment may not exceed the outstanding balance.
8. A driver must be `Active` and have no overlapping Confirmed or Active rental.
9. **Rental lifecycle:** `Confirmed → Active → Completed`, or `Confirmed → Cancelled`. A walk-in rental may be created directly as `Active` (scheduled start = actual pickup = now). Starting needs pickup time, `odometer_out` and `condition_out`. Completed and Cancelled are final.
10. Records with history are never hard-deleted. `DELETE` is a real, working operation for records nobody references (master data, a customer or vehicle added by mistake). If the record is referenced, catch MySQL error 1451 (SQLState 23000) in the DAO and throw `RecordInUseException` with the message "Deactivate it instead". Rentals, leases and payments are never deleted from the GUI; they are cancelled, terminated or refunded through their `status`.
11. A customer has exactly one subtype row matching `customer_type`. Create and update the parent and child rows in one transaction.
12. Maintenance cannot be opened on a vehicle that is currently `Rented` or `Leased`. If Confirmed rentals exist for the vehicle in the future, show a warning listing them (do not block).
13. Stored vehicle status changes only like this:

| Event | Vehicle status | Done by |
|---|---|---|
| Rental becomes Active (start or walk-in) | Rented | trigger |
| Rental completed | Available | trigger |
| Rental created Confirmed or cancelled | no stored change | — |
| Lease started (Scheduled → Active) | Leased | service, in a transaction |
| Lease completed or terminated | Available | service, in a transaction |
| Maintenance opened | Maintenance | service, in a transaction |
| Maintenance completed | Available | service, in a transaction |
| Staff deactivates | Inactive | service |

`Reserved` stays in the ENUM but is **derived at display time** (vehicle is Available now and a Confirmed rental starts within 24 hours). Code never writes it, because storing it would wrongly block a car for dates that are actually free.

## 8. Faults to avoid (the review prompt checks every one)

- **F1** Availability decided from `status` alone, or an overdue Active rental treated as ending at `NOW()`. Use the date-overlap rule in 7.4, where overdue Active rentals are open-ended.
- **F2** String-concatenated SQL. Use `PreparedStatement` with `?` for every value. `StringBuffer` builds only fixed fragments. `ORDER BY` columns come from a whitelist, never from user text.
- **F3** `LIKE` search with raw user text. Escape `%` and `_`, and add the wildcards in Java.
- **F4** Resource leaks. Connection, Statement, CallableStatement and ResultSet use try-with-resources.
- **F5** One shared static `Connection` used by several threads. `DBConnection.getConnection()` is static but returns a fresh short-lived connection each call.
- **F6** Swing components touched off the Event Dispatch Thread. All UI updates go through `SwingUtilities.invokeLater`; all database calls run off the EDT.
- **F7** Stale search results. When the user types fast, an old slow search can finish after a newer one. Keep a request counter and ignore results from outdated requests.
- **F8** Race condition when creating or starting a rental or lease for the same car. Inside the transaction, run `SELECT ... FOR UPDATE` on the vehicle row, then re-run the availability check, then insert or update.
- **F9** Both the trigger and Java changing vehicle status for rentals. The trigger owns rental-driven status changes; Java must not duplicate it.
- **F10** `double` for money, or `java.util.Date`.
- **F11** Hard-coded credentials, or absolute dates in seed data (they go stale). Seed with `NOW() - INTERVAL n DAY` style dates.
- **F12** Seed data that breaks the rules. The end of `05_seed.sql` runs a consistency query that must return 0 rows (for example every vehicle with an Active rental is `Rented`, no two overlapping commitments on one vehicle).
- **F13** Threads left running after the window closes. Use a `volatile boolean`, `interrupt()`, daemon threads and handle `InterruptedException` properly.
- **F14** Holding a database connection while a thread sleeps.
- **F15** Empty catch blocks, swallowed exceptions, raw `SQLException` text shown to users.
- **F16** No input validation. Check email, 10-digit phone, 6-digit pincode, GST format, licence number, dates and numbers in `Validator`, before touching the database.
- **F17** Pagination that loads all rows. Use `LIMIT ? OFFSET ?` with a matching `COUNT(*)` that uses the same filters.
- **F18** Deleting a row through the GUI without confirmation, or editing table cells directly. Tables are read-only; edits go through dialogs.
- **F19** Connection failures crashing the app. Show a clear "cannot connect" message with retry.
- **F20** Creating a rental plus its advance payment, or starting a lease, with several updates and no transaction. It must commit or roll back as one unit.
- **F21** The charge formula written twice (SQL function and Java preview) and drifting apart. `ConsoleTest` must compare the Java estimate with `fn_rental_charge` for several cases.
- **F22** Functions created without `DETERMINISTIC`/`READS SQL DATA` (error 1418), or scripts with `DELIMITER` run through JDBC.
- **F23** Asking a corporate customer for a driving licence, or letting an individual's expired licence slip through because only today's date was checked.

## 9. GUI specification

- `MainFrame` (about 1200×750) with a `JTabbedPane`: Dashboard, Vehicles, Customers, Rentals, Leases, Payments, Maintenance, Drivers, Reports, DBMS Insights.
- Status bar at the bottom: live clock (from `ClockThread`), database connection indicator, alert counts.
- Each module panel has: a filter bar on top, a read-only `JTable` in the middle, a pagination bar (Prev/Next, page label, page size) and Add/Edit/Deactivate-or-Delete/Refresh/Export CSV buttons.
- Clicking a table header sorts on the server through the `ORDER BY` whitelist.
- Live search: a `DocumentListener` plus a `javax.swing.Timer` debounce of about 300 ms.
- Status columns use a custom `DefaultTableCellRenderer` with colours (Available green, Rented amber, Maintenance red, Inactive grey).
- Add/Edit forms are `JDialog`s with validation messages next to the fields, Enter to submit, Escape to close.
- **The customer form adapts to the type:** choosing Individual shows date of birth and licence fields; choosing Corporate shows company name and GST number.
- **The payment form adapts to the type:** choosing Rental or Lease changes what the reference drop-down lists, and shows the outstanding balance.
- **Vehicles tab:** a Details dialog for the selected vehicle (rates, status, branch, next rentals) plus "Create Rental" and "Create Lease" buttons that open the form with the vehicle preselected.
- **Rentals tab actions:** New Rental (reservation or walk-in, with availability check and estimated charge), Start (pickup), Cancel, Return (calls `sp_complete_rental`).
- Use a mix of layout managers (`BorderLayout`, `GridBagLayout`, `FlowLayout`) and listeners (`ActionListener`, `DocumentListener`, `ListSelectionListener`, `MouseListener`, `WindowListener`).
- Detail view: selecting a customer shows a side or bottom panel with their rentals, leases and payments (joins) and outstanding balance (`fn_customer_outstanding`).
- **DBMS Insights tab** (read-only): the last 200 SQL statements run (time, SQL text, parameters, milliseconds, rows) from `QueryLog`; an "EXPLAIN selected query" button; "SHOW CREATE TABLE" for a chosen table; lists of views, functions, procedures and triggers with their definitions; a short checklist of SQL features with the example query for each. It never runs free-text SQL typed by the user and never shows credentials.

## 10. Thread specification

- `ClockThread extends Thread`: updates the status-bar clock every second.
- `AlertMonitor implements Runnable`, started inside a `Thread`: every 60 seconds (configurable) calls `AlertService` for overdue active rentals, Confirmed rentals starting within 24 hours or past their start without pickup, leases ending within 30 days, maintenance due within 7 days, and individual customers' licences expiring within 30 days; posts the counts to the dashboard and status bar via `invokeLater`; stops cleanly on window close.
- `BackgroundTask`: a small helper that runs one DAO call in a new `Thread`, shows a wait cursor, and delivers the result on the EDT with the stale-result guard from F7.
- `QueryLog.add(...)` is `synchronized` because several threads write to it.
- Document in `docs/CONCEPTS.md` which class uses `extends Thread`, `Runnable`, `synchronized`, `volatile`, `sleep`, `interrupt`, `join`.

## 11. Search and reports

- **Vehicles:** text (registration, model, brand), brand, category, branch, status, rental-rate and lease-rate ranges, "available between [from] and [to]" using the rule in 7.4.
- **Customers:** text over name, phone, email, company, licence number; type; "licence expiring within N days".
- **Rentals, Leases, Payments:** status, date range, customer text, vehicle text, plus an "outstanding only" filter.
- Reports (each a tab sub-view with filters, shown in a `JTable`, exportable to CSV with `BufferedWriter`, escaping commas and quotes):
  - R-01 available vehicles by branch and category (calls `sp_available_vehicles`)
  - R-02 active rentals with customer and vehicle
  - R-03 active leases and upcoming expirations
  - R-04 revenue for a date range, grouped by month (`TreeMap`)
  - R-05 outstanding balance per customer (uses `fn_customer_outstanding`)
  - R-06 most rented vehicles
  - R-07 vehicles due for maintenance
  - R-08 customer transaction history
  - R-09 rental status summary (Confirmed, Active, Completed, Cancelled)
- **Dashboard:** counts (total/available/rented/leased/maintenance vehicles, active rentals, active leases, upcoming rentals), revenue this month, the alert list, and a simple bar chart of revenue by month drawn with Swing `paintComponent` (no chart library).

## 12. Seed data

About 6 brands, 5 categories, 3 branches (Nashik, Pune, Mumbai), 24 vehicles, 16 individual and 6 corporate customers, 6 drivers, about 30 rentals (about 12 Completed, 6 Active, 10 Confirmed in the future, 2 Cancelled), 6 leases, about 30 payments, 8 maintenance records. All dates relative to now.

Because the triggers are created before the seed runs, insert active rentals as `Active` (with actual pickup and odometer) and let the trigger set the vehicle to `Rented`; insert finished rentals directly as `Completed` (with return data and charges from `fn_rental_charge`); never insert Active and then update. The trigger does not cover leases or maintenance, so the seed must set the vehicle status to `Leased` or `Maintenance` itself for active leases and in-progress maintenance.

Include these demo cases: an individual with an expired licence, a vehicle in maintenance, a Confirmed rental starting in about 3 hours, an overdue Active rental, a lease ending in about 20 days, a customer with an outstanding balance, a chauffeur-driven rental for a corporate customer, and two requests that would overlap if created (for the overlap demo).

## 13. Documents to produce

- `README.md`: prerequisites, database setup commands, how to run (IDE and command line), screenshots list.
- `docs/CONCEPTS.md`: a table of rubric item → class → method, and the same for every other lab topic used (including inner class, anonymous class, `final`, `super`).
- `docs/WALKTHROUGH.md`: step-by-step flow of five features (search vehicles, create a rental reservation, start rental, return rental, record payment) naming each class and each SQL routine touched.
- `docs/TESTING.md`: test cases from the acceptance criteria and sections 7 and 8, with steps, expected result and a pass/fail column.
- `docs/CLASS_DIAGRAM.md`: a Mermaid class diagram.
- `sql/06_demo_queries.sql`: at least 15 numbered, commented queries (joins, GROUP BY/HAVING, subqueries, view usage, the overlap query, calls to the functions and procedures, the outstanding-balance query, `EXPLAIN` before and after an index).
- `sql/07_negative_tests.sql`: statements that must be rejected (duplicate registration, payment with both or neither reference, expected return before scheduled start, Active rental without odometer_out, Completed rental without charges, invalid foreign key, odometer_in below odometer_out, deleting a referenced brand).
