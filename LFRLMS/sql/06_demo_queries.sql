-- =====================================================================
-- 06_demo_queries.sql : LFRLMS DBMS demonstration queries
-- Run AFTER 01..05. Run section by section in MySQL Workbench / mysql client.
-- Safe to run twice: every data-changing demo is rolled back or cleaned up.
-- NOTE: names come from PROJECT_SPEC.md. If a query errors on a column name,
--       compare with sql/01_schema.sql and fix that one line.
-- =====================================================================
USE lfrlms;
SET SQL_SAFE_UPDATES = 0;

-- ---------------------------------------------------------------------
-- A. JOINS
-- ---------------------------------------------------------------------

-- Q1. INNER JOIN (4 tables): fleet with brand, category and branch.
-- Shows each vehicle with readable names instead of foreign-key numbers.
SELECT v.vehicle_id, v.registration_no, b.brand_name, v.model,
       c.category_name, br.branch_name, v.rental_rate, v.lease_rate, v.status
FROM vehicle v
JOIN brand b             ON b.brand_id = v.brand_id
JOIN vehicle_category c  ON c.category_id = v.category_id
JOIN branch br           ON br.branch_id = v.branch_id
ORDER BY b.brand_name, v.model;

-- Q2. JOIN on the subtype design: every customer with its subtype columns.
-- LEFT JOINs because a customer has exactly one of the two subtype rows.
SELECT cu.customer_id, cu.customer_type,
       CONCAT(cu.first_name, ' ', cu.last_name) AS contact_name,
       ic.license_no, ic.license_expiry, co.company_name, co.gst_no
FROM customer cu
LEFT JOIN individual_customer ic ON ic.customer_id = cu.customer_id
LEFT JOIN corporate_customer  co ON co.customer_id = cu.customer_id
ORDER BY cu.customer_type, cu.customer_id;

-- Q3. Rentals with customer, vehicle and (optional) chauffeur.
-- LEFT JOIN driver because driver_id is NULL for individual self-drive rentals.
SELECT r.rental_id, r.status, CONCAT(cu.first_name, ' ', cu.last_name) AS customer,
       v.registration_no, v.model,
       CONCAT(d.first_name, ' ', d.last_name) AS chauffeur,
       r.scheduled_start_datetime, r.expected_return_datetime
FROM rental r
JOIN customer cu  ON cu.customer_id = r.customer_id
JOIN vehicle v    ON v.vehicle_id = r.vehicle_id
LEFT JOIN driver d ON d.driver_id = r.driver_id
ORDER BY r.scheduled_start_datetime DESC;

-- Q4. Payments for rentals AND leases in one list (UNION ALL).
-- The CHECK constraint guarantees each payment belongs to exactly one parent.
SELECT p.payment_id, 'Rental' AS paid_for, p.rental_id AS ref_id, p.amount, p.method, p.status
FROM payment p WHERE p.rental_id IS NOT NULL
UNION ALL
SELECT p.payment_id, 'Lease', p.lease_id, p.amount, p.method, p.status
FROM payment p WHERE p.lease_id IS NOT NULL
ORDER BY payment_id;

-- Q5. LEFT JOIN anti-pattern: vehicles that were never rented.
SELECT v.vehicle_id, v.registration_no, v.model
FROM vehicle v
LEFT JOIN rental r ON r.vehicle_id = v.vehicle_id
WHERE r.rental_id IS NULL;

-- ---------------------------------------------------------------------
-- B. GROUP BY / HAVING / AGGREGATES
-- ---------------------------------------------------------------------

-- Q6. Vehicles per category with average daily rate.
SELECT c.category_name, COUNT(*) AS vehicles, ROUND(AVG(v.rental_rate), 2) AS avg_daily_rate
FROM vehicle v
JOIN vehicle_category c ON c.category_id = v.category_id
GROUP BY c.category_name
ORDER BY vehicles DESC;

-- Q7. HAVING: customers with 2 or more rentals.
SELECT cu.customer_id, CONCAT(cu.first_name, ' ', cu.last_name) AS customer,
       COUNT(*) AS rentals
FROM rental r
JOIN customer cu ON cu.customer_id = r.customer_id
GROUP BY cu.customer_id, cu.first_name, cu.last_name
HAVING COUNT(*) >= 2
ORDER BY rentals DESC;

-- Q8. Revenue by month from Success payments only (report R-04).
SELECT DATE_FORMAT(payment_date, '%Y-%m') AS month, SUM(amount) AS revenue, COUNT(*) AS payments
FROM payment
WHERE status = 'Success'
GROUP BY DATE_FORMAT(payment_date, '%Y-%m')
ORDER BY month;

