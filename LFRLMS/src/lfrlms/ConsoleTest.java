package lfrlms;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;
import lfrlms.dao.CustomerDao;
import lfrlms.dao.LeaseDao;
import lfrlms.dao.MaintenanceDao;
import lfrlms.dao.PaymentDao;
import lfrlms.dao.RentalDao;
import lfrlms.dao.ReportDao;
import lfrlms.dao.VehicleDao;
import lfrlms.exception.IneligibleCustomerException;
import lfrlms.exception.LfrlmsException;
import lfrlms.exception.ValidationException;
import lfrlms.exception.VehicleNotAvailableException;
import lfrlms.model.CorporateCustomer;
import lfrlms.model.IndividualCustomer;
import lfrlms.model.Lease;
import lfrlms.model.Maintenance;
import lfrlms.model.Payment;
import lfrlms.model.Rental;
import lfrlms.model.Vehicle;
import lfrlms.service.AvailabilityService;
import lfrlms.service.BillingService;
import lfrlms.service.CustomerService;
import lfrlms.service.PaymentService;
import lfrlms.service.RentalService;
import lfrlms.util.Constants;

/** Runs repeatable command-line checks for the Phase 2 services. */
public final class ConsoleTest {
    private static final int PAGE_SIZE = 100;
    private final AvailabilityService availabilityService = new AvailabilityService();
    private final BillingService billingService = new BillingService();
    private final CustomerService customerService = new CustomerService();
    private final RentalService rentalService = new RentalService();
    private final PaymentService paymentService = new PaymentService();
    private final CustomerDao customerDao = new CustomerDao();
    private final LeaseDao leaseDao = new LeaseDao();
    private final MaintenanceDao maintenanceDao = new MaintenanceDao();
    private final RentalDao rentalDao = new RentalDao();
    private final VehicleDao vehicleDao = new VehicleDao();
    private final PaymentDao paymentDao = new PaymentDao();
    private final ReportDao reportDao = new ReportDao();
    private IndividualCustomer fixtureCustomer;
    private int failures;

    private ConsoleTest() {
    }

    public static void main(String[] arguments) {
        new ConsoleTest().runAll();
    }

