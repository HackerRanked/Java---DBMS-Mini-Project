USE lfrlms;

-- Each statement below must fail. The batch client uses --force to continue.

-- Duplicate registration number.
START TRANSACTION;
INSERT INTO vehicle
 (registration_no,brand_id,category_id,branch_id,model,model_year,rental_rate,lease_rate)
VALUES ('MH12AB1234',1,1,1,'Duplicate registration case',2024,1000.00,10000.00);
ROLLBACK;

-- Payment cannot point to both parent records.
START TRANSACTION;
INSERT INTO payment (rental_id,lease_id,amount,method,status)
VALUES (17,1,1.00,'Cash','Success');
ROLLBACK;

-- Payment must point to exactly one parent record.
START TRANSACTION;
INSERT INTO payment (rental_id,lease_id,amount,method,status)
VALUES (NULL,NULL,1.00,'Cash','Success');
ROLLBACK;

-- Expected return must follow scheduled start.
START TRANSACTION;
INSERT INTO rental
 (customer_id,vehicle_id,scheduled_start_datetime,expected_return_datetime,daily_rate_applied,status)
VALUES (1,22,DATE_ADD(NOW(),INTERVAL 2 DAY),DATE_ADD(NOW(),INTERVAL 1 DAY),16000.00,'Confirmed');
ROLLBACK;

-- Active rental requires an odometer reading at pickup.
START TRANSACTION;
INSERT INTO rental
 (customer_id,vehicle_id,scheduled_start_datetime,expected_return_datetime,
  actual_pickup_datetime,odometer_out,daily_rate_applied,status)
VALUES (1,22,NOW(),DATE_ADD(NOW(),INTERVAL 1 DAY),NOW(),NULL,16000.00,'Active');
ROLLBACK;

-- Completed rental requires charges.
START TRANSACTION;
INSERT INTO rental
 (customer_id,vehicle_id,scheduled_start_datetime,expected_return_datetime,
  actual_pickup_datetime,actual_return_datetime,odometer_out,odometer_in,
  daily_rate_applied,charges,status)
VALUES (1,22,DATE_SUB(NOW(),INTERVAL 2 DAY),NOW(),
        DATE_SUB(NOW(),INTERVAL 2 DAY),DATE_SUB(NOW(),INTERVAL 1 DAY),
        10000,10100,16000.00,NULL,'Completed');
ROLLBACK;

-- Actual return time must be strictly later than pickup.
START TRANSACTION;
INSERT INTO rental
 (customer_id,vehicle_id,scheduled_start_datetime,expected_return_datetime,
  actual_pickup_datetime,actual_return_datetime,odometer_out,odometer_in,
  daily_rate_applied,charges,status)
VALUES (1,22,DATE_SUB(NOW(),INTERVAL 2 DAY),NOW(),
        DATE_SUB(NOW(),INTERVAL 1 DAY),DATE_SUB(NOW(),INTERVAL 1 DAY),
        10000,10100,16000.00,16000.00,'Completed');
ROLLBACK;

-- Customer foreign key must exist.
START TRANSACTION;
INSERT INTO rental
 (customer_id,vehicle_id,scheduled_start_datetime,expected_return_datetime,daily_rate_applied,status)
VALUES (99999,22,DATE_ADD(NOW(),INTERVAL 2 DAY),DATE_ADD(NOW(),INTERVAL 3 DAY),16000.00,'Confirmed');
ROLLBACK;

-- Return odometer cannot be below the pickup odometer.
START TRANSACTION;
INSERT INTO rental
 (customer_id,vehicle_id,scheduled_start_datetime,expected_return_datetime,
  actual_pickup_datetime,actual_return_datetime,odometer_out,odometer_in,
  daily_rate_applied,charges,status)
VALUES (1,22,DATE_SUB(NOW(),INTERVAL 2 DAY),NOW(),
        DATE_SUB(NOW(),INTERVAL 2 DAY),DATE_SUB(NOW(),INTERVAL 1 DAY),
        10100,10000,16000.00,32000.00,'Completed');
ROLLBACK;

-- A referenced brand cannot be deleted.
START TRANSACTION;
DELETE FROM brand WHERE brand_id=1;
ROLLBACK;
