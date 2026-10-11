-- LFRLMS schema. MySQL 8.0.16 or newer is required for enforced CHECK constraints.
CREATE DATABASE IF NOT EXISTS lfrlms
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_0900_ai_ci;
USE lfrlms;

CREATE TABLE brand (
  brand_id INT NOT NULL AUTO_INCREMENT,
  brand_name VARCHAR(100) NOT NULL,
  PRIMARY KEY (brand_id),
  CONSTRAINT uq_brand_name UNIQUE (brand_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE vehicle_category (
  category_id INT NOT NULL AUTO_INCREMENT,
  category_name VARCHAR(100) NOT NULL,
  description VARCHAR(255),
  PRIMARY KEY (category_id),
  CONSTRAINT uq_vehicle_category_name UNIQUE (category_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE branch (
  branch_id INT NOT NULL AUTO_INCREMENT,
  branch_name VARCHAR(100) NOT NULL,
  street VARCHAR(180),
  city VARCHAR(80),
  pincode CHAR(6),
  phone CHAR(10),
  PRIMARY KEY (branch_id),
  CONSTRAINT uq_branch_name UNIQUE (branch_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE customer (
  customer_id INT NOT NULL AUTO_INCREMENT,
  customer_type ENUM('Individual', 'Corporate') NOT NULL,
  first_name VARCHAR(80) NOT NULL,
  last_name VARCHAR(80) NOT NULL,
  email VARCHAR(150),
  phone CHAR(10),
  street VARCHAR(180),
  city VARCHAR(80),
  pincode CHAR(6),
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (customer_id),
  CONSTRAINT uq_customer_email UNIQUE (email),
  INDEX ix_customer_name (last_name, first_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE individual_customer (
  customer_id INT NOT NULL,
  date_of_birth DATE NOT NULL,
  license_no VARCHAR(30) NOT NULL,
  license_expiry DATE NOT NULL,
  PRIMARY KEY (customer_id),
  CONSTRAINT uq_individual_license UNIQUE (license_no),
  CONSTRAINT fk_individual_customer_customer FOREIGN KEY (customer_id)
    REFERENCES customer (customer_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE corporate_customer (
  customer_id INT NOT NULL,
  company_name VARCHAR(150) NOT NULL,
  gst_no CHAR(15) NOT NULL,
  PRIMARY KEY (customer_id),
  CONSTRAINT uq_corporate_customer_gst UNIQUE (gst_no),
  CONSTRAINT fk_corporate_customer_customer FOREIGN KEY (customer_id)
    REFERENCES customer (customer_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE driver (
  driver_id INT NOT NULL AUTO_INCREMENT,
  first_name VARCHAR(80) NOT NULL,
  last_name VARCHAR(80) NOT NULL,
  phone CHAR(10) NOT NULL,
  license_no VARCHAR(30) NOT NULL,
  status ENUM('Active', 'Inactive') NOT NULL DEFAULT 'Active',
  PRIMARY KEY (driver_id),
  CONSTRAINT uq_driver_license UNIQUE (license_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE vehicle (
  vehicle_id INT NOT NULL AUTO_INCREMENT,
  registration_no VARCHAR(15) NOT NULL,
  brand_id INT NOT NULL,
  category_id INT NOT NULL,
  branch_id INT NOT NULL,
  model VARCHAR(100) NOT NULL,
  model_year SMALLINT NOT NULL,
  rental_rate DECIMAL(10,2),
  lease_rate DECIMAL(10,2),
  status ENUM('Available', 'Reserved', 'Rented', 'Leased', 'Maintenance', 'Inactive')
    NOT NULL DEFAULT 'Available',
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (vehicle_id),
  CONSTRAINT uq_vehicle_registration UNIQUE (registration_no),
  CONSTRAINT ck_vehicle_registration_format CHECK
    (registration_no REGEXP '^[A-Z]{2}[0-9]{2}[A-Z]{1,3}[0-9]{4}$'),
  CONSTRAINT ck_vehicle_model_year CHECK (model_year BETWEEN 2000 AND 2100),
  CONSTRAINT ck_vehicle_rental_rate CHECK (rental_rate IS NULL OR rental_rate >= 0),
  CONSTRAINT ck_vehicle_lease_rate CHECK (lease_rate IS NULL OR lease_rate >= 0),
  CONSTRAINT ck_vehicle_has_rate CHECK (rental_rate IS NOT NULL OR lease_rate IS NOT NULL),
  CONSTRAINT fk_vehicle_brand FOREIGN KEY (brand_id) REFERENCES brand (brand_id),
  CONSTRAINT fk_vehicle_category FOREIGN KEY (category_id) REFERENCES vehicle_category (category_id),
  CONSTRAINT fk_vehicle_branch FOREIGN KEY (branch_id) REFERENCES branch (branch_id),
  INDEX ix_vehicle_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE rental (
  rental_id INT NOT NULL AUTO_INCREMENT,
  customer_id INT NOT NULL,
  vehicle_id INT NOT NULL,
  driver_id INT,
  scheduled_start_datetime DATETIME NOT NULL,
  expected_return_datetime DATETIME NOT NULL,
  actual_pickup_datetime DATETIME,
  actual_return_datetime DATETIME,
  odometer_out INT,
  odometer_in INT,
  condition_out VARCHAR(255),
  condition_in VARCHAR(255),
  daily_rate_applied DECIMAL(10,2) NOT NULL,
  charges DECIMAL(10,2),
  status ENUM('Confirmed', 'Active', 'Completed', 'Cancelled')
    NOT NULL DEFAULT 'Confirmed',
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (rental_id),
  CONSTRAINT ck_rental_expected_after_scheduled
    CHECK (expected_return_datetime > scheduled_start_datetime),
  CONSTRAINT ck_rental_pickup_and_odometer
    CHECK (status NOT IN ('Active', 'Completed') OR
      (actual_pickup_datetime IS NOT NULL AND odometer_out IS NOT NULL)),
  CONSTRAINT ck_rental_completed_details
    CHECK (status <> 'Completed' OR
      (actual_return_datetime IS NOT NULL AND odometer_in IS NOT NULL AND charges IS NOT NULL)),
  CONSTRAINT ck_rental_odometer_in CHECK (odometer_in IS NULL OR odometer_in >= odometer_out),
  CONSTRAINT ck_rental_actual_return
    CHECK (actual_return_datetime IS NULL OR actual_return_datetime > actual_pickup_datetime),
  CONSTRAINT fk_rental_customer FOREIGN KEY (customer_id) REFERENCES customer (customer_id),
  CONSTRAINT fk_rental_vehicle FOREIGN KEY (vehicle_id) REFERENCES vehicle (vehicle_id),
  CONSTRAINT fk_rental_driver FOREIGN KEY (driver_id) REFERENCES driver (driver_id),
  INDEX ix_rental_vehicle_dates
    (vehicle_id, scheduled_start_datetime, expected_return_datetime)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE lease (
  lease_id INT NOT NULL AUTO_INCREMENT,
  customer_id INT NOT NULL,
  vehicle_id INT NOT NULL,
  start_date DATE NOT NULL,
  end_date DATE NOT NULL,
  monthly_charge DECIMAL(10,2) NOT NULL,
  deposit DECIMAL(10,2) NOT NULL DEFAULT 0,
  status ENUM('Scheduled', 'Active', 'Completed', 'Terminated') NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (lease_id),
  CONSTRAINT ck_lease_end_after_start CHECK (end_date > start_date),
  CONSTRAINT fk_lease_customer FOREIGN KEY (customer_id) REFERENCES customer (customer_id),
  CONSTRAINT fk_lease_vehicle FOREIGN KEY (vehicle_id) REFERENCES vehicle (vehicle_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE payment (
  payment_id INT NOT NULL AUTO_INCREMENT,
  rental_id INT,
  lease_id INT,
  amount DECIMAL(10,2) NOT NULL,
  method ENUM('Cash', 'Card', 'Bank Transfer', 'UPI') NOT NULL,
  payment_date DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  status ENUM('Success', 'Pending', 'Failed', 'Refunded') NOT NULL DEFAULT 'Success',
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (payment_id),
  CONSTRAINT ck_payment_positive_amount CHECK (amount > 0),
  CONSTRAINT ck_payment_exactly_one_parent
    CHECK ((rental_id IS NULL) <> (lease_id IS NULL)),
  CONSTRAINT fk_payment_rental FOREIGN KEY (rental_id) REFERENCES rental (rental_id),
  CONSTRAINT fk_payment_lease FOREIGN KEY (lease_id) REFERENCES lease (lease_id),
  INDEX ix_payment_date (payment_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE maintenance (
  maintenance_id INT NOT NULL AUTO_INCREMENT,
  vehicle_id INT NOT NULL,
  service_date DATE NOT NULL,
  service_type VARCHAR(100) NOT NULL,
  description VARCHAR(500),
  cost DECIMAL(10,2) NOT NULL,
  next_service_date DATE,
  completed_date DATE,
  status ENUM('In Progress', 'Complete') NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (maintenance_id),
  CONSTRAINT ck_maintenance_cost CHECK (cost >= 0),
  CONSTRAINT fk_maintenance_vehicle FOREIGN KEY (vehicle_id) REFERENCES vehicle (vehicle_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
