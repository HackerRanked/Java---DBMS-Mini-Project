USE lfrlms;

INSERT INTO brand (brand_id, brand_name) VALUES
 (1,'Mercedes-Benz'),(2,'BMW'),(3,'Audi'),(4,'Lexus'),(5,'Porsche'),(6,'Range Rover');
INSERT INTO vehicle_category (category_id,category_name,description) VALUES
 (1,'Luxury Sedan','Executive four-door vehicles'),(2,'Luxury SUV','Premium sport utility vehicles'),
 (3,'Sports Car','High-performance two-door vehicles'),(4,'Luxury Coupe','Premium grand tourers'),
 (5,'Electric Luxury','Premium battery electric vehicles');
INSERT INTO branch (branch_id,branch_name,street,city,pincode,phone) VALUES
 (1,'Nashik Central','College Road','Nashik','422005','9876501001'),
 (2,'Pune Airport','Viman Nagar','Pune','411014','9876501002'),
 (3,'Mumbai South','Bandra Kurla Complex','Mumbai','400051','9876501003');

INSERT INTO customer
 (customer_id,customer_type,first_name,last_name,email,phone,street,city,pincode) VALUES
 (1,'Individual','Aarav','Shah','aarav.shah@example.com','9000000001','College Road','Nashik','422005'),
 (2,'Individual','Anaya','Deshmukh','anaya.deshmukh@example.com','9000000002','Baner Road','Pune','411045'),
 (3,'Individual','Vivaan','Patil','vivaan.patil@example.com','9000000003','Marine Drive','Mumbai','400020'),
 (4,'Individual','Ira','Kulkarni','ira.kulkarni@example.com','9000000004','Gangapur Road','Nashik','422013'),
 (5,'Individual','Reyansh','Joshi','reyansh.joshi@example.com','9000000005','Koregaon Park','Pune','411001'),
 (6,'Individual','Sara','Mehta','sara.mehta@example.com','9000000006','Powai','Mumbai','400076'),
 (7,'Individual','Advik','Rao','advik.rao@example.com','9000000007','Indira Nagar','Nashik','422009'),
 (8,'Individual','Myra','Nair','myra.nair@example.com','9000000008','Aundh','Pune','411007'),
 (9,'Individual','Kabir','Jain','kabir.jain@example.com','9000000009','Juhu','Mumbai','400049'),
 (10,'Individual','Kiara','Pawar','kiara.pawar@example.com','9000000010','College Road','Nashik','422005'),
 (11,'Individual','Arjun','Bose','arjun.bose@example.com','9000000011','Kalyani Nagar','Pune','411006'),
 (12,'Individual','Diya','Kapoor','diya.kapoor@example.com','9000000012','Worli','Mumbai','400018'),
 (13,'Individual','Atharv','Sane','atharv.sane@example.com','9000000013','Panchavati','Nashik','422003'),
 (14,'Individual','Saanvi','Reddy','saanvi.reddy@example.com','9000000014','Kothrud','Pune','411038'),
 (15,'Individual','Ishaan','Malhotra','ishaan.malhotra@example.com','9000000015','Andheri West','Mumbai','400053'),
 (16,'Individual','Tara','Gokhale','tara.gokhale@example.com','9000000016','Satpur','Nashik','422007'),
 (17,'Corporate','Neha','Desai','fleet@weststar.example.com','9000000017','MIDC','Nashik','422010'),
 (18,'Corporate','Rohan','Kamat','travel@bluepeak.example.com','9000000018','Hinjawadi','Pune','411057'),
 (19,'Corporate','Meera','Seth','admin@silverline.example.com','9000000019','Bandra East','Mumbai','400051'),
 (20,'Corporate','Kunal','Bhat','office@crestview.example.com','9000000020','Nashik Road','Nashik','422101'),
 (21,'Corporate','Nisha','Kale','mobility@evergreen.example.com','9000000021','Shivaji Nagar','Pune','411005'),
 (22,'Corporate','Dev','Fernandes','accounts@horizon.example.com','9000000022','Lower Parel','Mumbai','400013');