    private void runAll() {
        String[] parityWindows = new String[] {
            "past-adjacent",
            "current",
            "overlapping Active rental",
            "overlapping Confirmed rental",
            "overlapping in-progress maintenance",
            "far future"
        };
        for (final String windowName : parityWindows) {
            run("availability parity: " + windowName, new TestAction() {
                public void run() throws Exception {
                    compareAvailabilitySources(createAvailabilityWindow(windowName));
                }
            });
        }

        run("availability search", new TestAction() {
            public void run() throws Exception {
                LocalDateTime from = LocalDateTime.now().plusDays(20);
                ArrayList<HashMap<String, Object>> rows = availabilityService
                        .searchAvailableVehicles(from, from.plusDays(2), null, null);
                require(!rows.isEmpty(), "no available vehicle was returned");
            }
        });

        try {
            fixtureCustomer = newIndividual(LocalDate.now().plusYears(5));
            customerService.save(fixtureCustomer);
        } catch (Exception exception) {
            failures++;
            System.out.println("FAIL: service test customer setup ("
                    + exception.getClass().getSimpleName() + ")");
            printUnavailableDependentResults();
            return;
        }

        try {
            run("creating a Confirmed rental", new TestAction() {
                public void run() throws Exception {
                    int rentalId = 0;
                    try {
                        Rental rental = createConfirmedForFixture();
                        rentalId = rental.getRentalId();
                        require(Constants.RENTAL_CONFIRMED.equals(
                                rentalDao.findById(rentalId).getStatus()),
                                "the inserted rental was not Confirmed");
                    } finally {
                        deleteRental(rentalId);
                    }
                }
            });

            run("overlapping rental is rejected", new TestAction() {
                public void run() throws Exception {
                    int firstId = 0;
                    int unexpectedId = 0;
                    try {
                        Rental first = createConfirmedForFixture();
                        firstId = first.getRentalId();
                        Rental overlap = confirmed(first.getVehicleId(),
                                first.getScheduledStartDatetime().plusHours(1),
                                first.getExpectedReturnDatetime().plusHours(1));
                        try {
                            unexpectedId = rentalService.createRental(overlap);
                            throw new AssertionError("the overlapping rental was accepted");
                        } catch (VehicleNotAvailableException expected) {
                            // Expected service rejection.
                        }
                    } finally {
                        deleteRental(unexpectedId);
                        deleteRental(firstId);
                    }
                }
            });

            run("overdue Active rental blocks its vehicle", new TestAction() {
                public void run() throws Exception {
                    Rental overdue = findOverdueActiveRental();
                    require(overdue != null, "the seed has no overdue Active rental");
                    LocalDateTime from = LocalDateTime.now();
                    boolean available = availabilityService.isVehicleAvailable(
                            overdue.getVehicleId(), from, from.plusHours(1), null, null);
                    require(!available, "the overdue rental's vehicle was reported available");
                }
            });

            run("starting a rental commits and trigger sets Rented", new TestAction() {
                public void run() throws Exception {
                    int rentalId = 0;
                    try {
                        Rental rental = createNearTermConfirmed();
                        rentalId = rental.getRentalId();
                        LocalDateTime pickup = LocalDateTime.now();
                        rentalService.startRental(rentalId, pickup, 1000, "Clean");
                        Rental saved = rentalDao.findById(rentalId);
                        require(Constants.RENTAL_ACTIVE.equals(saved.getStatus()),
                                "the rental status did not commit as Active");
                        require(Constants.VEHICLE_RENTED.equals(
                                vehicleDao.findById(saved.getVehicleId()).getStatus()),
                                "the trigger did not set the vehicle to Rented");
                        rentalService.completeRental(rentalId, pickup.plusHours(1),
                                1001, "Clean");
                    } finally {
                        deleteRental(rentalId);
                    }
                }
            });

            run("invalid advance payment rolls back with no rental row left behind",
                    new TestAction() {
                public void run() throws Exception {
                    long countBefore = rentalDao.countAll();
                    LocalDateTime start = LocalDateTime.now().plusDays(3);
                    int vehicleId = chooseAvailableVehicle(start, start.plusDays(1));
                    Rental rental = confirmed(vehicleId, start, start.plusDays(1));
                    Payment invalid = new Payment();
                    invalid.setAmount(BigDecimal.ZERO);
                    invalid.setMethod(Constants.PAYMENT_CASH);
                    invalid.setStatus(Constants.PAYMENT_PENDING);
                    try {
                        rentalService.createRentalWithAdvancePayment(rental, invalid);
                        throw new AssertionError("the invalid advance payment was accepted");
                    } catch (ValidationException expected) {
                        // The rental insert happened first and must have rolled back.
                    }
                    long countAfter = rentalDao.countAll();
                    require(countBefore == countAfter,
                            "the rental table count changed after rollback");
                    require(!hasRentalForCustomer(fixtureCustomer.getCustomerId()),
                            "a rental row remains for the test customer");
                }
            });

            run("sp_complete_rental completes and frees the vehicle", new TestAction() {
                public void run() throws Exception {
                    int rentalId = 0;
                    try {
                        Rental rental = createNearTermConfirmed();
                        rentalId = rental.getRentalId();
                        LocalDateTime pickup = LocalDateTime.now();
                        rentalService.startRental(rentalId, pickup, 1200, "Clean");
                        rentalService.completeRental(rentalId, pickup.plusHours(2),
                                1205, "Returned clean");
                        Rental saved = rentalDao.findById(rentalId);
                        require(Constants.RENTAL_COMPLETED.equals(saved.getStatus()),
                                "the procedure did not complete the rental");
                        require(Constants.VEHICLE_AVAILABLE.equals(
                                vehicleDao.findById(saved.getVehicleId()).getStatus()),
                                "the trigger did not free the vehicle");
                    } finally {
                        deleteRental(rentalId);
                    }
                }
            });

            run("payment with both references is rejected", new TestAction() {
                public void run() throws Exception {
                    Payment invalid = new Payment();
                    invalid.setRentalId(Integer.valueOf(99999999));
                    invalid.setLeaseId(Integer.valueOf(99999999));
                    invalid.setAmount(new BigDecimal("1.00"));
                    invalid.setMethod(Constants.PAYMENT_CASH);
                    invalid.setStatus(Constants.PAYMENT_PENDING);
                    try {
                        paymentService.recordPayment(invalid);
                        throw new AssertionError("payment with two references was accepted");
                    } catch (ValidationException expected) {
                        // Rejected before any database write.
                    }
                }
            });

            run("individual licence expiring before rental end is rejected", new TestAction() {
                public void run() throws Exception {
                    IndividualCustomer expiring = newIndividual(LocalDate.now().plusDays(1));
                    int customerId = customerService.save(expiring);
                    try {
                        LocalDateTime start = LocalDateTime.now().plusDays(2);
                        int vehicleId = chooseAvailableVehicle(start, start.plusDays(3));
                        Rental rental = confirmed(vehicleId, start, start.plusDays(3));
                        rental.setCustomerId(customerId);
                        try {
                            rentalService.createRental(rental);
                            throw new AssertionError("the expired licence was accepted");
                        } catch (IneligibleCustomerException expected) {
                            // Rejected before the rental insert.
                        }
                    } finally {
                        customerService.delete(customerId);
                    }
                }
            });

            run("corporate rental without a chauffeur is rejected", new TestAction() {
                public void run() throws Exception {
                    CorporateCustomer corporate = newCorporate();
                    int customerId = customerService.save(corporate);
                    try {
                        LocalDateTime start = LocalDateTime.now().plusDays(4);
                        int vehicleId = chooseAvailableVehicle(start, start.plusDays(1));
                        Rental rental = confirmed(vehicleId, start, start.plusDays(1));
                        rental.setCustomerId(customerId);
                        try {
                            rentalService.createRental(rental);
                            throw new AssertionError("the chauffeur-free corporate rental was accepted");
                        } catch (IneligibleCustomerException expected) {
                            // Rejected before the rental insert.
                        }
                    } finally {
                        customerService.delete(customerId);
                    }
                }
            });

            run("failed subtype insert rolls back with no customer parent left behind",
                    new TestAction() {
                public void run() throws Exception {
                    long countBefore = customerDao.countAll();
                    IndividualCustomer duplicateLicense = newIndividual(
                            LocalDate.now().plusYears(4));
                    duplicateLicense.setLicenseNo(fixtureCustomer.getLicenseNo());
                    boolean failed = false;
                    try {
                        customerService.save(duplicateLicense);
                    } catch (LfrlmsException expected) {
                        failed = true;
                    }
                    require(failed, "the duplicate subtype insert unexpectedly succeeded");
                    require(customerDao.countAll() == countBefore,
                            "the customer count changed after rollback");
                    require(!customerExistsByEmail(duplicateLicense.getEmail()),
                            "the customer parent row remains after subtype rollback");
                }
            });

            run("Java charge estimate matches fn_rental_charge", new TestAction() {
                public void run() throws Exception {
                    LocalDateTime start = LocalDateTime.of(2026, 1, 10, 8, 0);
                    int[] durations = new int[] {1, 23, 25, 48, 49};
                    BigDecimal rate = new BigDecimal("1250.00");
                    for (int duration : durations) {
                        LocalDateTime end = start.plusHours(duration);
                        BigDecimal javaEstimate = billingService.estimateRental(start, end, rate);
                        BigDecimal databaseEstimate = reportDao.rentalCharge(start, end, rate);
                        require(javaEstimate.compareTo(databaseEstimate) == 0,
                                "Java and SQL differ for " + duration + " hours");
                    }
                }
            });

            run("LocalDateTime round-trip preserves wall time", new TestAction() {
                public void run() throws Exception {
                    int rentalId = 0;
                    try {
                        LocalDateTime start = LocalDateTime.now().plusDays(20).withNano(0);
                        LocalDateTime end = start.plusDays(2);
                        int vehicleId = chooseAvailableVehicle(start, end);
                        Rental rental = confirmed(vehicleId, start, end);
                        rental.setCustomerId(fixtureCustomer.getCustomerId());
                        rentalId = rentalService.createRental(rental);
                        Rental saved = rentalDao.findById(rentalId);
                        require(saved != null && start.equals(saved.getScheduledStartDatetime()),
                                "the scheduled LocalDateTime changed after database round-trip");
                    } finally {
                        deleteRental(rentalId);
                    }
                }
            });
        } finally {
            try {
                if (fixtureCustomer != null && fixtureCustomer.getCustomerId() > 0) {
                    customerService.delete(fixtureCustomer.getCustomerId());
                }
            } catch (Exception exception) {
                failures++;
                System.out.println("FAIL: cleanup of shared test customer ("
                        + exception.getClass().getSimpleName() + ")");
            }
        }
        if (failures > 0) {
            System.exit(1);
        }
    }

