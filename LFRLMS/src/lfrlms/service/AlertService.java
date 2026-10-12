package lfrlms.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import lfrlms.dao.CustomerDao;
import lfrlms.dao.LeaseDao;
import lfrlms.dao.MaintenanceDao;
import lfrlms.dao.RentalDao;
import lfrlms.exception.LfrlmsException;
import lfrlms.model.Customer;
import lfrlms.model.Lease;
import lfrlms.model.Maintenance;
import lfrlms.model.Rental;
import lfrlms.util.Constants;

/** Builds the counts used by the dashboard's periodic alert monitor. */
public class AlertService {
    private static final int PAGE_SIZE = 100;
    private final RentalDao rentalDao;
    private final LeaseDao leaseDao;
    private final MaintenanceDao maintenanceDao;
    private final CustomerDao customerDao;

    public AlertService() {
        this(new RentalDao(), new LeaseDao(), new MaintenanceDao(), new CustomerDao());
    }

    public AlertService(RentalDao rentalDao, LeaseDao leaseDao,
            MaintenanceDao maintenanceDao, CustomerDao customerDao) {
        this.rentalDao = rentalDao;
        this.leaseDao = leaseDao;
        this.maintenanceDao = maintenanceDao;
        this.customerDao = customerDao;
    }

    public HashMap<String, Integer> getAlertCounts() throws LfrlmsException {
        LocalDateTime now = LocalDateTime.now();
        LocalDate today = LocalDate.now();
        HashMap<String, Integer> counts = new HashMap<String, Integer>();
        counts.put("overdueRentals", Integer.valueOf(countOverdueRentals(now)));
        counts.put("upcomingRentals", Integer.valueOf(countUpcomingRentals(now)));
        counts.put("leasesEnding", Integer.valueOf(countLeasesEnding(today)));
        counts.put("maintenanceDue", Integer.valueOf(countMaintenanceDue(today)));
        counts.put("licensesExpiring", Integer.valueOf(countLicensesExpiring()));
        return counts;
    }

    private int countOverdueRentals(LocalDateTime now) throws LfrlmsException {
        int count = 0;
        HashSet<Integer> seen = new HashSet<Integer>();
        int pageNumber = 1;
        while (true) {
            ArrayList<Rental> rows = rentalDao.search(null, Constants.RENTAL_ACTIVE,
                    null, now, false, "scheduledStart", true, pageNumber, PAGE_SIZE);
            for (Rental rental : rows) {
                if (seen.add(Integer.valueOf(rental.getRentalId()))
                        && rental.getExpectedReturnDatetime().isBefore(now)) {
                    count++;
                }
            }
            if (rows.size() < PAGE_SIZE) {
                return count;
            }
            pageNumber++;
        }
    }

    private int countUpcomingRentals(LocalDateTime now) throws LfrlmsException {
        int count = 0;
        HashSet<Integer> seen = new HashSet<Integer>();
        int pageNumber = 1;
        LocalDateTime through = now.plusHours(24);
        while (true) {
            ArrayList<Rental> rows = rentalDao.search(null, Constants.RENTAL_CONFIRMED,
                    null, through, false, "scheduledStart", true, pageNumber, PAGE_SIZE);
            for (Rental rental : rows) {
                if (seen.add(Integer.valueOf(rental.getRentalId()))) {
                    count++;
                }
            }
            if (rows.size() < PAGE_SIZE) {
                return count;
            }
            pageNumber++;
        }
    }

    private int countLeasesEnding(LocalDate today) throws LfrlmsException {
        int count = 0;
        HashSet<Integer> seen = new HashSet<Integer>();
        int pageNumber = 1;
        LocalDate through = today.plusDays(30);
        while (true) {
            ArrayList<Lease> rows = leaseDao.search(null, Constants.LEASE_ACTIVE,
                    null, null, false, "endDate", true, pageNumber, PAGE_SIZE);
            for (Lease lease : rows) {
                if (seen.add(Integer.valueOf(lease.getLeaseId()))
                        && !lease.getEndDate().isBefore(today)
                        && !lease.getEndDate().isAfter(through)) {
                    count++;
                }
            }
            if (rows.size() < PAGE_SIZE) {
                return count;
            }
            pageNumber++;
        }
    }

    private int countMaintenanceDue(LocalDate today) throws LfrlmsException {
        int count = 0;
        int pageNumber = 1;
        LocalDate through = today.plusDays(7);
        while (true) {
            ArrayList<Maintenance> rows = maintenanceDao.search(null,
                    Constants.MAINTENANCE_IN_PROGRESS, null, null,
                    "serviceDate", true, pageNumber, PAGE_SIZE);
            for (Maintenance maintenance : rows) {
                LocalDate nextDate = maintenance.getNextServiceDate();
                if (nextDate != null && !nextDate.isAfter(through)) {
                    count++;
                }
            }
            if (rows.size() < PAGE_SIZE) {
                return count;
            }
            pageNumber++;
        }
    }

    private int countLicensesExpiring() throws LfrlmsException {
        int count = 0;
        int pageNumber = 1;
        while (true) {
            ArrayList<Customer> rows = customerDao.search(null, Constants.CUSTOMER_INDIVIDUAL,
                    Integer.valueOf(30), "lastName", true, pageNumber, PAGE_SIZE);
            count += rows.size();
            if (rows.size() < PAGE_SIZE) {
                return count;
            }
            pageNumber++;
        }
    }
}
