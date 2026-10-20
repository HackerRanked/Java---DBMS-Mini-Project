USE lfrlms;

SELECT status, COUNT(*)
FROM rental
GROUP BY status;

SELECT rental_id, vehicle_id, actual_pickup_datetime, expected_return_datetime
FROM rental
WHERE status = 'Active';

USE lfrlms;

SHOW FULL TABLES
WHERE Table_type = 'BASE TABLE';


SELECT COUNT(*) AS vehicle_count
FROM vehicle;

SELECT status, COUNT(*) AS total
FROM vehicle
GROUP BY status
ORDER BY status;

SELECT status, COUNT(*) AS total
FROM rental
GROUP BY status
ORDER BY status;

SELECT
    v.vehicle_id,
    v.registration_no,
    v.status AS vehicle_status,
    r.rental_id,
    r.status AS rental_status
FROM vehicle v
JOIN rental r
    ON r.vehicle_id = v.vehicle_id
WHERE r.status = 'Active'
  AND v.status <> 'Rented';