    private Rental createConfirmedForFixture() throws Exception {
        LocalDateTime start = LocalDateTime.now().plusDays(20);
        int vehicleId = chooseAvailableVehicle(start, start.plusDays(2));
        Rental rental = confirmed(vehicleId, start, start.plusDays(2));
        rental.setCustomerId(fixtureCustomer.getCustomerId());
        int rentalId = rentalService.createRental(rental);
        rental.setRentalId(rentalId);
        return rental;
    }

    private AvailabilityWindow createAvailabilityWindow(String name) throws Exception {
        LocalDateTime now = LocalDateTime.now();
        if ("past-adjacent".equals(name)) {
            for (Rental rental : allRentals()) {
                if (Constants.RENTAL_COMPLETED.equals(rental.getStatus())
                        && rental.getActualReturnDatetime() != null) {
                    LocalDateTime from = rental.getActualReturnDatetime();
                    return new AvailabilityWindow(from, from.plusHours(1));
                }
            }
        } else if ("current".equals(name)) {
            return new AvailabilityWindow(now, now.plusHours(2));
        } else if ("overlapping Active rental".equals(name)) {
            for (Rental rental : allRentals()) {
                if (Constants.RENTAL_ACTIVE.equals(rental.getStatus())
                        && rental.getActualPickupDatetime() != null
                        && rental.getActualPickupDatetime().isBefore(now.plusMinutes(15))
                        && rental.getExpectedReturnDatetime().isAfter(now.plusMinutes(20))) {
                    return new AvailabilityWindow(now.plusMinutes(5), now.plusMinutes(15));
                }
            }
        } else if ("overlapping Confirmed rental".equals(name)) {
            for (Rental rental : allRentals()) {
                if (Constants.RENTAL_CONFIRMED.equals(rental.getStatus())
                        && rental.getScheduledStartDatetime().isAfter(now)) {
                    LocalDateTime from = rental.getScheduledStartDatetime();
                    return new AvailabilityWindow(from, from.plusHours(1));
                }
            }
        } else if ("overlapping in-progress maintenance".equals(name)) {
            ArrayList<Maintenance> records = maintenanceDao.search(null,
                    Constants.MAINTENANCE_IN_PROGRESS, null, null,
                    "serviceDate", true, 1, PAGE_SIZE);
            if (!records.isEmpty()) {
                LocalDateTime from = records.get(0).getServiceDate().atStartOfDay();
                return new AvailabilityWindow(from, from.plusHours(1));
            }
        } else if ("far future".equals(name)) {
            LocalDateTime from = now.plusYears(3);
            return new AvailabilityWindow(from, from.plusDays(1));
        }
        throw new AssertionError("No seeded record could build the '" + name + "' window.");
    }