INSERT INTO individual_customer (customer_id,date_of_birth,license_no,license_expiry) VALUES
 (1,DATE_SUB(CURDATE(),INTERVAL 34 YEAR),'MH-LIC-IND-0001',DATE_ADD(CURDATE(),INTERVAL 730 DAY)),
 (2,DATE_SUB(CURDATE(),INTERVAL 36 YEAR),'MH-LIC-IND-0002',DATE_ADD(CURDATE(),INTERVAL 730 DAY)),
 (3,DATE_SUB(CURDATE(),INTERVAL 38 YEAR),'MH-LIC-IND-0003',DATE_ADD(CURDATE(),INTERVAL 730 DAY)),
 (4,DATE_SUB(CURDATE(),INTERVAL 31 YEAR),'MH-LIC-IND-0004',DATE_ADD(CURDATE(),INTERVAL 730 DAY)),
 (5,DATE_SUB(CURDATE(),INTERVAL 39 YEAR),'MH-LIC-IND-0005',DATE_ADD(CURDATE(),INTERVAL 730 DAY)),
 (6,DATE_SUB(CURDATE(),INTERVAL 33 YEAR),'MH-LIC-IND-0006',DATE_ADD(CURDATE(),INTERVAL 730 DAY)),
 (7,DATE_SUB(CURDATE(),INTERVAL 37 YEAR),'MH-LIC-IND-0007',DATE_ADD(CURDATE(),INTERVAL 730 DAY)),
 (8,DATE_SUB(CURDATE(),INTERVAL 35 YEAR),'MH-LIC-IND-0008',DATE_ADD(CURDATE(),INTERVAL 730 DAY)),
 (9,DATE_SUB(CURDATE(),INTERVAL 40 YEAR),'MH-LIC-IND-0009',DATE_ADD(CURDATE(),INTERVAL 730 DAY)),
 (10,DATE_SUB(CURDATE(),INTERVAL 32 YEAR),'MH-LIC-IND-0010',DATE_ADD(CURDATE(),INTERVAL 730 DAY)),
 (11,DATE_SUB(CURDATE(),INTERVAL 41 YEAR),'MH-LIC-IND-0011',DATE_ADD(CURDATE(),INTERVAL 730 DAY)),
 (12,DATE_SUB(CURDATE(),INTERVAL 30 YEAR),'MH-LIC-IND-0012',DATE_ADD(CURDATE(),INTERVAL 730 DAY)),
 (13,DATE_SUB(CURDATE(),INTERVAL 42 YEAR),'MH-LIC-IND-0013',DATE_ADD(CURDATE(),INTERVAL 730 DAY)),
 (14,DATE_SUB(CURDATE(),INTERVAL 34 YEAR),'MH-LIC-IND-0014',DATE_ADD(CURDATE(),INTERVAL 730 DAY)),
 (15,DATE_SUB(CURDATE(),INTERVAL 43 YEAR),'MH-LIC-IND-0015',DATE_ADD(CURDATE(),INTERVAL 730 DAY)),
 (16,DATE_SUB(CURDATE(),INTERVAL 45 YEAR),'MH-LIC-IND-0016',DATE_SUB(CURDATE(),INTERVAL 30 DAY));
INSERT INTO corporate_customer (customer_id,company_name,gst_no) VALUES
 (17,'Weststar Technologies','27ABCDE1234F1Z5'),
 (18,'Bluepeak Finance','27BCDEF2345G1Z6'),
 (19,'Silverline Media','27CDEFG3456H1Z7'),
 (20,'Crestview Realty','27DEFGH4567J1Z8'),
 (21,'Evergreen Health','27EFGHI5678K1Z9'),
 (22,'Horizon Analytics','27FGHIJ6789L1Z0');

INSERT INTO driver (driver_id,first_name,last_name,phone,license_no,status) VALUES
 (1,'Sameer','Khan','9100000001','MH-DRV-0001','Active'),
 (2,'Pranav','More','9100000002','MH-DRV-0002','Active'),
 (3,'Nitin','Jadhav','9100000003','MH-DRV-0003','Active'),
 (4,'Farhan','Shaikh','9100000004','MH-DRV-0004','Active'),
 (5,'Suresh','Pawar','9100000005','MH-DRV-0005','Active'),
 (6,'Amit','Shinde','9100000006','MH-DRV-0006','Active');