-- Q9. Most rented vehicles (report R-06), excluding cancelled rentals.
SELECT v.registration_no, v.model, COUNT(*) AS times_rented
FROM rental r
JOIN vehicle v ON v.vehicle_id = r.vehicle_id
WHERE r.status <> 'Cancelled'
GROUP BY v.vehicle_id, v.registration_no, v.model
ORDER BY times_rented DESC
LIMIT 5;

-- ---------------------------------------------------------------------
-- C. SUBQUERIES
-- ---------------------------------------------------------------------

-- Q10. Scalar subquery: vehicles priced above the fleet average rental rate.
SELECT registration_no, model, rental_rate
FROM vehicle
WHERE rental_rate > (SELECT AVG(rental_rate) FROM vehicle)
ORDER BY rental_rate DESC;

-- Q11. NOT EXISTS: customers who never rented or leased anything.
SELECT cu.customer_id, CONCAT(cu.first_name, ' ', cu.last_name) AS customer
FROM customer cu
WHERE NOT EXISTS (SELECT 1 FROM rental r WHERE r.customer_id = cu.customer_id)
  AND NOT EXISTS (SELECT 1 FROM lease  l WHERE l.customer_id = cu.customer_id);

-- Q12. Correlated subquery: each vehicle's most recent rental start.
SELECT v.registration_no,
       (SELECT MAX(r.scheduled_start_datetime) FROM rental r WHERE r.vehicle_id = v.vehicle_id) AS last_rental_start
FROM vehicle v
ORDER BY last_rental_start DESC;

-- Q13. Business-rule check: individuals whose licence is expired or expires within 30 days.
-- (Seed contains an expired-licence demo customer.)
SELECT cu.customer_id, CONCAT(cu.first_name, ' ', cu.last_name) AS customer,
       ic.license_no, ic.license_expiry,
       DATEDIFF(ic.license_expiry, CURDATE()) AS days_left
FROM individual_customer ic
JOIN customer cu ON cu.customer_id = ic.customer_id
WHERE ic.license_expiry <= CURDATE() + INTERVAL 30 DAY
ORDER BY ic.license_expiry;

-- ---------------------------------------------------------------------
-- D. VIEWS (all five)
-- ---------------------------------------------------------------------
SELECT * FROM vw_vehicle_overview LIMIT 10;   -- Q14 vehicle + names + derived 'Reserved' display status
SELECT * FROM vw_available_fleet;             -- Q15 vehicles free right now
SELECT * FROM vw_active_leases;               -- Q16 leases currently running
SELECT * FROM vw_rental_balance LIMIT 10;     -- Q17 charge, paid, outstanding per rental
SELECT * FROM vw_lease_balance LIMIT 10;      -- Q18 total, paid, outstanding per lease

-- ---------------------------------------------------------------------
-- E. THE DATE-OVERLAP AVAILABILITY QUERY
-- Overlap rule: start < to AND end > from (strict, so back-to-back is allowed).
-- Blocks: Confirmed rentals, Active rentals (overdue = open-ended),
--         Scheduled/Active leases, in-progress maintenance.
-- ---------------------------------------------------------------------
SET @from = NOW();
SET @to   = NOW() + INTERVAL 3 DAY;

-- Q19. Vehicles free for the WHOLE period [@from, @to).
SELECT v.vehicle_id, v.registration_no, v.model, v.rental_rate
FROM vehicle v
WHERE v.status NOT IN ('Maintenance', 'Inactive')
  AND NOT EXISTS (      -- Confirmed rentals
        SELECT 1 FROM rental r
        WHERE r.vehicle_id = v.vehicle_id AND r.status = 'Confirmed'
          AND r.scheduled_start_datetime < @to AND r.expected_return_datetime > @from)
  AND NOT EXISTS (      -- Active rentals; overdue ones are open-ended
        SELECT 1 FROM rental r
        WHERE r.vehicle_id = v.vehicle_id AND r.status = 'Active'
          AND r.actual_pickup_datetime < @to
          AND (r.expected_return_datetime > @from OR r.expected_return_datetime <= NOW()))
  AND NOT EXISTS (      -- Scheduled or Active leases (end_date is inclusive, so +1 day)
        SELECT 1 FROM lease l
        WHERE l.vehicle_id = v.vehicle_id AND l.status IN ('Scheduled', 'Active')
          AND l.start_date < @to AND DATE_ADD(l.end_date, INTERVAL 1 DAY) > @from)
  AND NOT EXISTS (      -- maintenance in progress blocks until completed
        SELECT 1 FROM maintenance m
        WHERE m.vehicle_id = v.vehicle_id AND m.status = 'In Progress'
          AND m.service_date < @to)
ORDER BY v.registration_no;

