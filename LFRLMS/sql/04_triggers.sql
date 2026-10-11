USE lfrlms;

DELIMITER $$

CREATE TRIGGER trg_rental_after_insert
AFTER INSERT ON rental
FOR EACH ROW
BEGIN
  IF NEW.status = 'Active' THEN
    UPDATE vehicle
    SET status = 'Rented'
    WHERE vehicle_id = NEW.vehicle_id;
  END IF;
END$$

CREATE TRIGGER trg_rental_after_update
AFTER UPDATE ON rental
FOR EACH ROW
BEGIN
  IF OLD.status = 'Confirmed' AND NEW.status = 'Active' THEN
    UPDATE vehicle
    SET status = 'Rented'
    WHERE vehicle_id = NEW.vehicle_id;
  ELSEIF OLD.status = 'Active' AND NEW.status = 'Completed' THEN
    UPDATE vehicle
    SET status = 'Available'
    WHERE vehicle_id = NEW.vehicle_id;
  END IF;
END$$

DELIMITER ;