    private void compareAvailabilitySources(AvailabilityWindow window) throws Exception {
        HashSet<Integer> serviceSearch = idsFromRows(availabilityService
                .searchAvailableVehicles(window.from, window.to, null, null));
        HashSet<Integer> storedProcedure = idsFromRows(reportDao
                .availableVehicles(window.from, window.to, null, null));
        HashSet<Integer> daoSearch = idsFromVehicles(vehicleDao.search(null, null, null,
                null, null, null, null, null, null, window.from, window.to,
                "registrationNo", true, 1, PAGE_SIZE));
        ArrayList<Vehicle> vehicles = allVehicles();
        HashSet<Integer> javaRecheck = new HashSet<Integer>();
        for (Vehicle vehicle : vehicles) {
            if (availabilityService.isVehicleAvailable(vehicle.getVehicleId(),
                    window.from, window.to, null, null)) {
                javaRecheck.add(Integer.valueOf(vehicle.getVehicleId()));
            }
        }
        if (serviceSearch.equals(storedProcedure) && serviceSearch.equals(daoSearch)
                && serviceSearch.equals(javaRecheck)) {
            return;
        }
        throw new AssertionError(availabilityMismatch(window, vehicles, serviceSearch,
                daoSearch, storedProcedure, javaRecheck));
    }

    private HashSet<Integer> idsFromRows(ArrayList<HashMap<String, Object>> rows) {
        HashSet<Integer> ids = new HashSet<Integer>();
        for (HashMap<String, Object> row : rows) {
            Object value = row.get("vehicle_id");
            if (value instanceof Number) {
                ids.add(Integer.valueOf(((Number) value).intValue()));
            }
        }
        return ids;
    }