INSERT INTO vehicle
 (vehicle_id,registration_no,brand_id,category_id,branch_id,model,model_year,rental_rate,lease_rate) VALUES
 (1,'MH12AB1234',1,1,1,'S-Class',2024,24000,420000),
 (2,'MH14CD2345',2,1,2,'7 Series',2023,22000,400000),
 (3,'MH01EF3456',3,1,3,'A8 L',2024,21000,390000),
 (4,'MH02GH4567',4,2,1,'LX 500d',2023,20000,380000),
 (5,'MH03IJ5678',5,3,2,'911 Carrera',2025,28000,500000),
 (6,'MH04KL6789',6,2,3,'Range Rover Sport',2024,26000,470000),
 (7,'MH12MN7890',1,1,1,'E-Class',2022,12000,250000),
 (8,'MH14OP8901',2,2,2,'X5',2023,14000,290000),
 (9,'MH01QR9012',3,3,3,'R8',2021,25000,460000),
 (10,'MH02ST0123',4,1,1,'ES 300h',2024,11000,230000),
 (11,'MH03UV1234',5,4,2,'Panamera',2023,18000,340000),
 (12,'MH04WX2345',6,2,3,'Defender',2024,16000,320000),
 (13,'MH12YZ3456',1,5,1,'EQS',2025,19000,360000),
 (14,'MH14AB4567',2,5,2,'i7',2024,20000,380000),
 (15,'MH01CD5678',3,2,3,'Q8',2023,17000,330000),
 (16,'MH02EF6789',4,2,1,'RX 500h',2025,15000,300000),
 (17,'MH03GH7890',5,3,2,'718 Cayman',2024,17000,330000),
 (18,'MH04IJ8901',6,2,3,'Range Rover Velar',2023,18000,350000),
 (19,'MH12KL9012',1,1,1,'C-Class',2024,10000,210000),
 (20,'MH14MN0123',2,1,2,'5 Series',2022,11000,220000),
 (21,'MH01OP1234',3,2,3,'Q7',2021,13000,270000),
 (22,'MH02QR2345',4,1,1,'LS 500',2023,16000,310000),
 (23,'MH03ST3456',5,3,2,'Macan',2024,15000,300000),
 (24,'MH04UV4567',6,2,3,'Discovery',2022,14000,280000);