-- Q20. Overdue Active rentals: the reason for the open-ended branch.
-- These vehicles must be ABSENT from Q19 even though expected_return has passed.
SELECT r.rental_id, r.vehicle_id, r.expected_return_datetime,
       TIMESTAMPDIFF(HOUR, r.expected_return_datetime, NOW()) AS hours_overdue
FROM rental r
WHERE r.status = 'Active' AND r.expected_return_datetime <= NOW();

-- ---------------------------------------------------------------------
-- F. FUNCTIONS AND PROCEDURES
-- ---------------------------------------------------------------------

-- Q21a. fn_rental_charge: 49 hours -> CEIL(49/24)=3 days x 5000 = 15000.00
SELECT fn_rental_charge('2026-10-01 10:00:00', '2026-10-03 11:00:00', 5000.00) AS charge_3_days;
-- Q21b. Minimum one day: 2 hours still bills 1 day = 5000.00
SELECT fn_rental_charge('2026-10-01 10:00:00', '2026-10-01 12:00:00', 5000.00) AS charge_min_day;
-- Q21c. fn_lease_total: Jan 1 to Jun 30 -> 6 months x 100000 = 600000.00
SELECT fn_lease_total(100000.00, '2026-01-01', '2026-06-30') AS lease_total_6_months;

-- Q22. fn_customer_outstanding: customers who owe money (report R-05).
SELECT cu.customer_id, CONCAT(cu.first_name, ' ', cu.last_name) AS customer,
       fn_customer_outstanding(cu.customer_id) AS outstanding
FROM customer cu
HAVING outstanding > 0
ORDER BY outstanding DESC;

-- Q23. sp_available_vehicles(from, to, branch_id, category_id); NULL means "any".
CALL sp_available_vehicles(NOW(), NOW() + INTERVAL 3 DAY, NULL, NULL);
CALL sp_available_vehicles(NOW(), NOW() + INTERVAL 3 DAY, 1, NULL);   -- branch 1 only

-- Parity check: Q19 and the procedure must list the same vehicles (compare visually).

-- ---------------------------------------------------------------------
-- G. TRIGGER AND TRANSACTION DEMOS
-- ---------------------------------------------------------------------

-- Pick demo data dynamically (no hard-coded ids).
SET @veh  = (SELECT vehicle_id FROM vehicle WHERE status = 'Available' AND rental_rate IS NOT NULL
             ORDER BY vehicle_id LIMIT 1);
SET @rate = (SELECT rental_rate FROM vehicle WHERE vehicle_id = @veh);
SET @cust = (SELECT customer_id FROM individual_customer
             WHERE license_expiry > CURDATE() + INTERVAL 1 YEAR ORDER BY customer_id LIMIT 1);

-- Q24. Trigger trg_rental_after_insert + ROLLBACK.
-- Inserting an Active rental must flip the vehicle to 'Rented' by itself.
START TRANSACTION;
SELECT status AS vehicle_status_before FROM vehicle WHERE vehicle_id = @veh;
INSERT INTO rental (customer_id, vehicle_id, scheduled_start_datetime, expected_return_datetime,
                    actual_pickup_datetime, odometer_out, condition_out, daily_rate_applied, status)
VALUES (@cust, @veh, NOW(), NOW() + INTERVAL 2 DAY, NOW(), 1000, 'DEMO-06', @rate, 'Active');
SELECT status AS vehicle_status_after_trigger FROM vehicle WHERE vehicle_id = @veh;  -- expect Rented
ROLLBACK;
SELECT status AS vehicle_status_after_rollback FROM vehicle WHERE vehicle_id = @veh; -- back to Available

-- Q25. sp_complete_rental + trigger trg_rental_after_update.
-- IMPORTANT: sp_complete_rental runs its own START TRANSACTION/COMMIT, so it
-- cannot be rolled back from outside. We therefore create a DEMO rental,
-- complete it, show the result, then DELETE the demo row (cleanup).
DELETE FROM rental WHERE condition_out = 'DEMO-06';   -- leftovers from an earlier run
INSERT INTO rental (customer_id, vehicle_id, scheduled_start_datetime, expected_return_datetime,
                    actual_pickup_datetime, odometer_out, condition_out, daily_rate_applied, status)
VALUES (@cust, @veh, NOW() - INTERVAL 3 HOUR, NOW() + INTERVAL 1 DAY, NOW() - INTERVAL 3 HOUR,
        1000, 'DEMO-06', @rate, 'Active');
SET @demo = LAST_INSERT_ID();
SELECT status AS vehicle_status_while_rented FROM vehicle WHERE vehicle_id = @veh;   -- Rented

CALL sp_complete_rental(@demo, NOW(), 1250, 'DEMO returned');