    private HashSet<Integer> idsFromVehicles(ArrayList<Vehicle> vehicles) {
        HashSet<Integer> ids = new HashSet<Integer>();
        for (Vehicle vehicle : vehicles) {
            ids.add(Integer.valueOf(vehicle.getVehicleId()));
        }
        return ids;
    }

    private String availabilityMismatch(AvailabilityWindow window,
            ArrayList<Vehicle> vehicles, Set<Integer> serviceSearch,
            Set<Integer> daoSearch, Set<Integer> storedProcedure,
            Set<Integer> javaRecheck) throws Exception {
        ArrayList<Rental> rentals = allRentals();
        ArrayList<Lease> leases = allLeases();
        ArrayList<Maintenance> maintenance = allMaintenance();
        HashSet<Integer> expected = new HashSet<Integer>();
        for (Vehicle vehicle : vehicles) {
            if (availabilityBlockers(vehicle, window, rentals, leases, maintenance).isEmpty()) {
                expected.add(Integer.valueOf(vehicle.getVehicleId()));
            }
        }
        StringBuilder message = new StringBuilder();
        message.append("Expected-by-rules=").append(sortedIds(expected))
                .append("; service=").append(sortedIds(serviceSearch))
                .append("; VehicleDao=").append(sortedIds(daoSearch))
                .append("; procedure=").append(sortedIds(storedProcedure))
                .append("; Java-recheck=").append(sortedIds(javaRecheck));
        for (Vehicle vehicle : vehicles) {
            Integer id = Integer.valueOf(vehicle.getVehicleId());
            boolean expectedAvailable = expected.contains(id);
            if (serviceSearch.contains(id) != expectedAvailable
                    || daoSearch.contains(id) != expectedAvailable
                    || storedProcedure.contains(id) != expectedAvailable
                    || javaRecheck.contains(id) != expectedAvailable) {
                message.append("\nvehicle ").append(id)
                        .append(" status=").append(vehicle.getStatus())
                        .append(" expected=").append(expectedAvailable ? "available" : "blocked")
                        .append(" blockers=")
                        .append(availabilityBlockers(vehicle, window, rentals, leases, maintenance))
                        .append(" sources[service=").append(serviceSearch.contains(id))
                        .append(", VehicleDao=").append(daoSearch.contains(id))
                        .append(", procedure=").append(storedProcedure.contains(id))
                        .append(", Java=").append(javaRecheck.contains(id)).append(']');
            }
        }
        message.append("\nThe blocker list identifies the violated rule; an ID shown by a source despite a blocker ")
                .append("means that source missed that rule, while an omitted ID without a blocker ")
                .append("means that source applied an extra restriction.");
        return message.toString();
    }

    private ArrayList<String> availabilityBlockers(Vehicle vehicle,
            AvailabilityWindow window, ArrayList<Rental> rentals, ArrayList<Lease> leases,
            ArrayList<Maintenance> maintenance) {
        ArrayList<String> blockers = new ArrayList<String>();
        int vehicleId = vehicle.getVehicleId();
        LocalDateTime now = LocalDateTime.now();
        if (Constants.VEHICLE_MAINTENANCE.equals(vehicle.getStatus())
                || Constants.VEHICLE_INACTIVE.equals(vehicle.getStatus())) {
            blockers.add("vehicle status " + vehicle.getStatus());
        }
        for (Rental rental : rentals) {
            if (rental.getVehicleId() != vehicleId) {
                continue;
            }
            if (Constants.RENTAL_CONFIRMED.equals(rental.getStatus())
                    && rental.getScheduledStartDatetime().isBefore(window.to)
                    && rental.getExpectedReturnDatetime().isAfter(window.from)) {
                blockers.add("Confirmed rental #" + rental.getRentalId());
            } else if (Constants.RENTAL_ACTIVE.equals(rental.getStatus())
                    && rental.getActualPickupDatetime() != null
                    && rental.getActualPickupDatetime().isBefore(window.to)
                    && (rental.getExpectedReturnDatetime().isAfter(window.from)
                    || !rental.getExpectedReturnDatetime().isAfter(now))) {
                blockers.add("Active rental #" + rental.getRentalId()
                        + (rental.getExpectedReturnDatetime().isAfter(now)
                        ? "" : " (overdue/open-ended)"));
            }
        }
        addLeaseBlockers(blockers, vehicleId, window, leases);
        addMaintenanceBlockers(blockers, vehicleId, window, maintenance);
        return blockers;
    }

