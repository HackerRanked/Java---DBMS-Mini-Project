package lfrlms.service;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import lfrlms.dao.CustomerDao;
import lfrlms.dao.LeaseDao;
import lfrlms.dao.VehicleDao;
import lfrlms.exception.IneligibleCustomerException;
import lfrlms.exception.LfrlmsException;
import lfrlms.exception.ValidationException;
import lfrlms.exception.VehicleNotAvailableException;
import lfrlms.model.Customer;
import lfrlms.model.IndividualCustomer;
import lfrlms.model.Lease;
import lfrlms.model.Vehicle;
import lfrlms.util.Constants;
import lfrlms.util.Validator;

/** Coordinates lease reservations, starts and terminal transitions. */
public class LeaseService extends AbstractService {
    private final LeaseDao leaseDao;
    private final VehicleDao vehicleDao;
    private final CustomerDao customerDao;
    private final AvailabilityService availabilityService;

    public LeaseService() {
        this(new LeaseDao(), new VehicleDao(), new CustomerDao(), new AvailabilityService());
    }

    public LeaseService(LeaseDao leaseDao, VehicleDao vehicleDao,
            CustomerDao customerDao, AvailabilityService availabilityService) {
        this.leaseDao = leaseDao;
        this.vehicleDao = vehicleDao;
        this.customerDao = customerDao;
        this.availabilityService = availabilityService;
    }

    public int createLease(Lease lease) throws LfrlmsException {
        if (lease == null || lease.getCustomerId() <= 0 || lease.getVehicleId() <= 0
                || !Validator.isValid(lease.getStartDate(), lease.getEndDate())
                || (lease.getDeposit() != null && !Validator.isNonNegative(lease.getDeposit()))
                || !Constants.LEASE_SCHEDULED.equals(lease.getStatus())) {
            throw new ValidationException("Enter a valid scheduled lease period.");
        }
        try (Connection connection = openConnection()) {
            beginTransaction(connection);
            try {
                Vehicle vehicle = vehicleDao.lockByIdForUpdate(connection, lease.getVehicleId());
                if (vehicle == null || vehicle.getLeaseRate() == null
                        || Constants.VEHICLE_MAINTENANCE.equals(vehicle.getStatus())
                        || Constants.VEHICLE_INACTIVE.equals(vehicle.getStatus())) {
                    throw new VehicleNotAvailableException("The selected vehicle cannot be leased.");
                }
                validateCustomer(lease);
                if (!availabilityService.isVehicleAvailable(lease.getVehicleId(),
                        lease.getStartDate().atStartOfDay(),
                        lease.getEndDate().plusDays(1).atStartOfDay(), null, null)) {
                    throw new VehicleNotAvailableException(
                            "The vehicle is not available for the full lease period.");
                }
                lease.setMonthlyCharge(vehicle.getLeaseRate());
                int leaseId = leaseDao.insert(connection, lease);
                lease.setLeaseId(leaseId);
                commit(connection);
                return leaseId;
            } catch (LfrlmsException exception) {
                rollback(connection, exception);
                throw exception;
            } catch (RuntimeException exception) {
                rollback(connection, exception);
                throw exception;
            }
        } catch (SQLException exception) {
            throw databaseFailure("close the lease transaction", exception);
        }
    }

    public int scheduleLease(int customerId, int vehicleId, LocalDate startDate,
            LocalDate endDate, BigDecimal deposit) throws LfrlmsException {
        if (deposit == null || !Validator.isNonNegative(deposit)) {
            throw new ValidationException("Lease deposit must be zero or greater.");
        }
        Lease lease = new Lease();
        lease.setCustomerId(customerId);
        lease.setVehicleId(vehicleId);
        lease.setStartDate(startDate);
        lease.setEndDate(endDate);
        lease.setDeposit(deposit);
        lease.setStatus(Constants.LEASE_SCHEDULED);
        return createLease(lease);
    }

