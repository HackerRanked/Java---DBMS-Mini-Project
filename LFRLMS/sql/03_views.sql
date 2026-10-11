USE lfrlms;

CREATE OR REPLACE VIEW vw_vehicle_overview AS
SELECT
  v.vehicle_id,
  v.registration_no,
  b.brand_name,
  vc.category_name,
  br.branch_name,
  br.city AS branch_city,
  v.model,
  v.model_year,
  v.rental_rate,
  v.lease_rate,
  v.status AS stored_status,
  CASE
    WHEN v.status = 'Available'
      AND EXISTS (
        SELECT 1
        FROM rental r
        WHERE r.vehicle_id = v.vehicle_id
          AND r.status = 'Confirmed'
          AND r.scheduled_start_datetime > NOW()
          AND r.scheduled_start_datetime <= DATE_ADD(NOW(), INTERVAL 24 HOUR)
      )
    THEN 'Reserved'
    ELSE v.status
  END AS display_status
FROM vehicle v
JOIN brand b ON b.brand_id = v.brand_id
JOIN vehicle_category vc ON vc.category_id = v.category_id
JOIN branch br ON br.branch_id = v.branch_id;

-- “Right now” is represented as the half-open interval [NOW(), NOW() + 1 second).
CREATE OR REPLACE VIEW vw_available_fleet AS
SELECT
  overview.vehicle_id,
  overview.registration_no,
  overview.brand_name,
  overview.category_name,
  overview.branch_name,
  overview.branch_city,
  overview.model,
  overview.model_year,
  overview.rental_rate,
  overview.lease_rate,
  overview.stored_status AS status,
  overview.display_status
FROM vw_vehicle_overview overview
WHERE overview.stored_status NOT IN ('Maintenance', 'Inactive')
  AND NOT EXISTS (
    SELECT 1
    FROM rental r
    WHERE r.vehicle_id = overview.vehicle_id
      AND r.status = 'Confirmed'
      AND r.scheduled_start_datetime < DATE_ADD(NOW(), INTERVAL 1 SECOND)
      AND r.expected_return_datetime > NOW()
  )
  AND NOT EXISTS (
    SELECT 1
    FROM rental r
    WHERE r.vehicle_id = overview.vehicle_id
      AND r.status = 'Active'
      AND r.actual_pickup_datetime < DATE_ADD(NOW(), INTERVAL 1 SECOND)
      AND (
        r.expected_return_datetime > NOW()
        OR r.expected_return_datetime <= NOW()
      )
  )
  AND NOT EXISTS (
    SELECT 1
    FROM lease l
    WHERE l.vehicle_id = overview.vehicle_id
      AND l.status IN ('Scheduled', 'Active')
      AND CAST(l.start_date AS DATETIME) < DATE_ADD(NOW(), INTERVAL 1 SECOND)
      AND CAST(DATE_ADD(l.end_date, INTERVAL 1 DAY) AS DATETIME) > NOW()
  )
  AND NOT EXISTS (
    SELECT 1
    FROM maintenance m
    WHERE m.vehicle_id = overview.vehicle_id
      AND m.status = 'In Progress'
      AND CAST(m.service_date AS DATETIME) < DATE_ADD(NOW(), INTERVAL 1 SECOND)
  );

CREATE OR REPLACE VIEW vw_active_leases AS
SELECT
  l.lease_id,
  l.customer_id,
  CONCAT(c.first_name, ' ', c.last_name) AS customer_name,
  l.vehicle_id,
  v.registration_no,
  v.model,
  l.start_date,
  l.end_date,
  l.monthly_charge,
  l.deposit,
  l.status
FROM lease l
JOIN customer c ON c.customer_id = l.customer_id
JOIN vehicle v ON v.vehicle_id = l.vehicle_id
WHERE l.status = 'Active';

CREATE OR REPLACE VIEW vw_rental_balance AS
SELECT
  rental_totals.rental_id,
  rental_totals.customer_id,
  rental_totals.vehicle_id,
  rental_totals.status,
  rental_totals.scheduled_start_datetime,
  rental_totals.expected_return_datetime,
  rental_totals.actual_pickup_datetime,
  rental_totals.actual_return_datetime,
  rental_totals.daily_rate_applied,
  rental_totals.charge_total,
  COALESCE(payment_totals.paid_amount, 0.00) AS paid_amount,
  rental_totals.charge_total - COALESCE(payment_totals.paid_amount, 0.00) AS balance_due
FROM (
  SELECT
    r.rental_id,
    r.customer_id,
    r.vehicle_id,
    r.status,
    r.scheduled_start_datetime,
    r.expected_return_datetime,
    r.actual_pickup_datetime,
    r.actual_return_datetime,
    r.daily_rate_applied,
    CASE
      WHEN r.status = 'Completed' THEN r.charges
      ELSE fn_rental_charge(
        COALESCE(r.actual_pickup_datetime, r.scheduled_start_datetime),
        GREATEST(r.expected_return_datetime, NOW()),
        r.daily_rate_applied
      )
    END AS charge_total
  FROM rental r
  WHERE r.status IN ('Active', 'Completed')
) rental_totals
LEFT JOIN (
  SELECT rental_id, SUM(amount) AS paid_amount
  FROM payment
  WHERE rental_id IS NOT NULL AND status = 'Success'
  GROUP BY rental_id
) payment_totals ON payment_totals.rental_id = rental_totals.rental_id;

CREATE OR REPLACE VIEW vw_lease_balance AS
SELECT
  lease_totals.lease_id,
  lease_totals.customer_id,
  lease_totals.vehicle_id,
  lease_totals.status,
  lease_totals.start_date,
  lease_totals.end_date,
  lease_totals.monthly_charge,
  lease_totals.deposit,
  lease_totals.lease_total,
  COALESCE(payment_totals.paid_amount, 0.00) AS paid_amount,
  lease_totals.lease_total - COALESCE(payment_totals.paid_amount, 0.00) AS balance_due
FROM (
  SELECT
    l.lease_id,
    l.customer_id,
    l.vehicle_id,
    l.status,
    l.start_date,
    l.end_date,
    l.monthly_charge,
    l.deposit,
    fn_lease_total(l.monthly_charge, l.start_date, l.end_date) AS lease_total
  FROM lease l
  WHERE l.status IN ('Active', 'Completed')
) lease_totals
LEFT JOIN (
  SELECT lease_id, SUM(amount) AS paid_amount
  FROM payment
  WHERE lease_id IS NOT NULL AND status = 'Success'
  GROUP BY lease_id
) payment_totals ON payment_totals.lease_id = lease_totals.lease_id;