    private void addLeaseBlockers(ArrayList<String> blockers, int vehicleId,
            AvailabilityWindow window, ArrayList<Lease> leases) {
        for (Lease lease : leases) {
            if (lease.getVehicleId() == vehicleId
                    && (Constants.LEASE_SCHEDULED.equals(lease.getStatus())
                    || Constants.LEASE_ACTIVE.equals(lease.getStatus()))) {
                LocalDateTime start = lease.getStartDate().atStartOfDay();
                LocalDateTime end = lease.getEndDate().plusDays(1).atStartOfDay();
                if (start.isBefore(window.to) && end.isAfter(window.from)) {
                    blockers.add(lease.getStatus() + " lease #" + lease.getLeaseId());
                }
            }
        }
    }

    private void addMaintenanceBlockers(ArrayList<String> blockers, int vehicleId,
            AvailabilityWindow window, ArrayList<Maintenance> records) {
        for (Maintenance record : records) {
            if (record.getVehicleId() == vehicleId
                    && Constants.MAINTENANCE_IN_PROGRESS.equals(record.getStatus())
                    && record.getServiceDate().atStartOfDay().isBefore(window.to)) {
                blockers.add("in-progress maintenance #" + record.getMaintenanceId());
            }
        }
    }

    private String sortedIds(Set<Integer> ids) {
        return new TreeSet<Integer>(ids).toString();
    }

    private ArrayList<Vehicle> allVehicles() throws LfrlmsException {
        ArrayList<Vehicle> rows = new ArrayList<Vehicle>();
        int pageNumber = 1;
        while (true) {
            ArrayList<Vehicle> page = vehicleDao.search(null, null, null, null,
                    null, null, null, null, null, null, null,
                    "registrationNo", true, pageNumber, PAGE_SIZE);
            rows.addAll(page);
            if (page.size() < PAGE_SIZE) {
                return rows;
            }
            pageNumber++;
        }
    }

    private ArrayList<Rental> allRentals() throws LfrlmsException {
        ArrayList<Rental> rows = new ArrayList<Rental>();
        int pageNumber = 1;
        while (true) {
            ArrayList<Rental> page = rentalDao.search(null, null, null, null, false,
                    "scheduledStart", true, pageNumber, PAGE_SIZE);
            rows.addAll(page);
            if (page.size() < PAGE_SIZE) {
                return rows;
            }
            pageNumber++;
        }
    }

    private ArrayList<Lease> allLeases() throws LfrlmsException {
        ArrayList<Lease> rows = new ArrayList<Lease>();
        int pageNumber = 1;
        while (true) {
            ArrayList<Lease> page = leaseDao.search(null, null, null, null, false,
                    "startDate", true, pageNumber, PAGE_SIZE);
            rows.addAll(page);
            if (page.size() < PAGE_SIZE) {
                return rows;
            }
            pageNumber++;
        }
    }

    private ArrayList<Maintenance> allMaintenance() throws LfrlmsException {
        ArrayList<Maintenance> rows = new ArrayList<Maintenance>();
        int pageNumber = 1;
        while (true) {
            ArrayList<Maintenance> page = maintenanceDao.search(null, null, null, null,
                    "serviceDate", true, pageNumber, PAGE_SIZE);
            rows.addAll(page);
            if (page.size() < PAGE_SIZE) {
                return rows;
            }
            pageNumber++;
        }
    }

    private Rental createNearTermConfirmed() throws Exception {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime start = now.plusMinutes(2);
        LocalDateTime end = now.plusHours(12);
        int vehicleId = chooseAvailableVehicle(now, end);
        Rental rental = confirmed(vehicleId, start, end);
        rental.setCustomerId(fixtureCustomer.getCustomerId());
        int rentalId = rentalService.createRental(rental);
        rental.setRentalId(rentalId);
        return rental;
    }

    private int chooseAvailableVehicle(LocalDateTime from, LocalDateTime to)
            throws Exception {
        LocalDateTime candidateFrom = from;
        LocalDateTime candidateTo = to;
        for (int attempt = 0; attempt < 8; attempt++) {
            ArrayList<HashMap<String, Object>> vehicles =
                    availabilityService.searchAvailableVehicles(
                    candidateFrom, candidateTo, null, null);
            for (HashMap<String, Object> row : vehicles) {
                if (row.get("rental_rate") != null) {
                    Object id = row.get("vehicle_id");
                    return ((Number) id).intValue();
                }
            }
            candidateFrom = candidateFrom.plusDays(30);
            candidateTo = candidateTo.plusDays(30);
        }
        throw new AssertionError("no available rental vehicle was returned");
    }