SELECT rental_id, status, odometer_out, odometer_in, charges FROM rental WHERE rental_id = @demo;
SELECT fn_rental_charge(actual_pickup_datetime, actual_return_datetime, daily_rate_applied) AS recomputed
FROM rental WHERE rental_id = @demo;                  -- must equal charges above
SELECT status AS vehicle_status_after_completion FROM vehicle WHERE vehicle_id = @veh;  -- Available (trigger)

-- Cleanup: remove the demo row. Vehicle is already Available, so nothing else to restore.
DELETE FROM rental WHERE rental_id = @demo;
SELECT COUNT(*) AS demo_rows_left FROM rental WHERE condition_out = 'DEMO-06';          -- expect 0

-- ---------------------------------------------------------------------
-- H. DESIGN-INTEGRITY CHECKS (each must return 0 rows)
-- ---------------------------------------------------------------------

-- Q26. Subtype invariant: exactly one subtype row matching customer_type.
SELECT c.customer_id, c.customer_type
FROM customer c
LEFT JOIN individual_customer i ON i.customer_id = c.customer_id
LEFT JOIN corporate_customer  k ON k.customer_id = c.customer_id
WHERE (c.customer_type = 'Individual' AND (i.customer_id IS NULL OR k.customer_id IS NOT NULL))
   OR (c.customer_type = 'Corporate'  AND (k.customer_id IS NULL OR i.customer_id IS NOT NULL));

-- Q27. Stored status vs reality: vehicle marked Rented without an Active rental.
SELECT v.vehicle_id, v.registration_no, v.status
FROM vehicle v
WHERE v.status = 'Rented'
  AND NOT EXISTS (SELECT 1 FROM rental r WHERE r.vehicle_id = v.vehicle_id AND r.status = 'Active');

-- ---------------------------------------------------------------------
-- I. METADATA: triggers, views, constraints
-- ---------------------------------------------------------------------
SHOW TRIGGERS;                                       -- Q28 both rental triggers
SHOW CREATE VIEW vw_available_fleet;                 -- Q29 view definition
SHOW CREATE FUNCTION fn_rental_charge;               -- Q30 function body
SHOW CREATE PROCEDURE sp_complete_rental;            -- Q31 procedure body (transaction + handler)

-- Q32. All CHECK constraints in the schema (MySQL 8.0.16+).
SELECT tc.TABLE_NAME, tc.CONSTRAINT_NAME, cc.CHECK_CLAUSE
FROM information_schema.TABLE_CONSTRAINTS tc
JOIN information_schema.CHECK_CONSTRAINTS cc
  ON cc.CONSTRAINT_SCHEMA = tc.CONSTRAINT_SCHEMA AND cc.CONSTRAINT_NAME = tc.CONSTRAINT_NAME
WHERE tc.CONSTRAINT_SCHEMA = 'lfrlms' AND tc.CONSTRAINT_TYPE = 'CHECK'
ORDER BY tc.TABLE_NAME;

-- Q33. Indexes on rental.
SHOW INDEX FROM rental;

-- ---------------------------------------------------------------------
-- J. EXPLAIN BEFORE AND AFTER AN INDEX
-- actual_pickup_datetime has no index, so we add a scratch one and drop it.
-- (CREATE/DROP INDEX are DDL: they auto-commit, which is fine here.)
-- ---------------------------------------------------------------------

-- Q34a. BEFORE: expect type = ALL (full table scan), key = NULL.
EXPLAIN SELECT rental_id, vehicle_id, actual_pickup_datetime
FROM rental
WHERE actual_pickup_datetime >= NOW() - INTERVAL 30 DAY;

CREATE INDEX idx_demo_rental_pickup ON rental (actual_pickup_datetime);

-- Q34b. AFTER: expect type = range and key = idx_demo_rental_pickup.
-- On a table with only ~30 rows MySQL may still pick a scan; FORCE INDEX shows the plan.
EXPLAIN SELECT rental_id, vehicle_id, actual_pickup_datetime
FROM rental FORCE INDEX (idx_demo_rental_pickup)
WHERE actual_pickup_datetime >= NOW() - INTERVAL 30 DAY;

DROP INDEX idx_demo_rental_pickup ON rental;      -- cleanup

-- Q35. The real index: the overlap query's per-vehicle lookup uses
-- (vehicle_id, scheduled_start_datetime, expected_return_datetime).
EXPLAIN SELECT 1 FROM rental
WHERE vehicle_id = 1 AND status = 'Confirmed'
  AND scheduled_start_datetime < NOW() + INTERVAL 3 DAY
  AND expected_return_datetime > NOW();

SET SQL_SAFE_UPDATES = 1;
-- END OF SCRIPT
