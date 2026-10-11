USE lfrlms;

DROP PROCEDURE IF EXISTS sp_complete_rental;
DROP PROCEDURE IF EXISTS sp_available_vehicles;
DROP FUNCTION IF EXISTS fn_customer_outstanding;
DROP FUNCTION IF EXISTS fn_lease_total;
DROP FUNCTION IF EXISTS fn_rental_charge;

DELIMITER $$

CREATE FUNCTION fn_rental_charge(
  p_start DATETIME,
  p_end DATETIME,
  p_daily_rate DECIMAL(10,2)
)
RETURNS DECIMAL(10,2)
DETERMINISTIC
NO SQL
BEGIN
  RETURN GREATEST(1, CEIL(TIMESTAMPDIFF(HOUR, p_start, p_end) / 24)) * p_daily_rate;
END$$

CREATE FUNCTION fn_lease_total(
  p_monthly DECIMAL(10,2),
  p_start DATE,
  p_end DATE
)
RETURNS DECIMAL(10,2)
DETERMINISTIC
NO SQL
BEGIN
  RETURN p_monthly * GREATEST(
    1,
    TIMESTAMPDIFF(MONTH, p_start, DATE_ADD(p_end, INTERVAL 1 DAY))
  );
END$$

CREATE FUNCTION fn_customer_outstanding(p_customer_id INT)
RETURNS DECIMAL(10,2)
READS SQL DATA
BEGIN
  DECLARE v_rental_total DECIMAL(10,2) DEFAULT 0.00;
  DECLARE v_rental_paid DECIMAL(10,2) DEFAULT 0.00;
  DECLARE v_lease_total DECIMAL(10,2) DEFAULT 0.00;
  DECLARE v_lease_paid DECIMAL(10,2) DEFAULT 0.00;

  SELECT COALESCE(SUM(
    CASE
      WHEN r.status = 'Completed' THEN r.charges
      ELSE fn_rental_charge(
        COALESCE(r.actual_pickup_datetime, r.scheduled_start_datetime),
        GREATEST(r.expected_return_datetime, NOW()),
        r.daily_rate_applied
      )
    END
  ), 0.00)
  INTO v_rental_total
  FROM rental r
  WHERE r.customer_id = p_customer_id
    AND r.status IN ('Active', 'Completed');

  SELECT COALESCE(SUM(p.amount), 0.00)
  INTO v_rental_paid
  FROM payment p
  JOIN rental r ON r.rental_id = p.rental_id
  WHERE r.customer_id = p_customer_id
    AND r.status IN ('Active', 'Completed')
    AND p.status = 'Success';

  SELECT COALESCE(SUM(fn_lease_total(l.monthly_charge, l.start_date, l.end_date)), 0.00)
  INTO v_lease_total
  FROM lease l
  WHERE l.customer_id = p_customer_id
    AND l.status IN ('Active', 'Completed');

  SELECT COALESCE(SUM(p.amount), 0.00)
  INTO v_lease_paid
  FROM payment p
  JOIN lease l ON l.lease_id = p.lease_id
  WHERE l.customer_id = p_customer_id
    AND l.status IN ('Active', 'Completed')
    AND p.status = 'Success';

  RETURN v_rental_total - v_rental_paid + v_lease_total - v_lease_paid;
END$$

CREATE PROCEDURE sp_available_vehicles(
  IN p_from DATETIME,
  IN p_to DATETIME,
  IN p_branch_id INT,
  IN p_category_id INT
)
READS SQL DATA
BEGIN
  IF p_from IS NULL OR p_to IS NULL OR p_to <= p_from THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Availability range must have an end later than its start';
  END IF;

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
    v.status
  FROM vehicle v
  JOIN brand b ON b.brand_id = v.brand_id
  JOIN vehicle_category vc ON vc.category_id = v.category_id
  JOIN branch br ON br.branch_id = v.branch_id
  WHERE v.status NOT IN ('Maintenance', 'Inactive')
    AND (p_branch_id IS NULL OR v.branch_id = p_branch_id)
    AND (p_category_id IS NULL OR v.category_id = p_category_id)
    AND NOT EXISTS (
      SELECT 1
      FROM rental r
      WHERE r.vehicle_id = v.vehicle_id
        AND r.status = 'Confirmed'
        AND r.scheduled_start_datetime < p_to
        AND r.expected_return_datetime > p_from
    )
    AND NOT EXISTS (
      SELECT 1
      FROM rental r
      WHERE r.vehicle_id = v.vehicle_id
        AND r.status = 'Active'
        AND r.actual_pickup_datetime < p_to
        AND (
          r.expected_return_datetime > p_from
          OR r.expected_return_datetime <= NOW()
        )
    )
    AND NOT EXISTS (
      SELECT 1
      FROM lease l
      WHERE l.vehicle_id = v.vehicle_id
        AND l.status IN ('Scheduled', 'Active')
        AND CAST(l.start_date AS DATETIME) < p_to
        AND CAST(DATE_ADD(l.end_date, INTERVAL 1 DAY) AS DATETIME) > p_from
    )
    AND NOT EXISTS (
      SELECT 1
      FROM maintenance m
      WHERE m.vehicle_id = v.vehicle_id
        AND m.status = 'In Progress'
        AND CAST(m.service_date AS DATETIME) < p_to
    )
  ORDER BY br.branch_name, vc.category_name, b.brand_name, v.model, v.vehicle_id;
END$$

CREATE PROCEDURE sp_complete_rental(
  IN p_rental_id INT,
  IN p_return_dt DATETIME,
  IN p_odometer_in INT,
  IN p_condition_in VARCHAR(255)
)
BEGIN
  DECLARE v_status VARCHAR(20) DEFAULT NULL;
  DECLARE v_pickup DATETIME DEFAULT NULL;
  DECLARE v_odometer_out INT DEFAULT NULL;
  DECLARE v_daily_rate DECIMAL(10,2) DEFAULT NULL;
  DECLARE v_not_found BOOLEAN DEFAULT FALSE;
  DECLARE v_charge DECIMAL(10,2) DEFAULT NULL;

  DECLARE CONTINUE HANDLER FOR NOT FOUND SET v_not_found = TRUE;
  DECLARE EXIT HANDLER FOR SQLEXCEPTION
  BEGIN
    ROLLBACK;
    RESIGNAL;
  END;

  START TRANSACTION;

  SELECT status, actual_pickup_datetime, odometer_out, daily_rate_applied
  INTO v_status, v_pickup, v_odometer_out, v_daily_rate
  FROM rental
  WHERE rental_id = p_rental_id
  FOR UPDATE;

  IF v_not_found THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Rental was not found';
  END IF;
  IF v_status <> 'Active' THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Only an Active rental can be completed';
  END IF;
  IF p_return_dt IS NULL OR p_return_dt <= v_pickup THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Return time must be later than pickup';
  END IF;
  IF p_odometer_in IS NULL OR p_odometer_in < v_odometer_out THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Return odometer cannot be below the pickup odometer';
  END IF;

  SET v_charge = fn_rental_charge(v_pickup, p_return_dt, v_daily_rate);

  UPDATE rental
  SET actual_return_datetime = p_return_dt,
      odometer_in = p_odometer_in,
      condition_in = p_condition_in,
      charges = v_charge,
      status = 'Completed'
  WHERE rental_id = p_rental_id;

  COMMIT;
END$$

DELIMITER ;