    private Rental confirmed(int vehicleId, LocalDateTime start, LocalDateTime end) {
        return new Rental(0, fixtureCustomer == null ? 0 : fixtureCustomer.getCustomerId(),
                vehicleId, null, start, end, null, null, null, null, null, null,
                null, null, Constants.RENTAL_CONFIRMED);
    }

    private Rental findOverdueActiveRental() throws Exception {
        int pageNumber = 1;
        while (true) {
            ArrayList<Rental> rows = rentalDao.search(null, Constants.RENTAL_ACTIVE,
                    null, null, false, "scheduledStart", true, pageNumber, 100);
            for (Rental rental : rows) {
                if (rental.getExpectedReturnDatetime().isBefore(LocalDateTime.now())) {
                    return rental;
                }
            }
            if (rows.size() < 100) {
                return null;
            }
            pageNumber++;
        }
    }

    private boolean hasRentalForCustomer(int customerId) throws Exception {
        int pageNumber = 1;
        while (true) {
            ArrayList<Rental> rows = rentalDao.search(null, null, null, null, false,
                    "scheduledStart", true, pageNumber, 100);
            for (Rental rental : rows) {
                if (rental.getCustomerId() == customerId) {
                    return true;
                }
            }
            if (rows.size() < 100) {
                return false;
            }
            pageNumber++;
        }
    }

    private boolean customerExistsByEmail(String email) throws Exception {
        return !customerDao.search(email, null, null, "email", true, 1, 5).isEmpty();
    }

    private IndividualCustomer newIndividual(LocalDate expiry) {
        String token = Long.toHexString(System.nanoTime()).toUpperCase();
        String phone = Long.toString(1000000000L + (System.nanoTime() % 8999999999L));
        return new IndividualCustomer(0, "Phase", "Tester", "phase2b." + token
                + "@example.test", phone, "Test Street", "Nashik", "422001",
                LocalDate.now().minusYears(25), "TEST/" + token, expiry);
    }

    private CorporateCustomer newCorporate() {
        long suffix = Math.abs(System.nanoTime() % 10000L);
        String token = Long.toHexString(System.nanoTime()).toUpperCase();
        String phone = Long.toString(1000000000L + (System.nanoTime() % 8999999999L));
        String gst = "27ABCDE" + String.format("%04d", Long.valueOf(suffix)) + "F1Z5";
        return new CorporateCustomer(0, "Phase", "Corporate", "company." + token
                + "@example.test", phone, "Test Street", "Nashik", "422001",
                "Phase Two Mobility " + token, gst);
    }

    private void deleteRental(int rentalId) throws LfrlmsException {
        if (rentalId > 0) {
            rentalDao.delete(rentalId);
        }
    }

    private void run(String name, TestAction action) {
        try {
            action.run();
            System.out.println("PASS: " + name);
        } catch (Throwable failure) {
            failures++;
            System.out.println("FAIL: " + name + " ("
                    + failure.getClass().getSimpleName() + ")");
            if (failure.getMessage() != null && !failure.getMessage().trim().isEmpty()) {
                System.out.println(failure.getMessage());
            }
        }
    }

    private void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private void printUnavailableDependentResults() {
        String[] names = new String[] {
            "creating a Confirmed rental",
            "overlapping rental is rejected",
            "overdue Active rental blocks its vehicle",
            "starting a rental commits and trigger sets Rented",
            "invalid advance payment rolls back with no rental row left behind",
            "sp_complete_rental completes and frees the vehicle",
            "payment with both references is rejected",
            "individual licence expiring before rental end is rejected",
            "corporate rental without a chauffeur is rejected",
            "failed subtype insert rolls back with no customer parent left behind",
            "Java charge estimate matches fn_rental_charge",
            "LocalDateTime round-trip preserves wall time"
        };
        for (String name : names) {
            failures++;
            System.out.println("FAIL: " + name + " (test customer setup failed)");
        }
        System.exit(1);
    }

    private interface TestAction {
        void run() throws Exception;
    }

    private static final class AvailabilityWindow {
        private final LocalDateTime from;
        private final LocalDateTime to;

        private AvailabilityWindow(LocalDateTime from, LocalDateTime to) {
            this.from = from;
            this.to = to;
        }
    }
}