-- The first Confirmed reservation starts in about three hours. Candidate overlap-demo
-- requests for vehicle 23 share a future interval, but are not stored commitments.
INSERT INTO rental
 (rental_id,customer_id,vehicle_id,driver_id,scheduled_start_datetime,expected_return_datetime,
  actual_pickup_datetime,actual_return_datetime,odometer_out,odometer_in,condition_out,condition_in,
  daily_rate_applied,charges,status) VALUES
 (1,1,7,NULL,DATE_ADD(NOW(),INTERVAL 3 HOUR),DATE_ADD(DATE_ADD(NOW(),INTERVAL 3 HOUR),INTERVAL 2 DAY),NULL,NULL,NULL,NULL,NULL,NULL,12000,NULL,'Confirmed'),
 (2,2,8,NULL,DATE_ADD(NOW(),INTERVAL 2 DAY),DATE_ADD(NOW(),INTERVAL 4 DAY),NULL,NULL,NULL,NULL,NULL,NULL,14000,NULL,'Confirmed'),
 (3,3,9,NULL,DATE_ADD(NOW(),INTERVAL 5 DAY),DATE_ADD(NOW(),INTERVAL 7 DAY),NULL,NULL,NULL,NULL,NULL,NULL,25000,NULL,'Confirmed'),
 (4,4,10,NULL,DATE_ADD(NOW(),INTERVAL 8 DAY),DATE_ADD(NOW(),INTERVAL 10 DAY),NULL,NULL,NULL,NULL,NULL,NULL,11000,NULL,'Confirmed'),
 (5,5,11,NULL,DATE_ADD(NOW(),INTERVAL 11 DAY),DATE_ADD(NOW(),INTERVAL 13 DAY),NULL,NULL,NULL,NULL,NULL,NULL,18000,NULL,'Confirmed'),
 (6,6,12,NULL,DATE_ADD(NOW(),INTERVAL 14 DAY),DATE_ADD(NOW(),INTERVAL 16 DAY),NULL,NULL,NULL,NULL,NULL,NULL,16000,NULL,'Confirmed'),
 (7,7,13,NULL,DATE_ADD(NOW(),INTERVAL 17 DAY),DATE_ADD(NOW(),INTERVAL 19 DAY),NULL,NULL,NULL,NULL,NULL,NULL,19000,NULL,'Confirmed'),
 (8,8,14,NULL,DATE_ADD(NOW(),INTERVAL 20 DAY),DATE_ADD(NOW(),INTERVAL 22 DAY),NULL,NULL,NULL,NULL,NULL,NULL,20000,NULL,'Confirmed'),
 (9,9,15,NULL,DATE_ADD(NOW(),INTERVAL 23 DAY),DATE_ADD(NOW(),INTERVAL 25 DAY),NULL,NULL,NULL,NULL,NULL,NULL,17000,NULL,'Confirmed'),
 (10,10,16,NULL,DATE_ADD(NOW(),INTERVAL 26 DAY),DATE_ADD(NOW(),INTERVAL 28 DAY),NULL,NULL,NULL,NULL,NULL,NULL,15000,NULL,'Confirmed'),
 (11,1,1,NULL,DATE_SUB(NOW(),INTERVAL 3 DAY),DATE_SUB(NOW(),INTERVAL 1 HOUR),DATE_SUB(NOW(),INTERVAL 3 DAY),NULL,45000,NULL,'Clean',NULL,24000,NULL,'Active'),
 (12,2,2,NULL,DATE_SUB(NOW(),INTERVAL 2 HOUR),DATE_ADD(NOW(),INTERVAL 1 DAY),DATE_SUB(NOW(),INTERVAL 2 HOUR),NULL,38000,NULL,'Clean',NULL,22000,NULL,'Active'),
 (13,17,3,1,DATE_SUB(NOW(),INTERVAL 5 HOUR),DATE_ADD(NOW(),INTERVAL 18 HOUR),DATE_SUB(NOW(),INTERVAL 5 HOUR),NULL,52000,NULL,'Clean',NULL,21000,NULL,'Active'),
 (14,3,4,NULL,DATE_SUB(NOW(),INTERVAL 1 DAY),DATE_ADD(NOW(),INTERVAL 1 DAY),DATE_SUB(NOW(),INTERVAL 1 DAY),NULL,29000,NULL,'Clean',NULL,20000,NULL,'Active'),
 (15,18,5,2,DATE_SUB(NOW(),INTERVAL 8 HOUR),DATE_ADD(NOW(),INTERVAL 1 DAY),DATE_SUB(NOW(),INTERVAL 8 HOUR),NULL,17000,NULL,'Clean',NULL,28000,NULL,'Active'),
 (16,4,6,NULL,DATE_SUB(NOW(),INTERVAL 1 HOUR),DATE_ADD(NOW(),INTERVAL 2 DAY),DATE_SUB(NOW(),INTERVAL 1 HOUR),NULL,61000,NULL,'Clean',NULL,26000,NULL,'Active'),
 (17,5,7,NULL,DATE_SUB(NOW(),INTERVAL 60 DAY),DATE_SUB(NOW(),INTERVAL 57 DAY),DATE_SUB(NOW(),INTERVAL 60 DAY),DATE_SUB(NOW(),INTERVAL 58 DAY),21000,21400,'Clean','Clean',12000,fn_rental_charge(DATE_SUB(NOW(),INTERVAL 60 DAY),DATE_SUB(NOW(),INTERVAL 58 DAY),12000),'Completed'),
 (18,6,8,NULL,DATE_SUB(NOW(),INTERVAL 55 DAY),DATE_SUB(NOW(),INTERVAL 52 DAY),DATE_SUB(NOW(),INTERVAL 55 DAY),DATE_SUB(NOW(),INTERVAL 53 DAY),32000,32400,'Clean','Clean',14000,fn_rental_charge(DATE_SUB(NOW(),INTERVAL 55 DAY),DATE_SUB(NOW(),INTERVAL 53 DAY),14000),'Completed'),
 (19,7,9,NULL,DATE_SUB(NOW(),INTERVAL 50 DAY),DATE_SUB(NOW(),INTERVAL 47 DAY),DATE_SUB(NOW(),INTERVAL 50 DAY),DATE_SUB(NOW(),INTERVAL 48 DAY),18000,18400,'Clean','Clean',25000,fn_rental_charge(DATE_SUB(NOW(),INTERVAL 50 DAY),DATE_SUB(NOW(),INTERVAL 48 DAY),25000),'Completed'),
 (20,8,10,NULL,DATE_SUB(NOW(),INTERVAL 45 DAY),DATE_SUB(NOW(),INTERVAL 42 DAY),DATE_SUB(NOW(),INTERVAL 45 DAY),DATE_SUB(NOW(),INTERVAL 43 DAY),26000,26400,'Clean','Clean',11000,fn_rental_charge(DATE_SUB(NOW(),INTERVAL 45 DAY),DATE_SUB(NOW(),INTERVAL 43 DAY),11000),'Completed'),
 (21,9,11,NULL,DATE_SUB(NOW(),INTERVAL 40 DAY),DATE_SUB(NOW(),INTERVAL 37 DAY),DATE_SUB(NOW(),INTERVAL 40 DAY),DATE_SUB(NOW(),INTERVAL 38 DAY),35000,35400,'Clean','Clean',18000,fn_rental_charge(DATE_SUB(NOW(),INTERVAL 40 DAY),DATE_SUB(NOW(),INTERVAL 38 DAY),18000),'Completed'),
 (22,10,12,NULL,DATE_SUB(NOW(),INTERVAL 35 DAY),DATE_SUB(NOW(),INTERVAL 32 DAY),DATE_SUB(NOW(),INTERVAL 35 DAY),DATE_SUB(NOW(),INTERVAL 33 DAY),41000,41400,'Clean','Clean',16000,fn_rental_charge(DATE_SUB(NOW(),INTERVAL 35 DAY),DATE_SUB(NOW(),INTERVAL 33 DAY),16000),'Completed'),
 (23,11,13,NULL,DATE_SUB(NOW(),INTERVAL 30 DAY),DATE_SUB(NOW(),INTERVAL 27 DAY),DATE_SUB(NOW(),INTERVAL 30 DAY),DATE_SUB(NOW(),INTERVAL 28 DAY),12000,12400,'Clean','Clean',19000,fn_rental_charge(DATE_SUB(NOW(),INTERVAL 30 DAY),DATE_SUB(NOW(),INTERVAL 28 DAY),19000),'Completed'),
 (24,12,14,NULL,DATE_SUB(NOW(),INTERVAL 25 DAY),DATE_SUB(NOW(),INTERVAL 22 DAY),DATE_SUB(NOW(),INTERVAL 25 DAY),DATE_SUB(NOW(),INTERVAL 23 DAY),15000,15400,'Clean','Clean',20000,fn_rental_charge(DATE_SUB(NOW(),INTERVAL 25 DAY),DATE_SUB(NOW(),INTERVAL 23 DAY),20000),'Completed'),
 (25,13,15,NULL,DATE_SUB(NOW(),INTERVAL 20 DAY),DATE_SUB(NOW(),INTERVAL 17 DAY),DATE_SUB(NOW(),INTERVAL 20 DAY),DATE_SUB(NOW(),INTERVAL 18 DAY),27000,27400,'Clean','Clean',17000,fn_rental_charge(DATE_SUB(NOW(),INTERVAL 20 DAY),DATE_SUB(NOW(),INTERVAL 18 DAY),17000),'Completed'),
 (26,14,16,NULL,DATE_SUB(NOW(),INTERVAL 15 DAY),DATE_SUB(NOW(),INTERVAL 12 DAY),DATE_SUB(NOW(),INTERVAL 15 DAY),DATE_SUB(NOW(),INTERVAL 13 DAY),22000,22400,'Clean','Clean',15000,fn_rental_charge(DATE_SUB(NOW(),INTERVAL 15 DAY),DATE_SUB(NOW(),INTERVAL 13 DAY),15000),'Completed'),
 (27,15,17,NULL,DATE_SUB(NOW(),INTERVAL 50 DAY),DATE_SUB(NOW(),INTERVAL 47 DAY),DATE_SUB(NOW(),INTERVAL 50 DAY),DATE_SUB(NOW(),INTERVAL 48 DAY),19000,19400,'Clean','Clean',17000,fn_rental_charge(DATE_SUB(NOW(),INTERVAL 50 DAY),DATE_SUB(NOW(),INTERVAL 48 DAY),17000),'Completed'),
 (28,16,18,NULL,DATE_SUB(NOW(),INTERVAL 45 DAY),DATE_SUB(NOW(),INTERVAL 42 DAY),DATE_SUB(NOW(),INTERVAL 45 DAY),DATE_SUB(NOW(),INTERVAL 43 DAY),23000,23400,'Clean','Clean',18000,fn_rental_charge(DATE_SUB(NOW(),INTERVAL 45 DAY),DATE_SUB(NOW(),INTERVAL 43 DAY),18000),'Completed'),
 (29,15,23,NULL,DATE_ADD(NOW(),INTERVAL 2 DAY),DATE_ADD(NOW(),INTERVAL 4 DAY),NULL,NULL,NULL,NULL,NULL,NULL,15000,NULL,'Cancelled'),
 (30,16,24,NULL,DATE_ADD(NOW(),INTERVAL 8 DAY),DATE_ADD(NOW(),INTERVAL 10 DAY),NULL,NULL,NULL,NULL,NULL,NULL,14000,NULL,'Cancelled');