    public void startLease(int leaseId) throws LfrlmsException {
        Lease initial = leaseDao.findById(leaseId);
        if (initial == null) {
            throw new ValidationException("The lease record no longer exists.");
        }
        try (Connection connection = openConnection()) {
            beginTransaction(connection);
            try {
                Vehicle vehicle = vehicleDao.lockByIdForUpdate(connection, initial.getVehicleId());
                Lease lease = leaseDao.findById(leaseId);
                if (vehicle == null || lease == null
                        || !Constants.LEASE_SCHEDULED.equals(lease.getStatus())) {
                    throw new ValidationException("Only a scheduled lease can be started.");
                }
                validateCustomer(lease);
                LocalDateTime now = LocalDateTime.now();
                LocalDateTime scheduledStart = lease.getStartDate().atStartOfDay();
                LocalDateTime availabilityFrom = now.isBefore(scheduledStart)
                        ? now : scheduledStart;
                if (!availabilityService.isVehicleAvailable(lease.getVehicleId(),
                        availabilityFrom,
                        lease.getEndDate().plusDays(1).atStartOfDay(), null,
                        Integer.valueOf(leaseId))) {
                    throw new VehicleNotAvailableException(
                            "The vehicle has another commitment during this lease.");
                }
                lease.setStatus(Constants.LEASE_ACTIVE);
                vehicle.setStatus(Constants.VEHICLE_LEASED);
                if (!leaseDao.update(connection, lease) || !vehicleDao.update(connection, vehicle)) {
                    throw new ValidationException("The lease could not be started.");
                }
                commit(connection);
            } catch (LfrlmsException exception) {
                rollback(connection, exception);
                throw exception;
            } catch (RuntimeException exception) {
                rollback(connection, exception);
                throw exception;
            }
        } catch (SQLException exception) {
            throw databaseFailure("close the lease transaction", exception);
        }
    }

    public void finishLease(int leaseId, String finalStatus) throws LfrlmsException {
        if (!Constants.LEASE_COMPLETED.equals(finalStatus)
                && !Constants.LEASE_TERMINATED.equals(finalStatus)) {
            throw new ValidationException("Choose Completed or Terminated as the final status.");
        }
        Lease initial = leaseDao.findById(leaseId);
        if (initial == null || (!Constants.LEASE_ACTIVE.equals(initial.getStatus())
                && !Constants.LEASE_SCHEDULED.equals(initial.getStatus()))) {
            throw new ValidationException("Only an active or scheduled lease can be ended.");
        }
        try (Connection connection = openConnection()) {
            beginTransaction(connection);
            try {
                Vehicle vehicle = vehicleDao.lockByIdForUpdate(connection, initial.getVehicleId());
                Lease lease = leaseDao.findById(leaseId);
                if (lease == null) {
                    throw new ValidationException("The lease record no longer exists.");
                }
                boolean wasActive = Constants.LEASE_ACTIVE.equals(lease.getStatus());
                lease.setStatus(finalStatus);
                if (!leaseDao.update(connection, lease)) {
                    throw new ValidationException("The lease could not be updated.");
                }
                if (wasActive && vehicle != null) {
                    vehicle.setStatus(Constants.VEHICLE_AVAILABLE);
                    vehicleDao.update(connection, vehicle);
                }
                commit(connection);
            } catch (LfrlmsException exception) {
                rollback(connection, exception);
                throw exception;
            } catch (RuntimeException exception) {
                rollback(connection, exception);
                throw exception;
            }
        } catch (SQLException exception) {
            throw databaseFailure("close the lease transaction", exception);
        }
    }

    public Lease findById(int leaseId) throws LfrlmsException {
        return leaseDao.findById(leaseId);
    }

    public ArrayList<Lease> search(String text, String status,
            LocalDate from, LocalDate to, boolean outstandingOnly,
            String sortKey, boolean ascending, int pageNumber, int pageSize)
            throws LfrlmsException {
        return leaseDao.search(text, status, from, to, outstandingOnly,
                sortKey, ascending, pageNumber, pageSize);
    }

    public long countSearchResults(String text, String status, LocalDate from,
            LocalDate to, boolean outstandingOnly) throws LfrlmsException {
        return leaseDao.countSearchResults(text, status, from, to, outstandingOnly);
    }

    private void validateCustomer(Lease lease) throws LfrlmsException {
        Customer customer = customerDao.findById(lease.getCustomerId());
        if (customer == null) {
            throw new IneligibleCustomerException("Select an existing customer.");
        }
        if (customer instanceof IndividualCustomer
                && !Validator.isEligibleThrough(
                ((IndividualCustomer) customer).getLicenseExpiry(), lease.getEndDate())) {
            throw new IneligibleCustomerException(
                    "The individual's licence must remain valid through the lease end date.");
        }
    }
}