INSERT INTO lease (lease_id,customer_id,vehicle_id,start_date,end_date,monthly_charge,deposit,status) VALUES
 (1,17,17,DATE_SUB(CURDATE(),INTERVAL 5 DAY),DATE_ADD(CURDATE(),INTERVAL 20 DAY),330000,500000,'Active'),
 (2,18,18,DATE_SUB(CURDATE(),INTERVAL 2 DAY),DATE_ADD(CURDATE(),INTERVAL 180 DAY),350000,600000,'Active'),
 (3,19,19,DATE_ADD(CURDATE(),INTERVAL 90 DAY),DATE_ADD(CURDATE(),INTERVAL 455 DAY),210000,300000,'Scheduled'),
 (4,20,20,DATE_SUB(CURDATE(),INTERVAL 400 DAY),DATE_SUB(CURDATE(),INTERVAL 35 DAY),220000,300000,'Completed'),
 (5,21,23,DATE_SUB(CURDATE(),INTERVAL 300 DAY),DATE_SUB(CURDATE(),INTERVAL 210 DAY),300000,400000,'Completed'),
 (6,22,24,DATE_SUB(CURDATE(),INTERVAL 100 DAY),DATE_SUB(CURDATE(),INTERVAL 70 DAY),280000,350000,'Terminated');

-- These statuses are set by the seed because there are no lease or maintenance triggers.
UPDATE vehicle SET status = 'Leased' WHERE vehicle_id IN (17,18);

INSERT INTO maintenance
 (maintenance_id,vehicle_id,service_date,service_type,description,cost,next_service_date,completed_date,status) VALUES
 (1,21,DATE_SUB(CURDATE(),INTERVAL 2 DAY),'Brake inspection','Parts are being inspected',38000,DATE_ADD(CURDATE(),INTERVAL 25 DAY),NULL,'In Progress'),
 (2,1,DATE_SUB(CURDATE(),INTERVAL 300 DAY),'Oil service','Scheduled oil and filter service',22000,DATE_ADD(CURDATE(),INTERVAL 65 DAY),DATE_SUB(CURDATE(),INTERVAL 299 DAY),'Complete'),
 (3,2,DATE_SUB(CURDATE(),INTERVAL 270 DAY),'Tyre replacement','Premium tyre replacement',145000,DATE_ADD(CURDATE(),INTERVAL 95 DAY),DATE_SUB(CURDATE(),INTERVAL 269 DAY),'Complete'),
 (4,3,DATE_SUB(CURDATE(),INTERVAL 240 DAY),'Annual service','Annual inspection and service',52000,DATE_ADD(CURDATE(),INTERVAL 125 DAY),DATE_SUB(CURDATE(),INTERVAL 239 DAY),'Complete'),
 (5,4,DATE_SUB(CURDATE(),INTERVAL 210 DAY),'Detailing','Interior and exterior detailing',18000,DATE_ADD(CURDATE(),INTERVAL 155 DAY),DATE_SUB(CURDATE(),INTERVAL 209 DAY),'Complete'),
 (6,5,DATE_SUB(CURDATE(),INTERVAL 180 DAY),'Brake service','Brake pads and fluid service',44000,DATE_ADD(CURDATE(),INTERVAL 185 DAY),DATE_SUB(CURDATE(),INTERVAL 179 DAY),'Complete'),
 (7,6,DATE_SUB(CURDATE(),INTERVAL 150 DAY),'Battery service','Battery inspection and replacement',39000,DATE_ADD(CURDATE(),INTERVAL 215 DAY),DATE_SUB(CURDATE(),INTERVAL 149 DAY),'Complete'),
 (8,7,DATE_SUB(CURDATE(),INTERVAL 120 DAY),'Suspension check','Suspension inspection',27000,DATE_ADD(CURDATE(),INTERVAL 245 DAY),DATE_SUB(CURDATE(),INTERVAL 119 DAY),'Complete');
UPDATE vehicle SET status = 'Maintenance' WHERE vehicle_id = 21;

-- Successful payments are partial and leave an outstanding balance for the demo.
INSERT INTO payment (rental_id,lease_id,amount,method,status)
SELECT rental_id,NULL,ROUND(charges*0.50,2),'Card','Success'
FROM rental WHERE rental_id BETWEEN 17 AND 28;
INSERT INTO payment (rental_id,lease_id,amount,method,status)
SELECT rental_id,NULL,
 ROUND(fn_rental_charge(actual_pickup_datetime,GREATEST(expected_return_datetime,NOW()),daily_rate_applied)*0.25,2),
 'UPI','Success'
FROM rental WHERE rental_id BETWEEN 11 AND 16;
INSERT INTO payment (rental_id,lease_id,amount,method,status)
SELECT NULL,lease_id,ROUND(monthly_charge*0.50,2),'Bank Transfer','Success'
FROM lease WHERE lease_id BETWEEN 1 AND 6;
INSERT INTO payment (rental_id,lease_id,amount,method,status)
SELECT rental_id,NULL,ROUND(charges*0.10,2),'Cash','Pending'
FROM rental WHERE rental_id BETWEEN 17 AND 20;
INSERT INTO payment (rental_id,lease_id,amount,method,status)
SELECT NULL,lease_id,ROUND(monthly_charge*0.25,2),'Card','Pending'
FROM lease WHERE lease_id IN (1,2);

-- F12: each returned row identifies an inconsistency; expected result is zero rows.
SELECT issue,vehicle_id,ref_a,ref_b
FROM (
 SELECT 'ACTIVE_RENTAL_VEHICLE_STATUS' issue,r.vehicle_id,
        CONCAT('rental:',r.rental_id) ref_a,CONCAT('status:',v.status) ref_b
 FROM rental r JOIN vehicle v ON v.vehicle_id=r.vehicle_id
 WHERE r.status='Active' AND v.status<>'Rented'
 UNION ALL
 SELECT 'RENTED_VEHICLE_WITHOUT_ACTIVE_RENTAL',v.vehicle_id,
        CONCAT('vehicle:',v.vehicle_id),'no active rental'
 FROM vehicle v WHERE v.status='Rented'
 AND NOT EXISTS (SELECT 1 FROM rental r WHERE r.vehicle_id=v.vehicle_id AND r.status='Active')
 UNION ALL
 SELECT 'ACTIVE_LEASE_VEHICLE_STATUS',l.vehicle_id,
        CONCAT('lease:',l.lease_id),CONCAT('status:',v.status)
 FROM lease l JOIN vehicle v ON v.vehicle_id=l.vehicle_id
 WHERE l.status='Active' AND v.status<>'Leased'
 UNION ALL
 SELECT 'LEASED_VEHICLE_WITHOUT_ACTIVE_LEASE',v.vehicle_id,
        CONCAT('vehicle:',v.vehicle_id),'no active lease'
 FROM vehicle v WHERE v.status='Leased'
 AND NOT EXISTS (SELECT 1 FROM lease l WHERE l.vehicle_id=v.vehicle_id AND l.status='Active')
 UNION ALL
 SELECT 'IN_PROGRESS_MAINTENANCE_VEHICLE_STATUS',m.vehicle_id,
        CONCAT('maintenance:',m.maintenance_id),CONCAT('status:',v.status)
 FROM maintenance m JOIN vehicle v ON v.vehicle_id=m.vehicle_id
 WHERE m.status='In Progress' AND v.status<>'Maintenance'
 UNION ALL
 SELECT 'MAINTENANCE_VEHICLE_WITHOUT_IN_PROGRESS_RECORD',v.vehicle_id,
        CONCAT('vehicle:',v.vehicle_id),'no in-progress maintenance'
 FROM vehicle v WHERE v.status='Maintenance'
 AND NOT EXISTS (SELECT 1 FROM maintenance m WHERE m.vehicle_id=v.vehicle_id AND m.status='In Progress')
 UNION ALL
 SELECT 'OVERLAPPING_RENTALS',a.vehicle_id,CONCAT('rental:',a.rental_id),CONCAT('rental:',b.rental_id)
 FROM rental a JOIN rental b ON b.vehicle_id=a.vehicle_id AND b.rental_id>a.rental_id
 WHERE a.status IN ('Confirmed','Active') AND b.status IN ('Confirmed','Active')
 AND (
   (b.status='Confirmed' AND b.expected_return_datetime >
     CASE WHEN a.status='Active' THEN a.actual_pickup_datetime ELSE a.scheduled_start_datetime END)
   OR (b.status='Active' AND (b.expected_return_datetime >
     CASE WHEN a.status='Active' THEN a.actual_pickup_datetime ELSE a.scheduled_start_datetime END
     OR b.expected_return_datetime<=NOW()))
 )
 AND (
   (a.status='Confirmed' AND a.expected_return_datetime >
     CASE WHEN b.status='Active' THEN b.actual_pickup_datetime ELSE b.scheduled_start_datetime END)
   OR (a.status='Active' AND (a.expected_return_datetime >
     CASE WHEN b.status='Active' THEN b.actual_pickup_datetime ELSE b.scheduled_start_datetime END
     OR a.expected_return_datetime<=NOW()))
 )
 UNION ALL
 SELECT 'OVERLAPPING_LEASES',a.vehicle_id,CONCAT('lease:',a.lease_id),CONCAT('lease:',b.lease_id)
 FROM lease a JOIN lease b ON b.vehicle_id=a.vehicle_id AND b.lease_id>a.lease_id
 WHERE a.status IN ('Scheduled','Active') AND b.status IN ('Scheduled','Active')
 AND a.start_date<DATE_ADD(b.end_date,INTERVAL 1 DAY)
 AND b.start_date<DATE_ADD(a.end_date,INTERVAL 1 DAY)
 UNION ALL
 SELECT 'RENTAL_LEASE_OVERLAP',r.vehicle_id,CONCAT('rental:',r.rental_id),CONCAT('lease:',l.lease_id)
 FROM rental r JOIN lease l ON l.vehicle_id=r.vehicle_id
 WHERE r.status IN ('Confirmed','Active') AND l.status IN ('Scheduled','Active')
 AND (CASE WHEN r.status='Active' THEN r.actual_pickup_datetime ELSE r.scheduled_start_datetime END)
       < CAST(DATE_ADD(l.end_date,INTERVAL 1 DAY) AS DATETIME)
 AND (
   (r.status='Confirmed' AND r.expected_return_datetime>CAST(l.start_date AS DATETIME))
   OR (r.status='Active' AND (r.expected_return_datetime>CAST(l.start_date AS DATETIME)
     OR r.expected_return_datetime<=NOW()))
 )
 UNION ALL
 SELECT 'RENTAL_MAINTENANCE_OVERLAP',r.vehicle_id,CONCAT('rental:',r.rental_id),CONCAT('maintenance:',m.maintenance_id)
 FROM rental r JOIN maintenance m ON m.vehicle_id=r.vehicle_id AND m.status='In Progress'
 WHERE r.status IN ('Confirmed','Active')
 AND (
   (r.status='Confirmed' AND r.expected_return_datetime>CAST(m.service_date AS DATETIME))
   OR (r.status='Active' AND (r.expected_return_datetime>CAST(m.service_date AS DATETIME)
     OR r.expected_return_datetime<=NOW()))
 )
 UNION ALL
 SELECT 'LEASE_MAINTENANCE_OVERLAP',l.vehicle_id,CONCAT('lease:',l.lease_id),CONCAT('maintenance:',m.maintenance_id)
 FROM lease l JOIN maintenance m ON m.vehicle_id=l.vehicle_id AND m.status='In Progress'
 WHERE l.status IN ('Scheduled','Active') AND DATE_ADD(l.end_date,INTERVAL 1 DAY)>m.service_date
 UNION ALL
 SELECT 'MULTIPLE_IN_PROGRESS_MAINTENANCE',a.vehicle_id,
        CONCAT('maintenance:',a.maintenance_id),CONCAT('maintenance:',b.maintenance_id)
 FROM maintenance a JOIN maintenance b ON b.vehicle_id=a.vehicle_id AND b.maintenance_id>a.maintenance_id
 WHERE a.status='In Progress' AND b.status='In Progress'
 UNION ALL
 SELECT 'CORPORATE_RENTAL_WITHOUT_ACTIVE_CHAUFFEUR',r.vehicle_id,
        CONCAT('rental:',r.rental_id),
        CASE WHEN r.driver_id IS NULL THEN 'missing driver'
             ELSE CONCAT('driver:',r.driver_id,':',d.status) END
 FROM rental r
 JOIN customer c ON c.customer_id=r.customer_id
 LEFT JOIN driver d ON d.driver_id=r.driver_id
 WHERE c.customer_type='Corporate'
   AND (r.driver_id IS NULL OR d.status<>'Active')
 UNION ALL
 SELECT 'INDIVIDUAL_LICENSE_EXPIRES_BEFORE_RENTAL_END',r.vehicle_id,
        CONCAT('rental:',r.rental_id),CONCAT('license_expiry:',i.license_expiry)
 FROM rental r
 JOIN customer c ON c.customer_id=r.customer_id
 JOIN individual_customer i ON i.customer_id=c.customer_id
 WHERE r.status IN ('Confirmed','Active','Completed')
   AND i.license_expiry < DATE(
     CASE WHEN r.status='Completed' THEN r.actual_return_datetime
          ELSE r.expected_return_datetime END
   )
 UNION ALL
 SELECT 'INDIVIDUAL_LICENSE_EXPIRES_BEFORE_LEASE_END',l.vehicle_id,
        CONCAT('lease:',l.lease_id),CONCAT('license_expiry:',i.license_expiry)
 FROM lease l
 JOIN customer c ON c.customer_id=l.customer_id
 JOIN individual_customer i ON i.customer_id=c.customer_id
 WHERE l.status IN ('Scheduled','Active','Completed')
   AND i.license_expiry < l.end_date
 UNION ALL
 SELECT 'CUSTOMER_SUBTYPE_MISMATCH',NULL,CONCAT('customer:',c.customer_id),c.customer_type
 FROM customer c
 LEFT JOIN individual_customer i ON i.customer_id=c.customer_id
 LEFT JOIN corporate_customer cc ON cc.customer_id=c.customer_id
 WHERE (c.customer_type='Individual' AND (i.customer_id IS NULL OR cc.customer_id IS NOT NULL))
    OR (c.customer_type='Corporate' AND (cc.customer_id IS NULL OR i.customer_id IS NOT NULL))
) consistency_issues
ORDER BY issue,vehicle_id,ref_a;
