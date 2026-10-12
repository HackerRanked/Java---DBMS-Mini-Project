package lfrlms.service;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import lfrlms.dao.CustomerDao;
import lfrlms.dao.DriverDao;
import lfrlms.dao.RentalDao;
import lfrlms.dao.VehicleDao;
import lfrlms.exception.IneligibleCustomerException;
import lfrlms.exception.LfrlmsException;
import lfrlms.exception.ValidationException;
import lfrlms.exception.VehicleNotAvailableException;
import lfrlms.model.CorporateCustomer;
import lfrlms.model.Customer;
import lfrlms.model.Driver;
import lfrlms.model.IndividualCustomer;
import lfrlms.model.Payment;
import lfrlms.model.Rental;
import lfrlms.model.Vehicle;
import lfrlms.util.Constants;
import lfrlms.util.Validator;

/** Coordinates rental creation, pickup and return operations. */
public class RentalService extends AbstractService {
    private final RentalDao rentalDao;
    private final VehicleDao vehicleDao;
    private final CustomerDao customerDao;
    private final DriverDao driverDao;
    private final AvailabilityService availabilityService;
    private final PaymentService paymentService;

    public RentalService() {
        this(new RentalDao(), new VehicleDao(), new CustomerDao(), new DriverDao(),
                new AvailabilityService(), new PaymentService());
    }

    public RentalService(RentalDao rentalDao, VehicleDao vehicleDao,
            CustomerDao customerDao, DriverDao driverDao,
            AvailabilityService availabilityService, PaymentService paymentService) {
        this.rentalDao = rentalDao;
        this.vehicleDao = vehicleDao;
        this.customerDao = customerDao;
        this.driverDao = driverDao;
        this.availabilityService = availabilityService;
        this.paymentService = paymentService;
    }

    public int createRental(Rental rental) throws LfrlmsException {
        return createRental(rental, null);
    }

    public int createRentalWithAdvancePayment(Rental rental, Payment payment)
            throws LfrlmsException {
        if (payment == null) {
            throw new ValidationException("An advance payment is required.");
        }
        return createRental(rental, payment);
    }

    public void startRental(int rentalId, LocalDateTime pickupDateTime,
            int odometerOut, String conditionOut) throws LfrlmsException {
        if (pickupDateTime == null || !Validator.isNonNegative(Integer.valueOf(odometerOut))
                || !Validator.isValid(conditionOut)) {
            throw new ValidationException("Enter the pickup time, odometer and condition.");
        }
        Rental initial = rentalDao.findById(rentalId);
        if (initial == null) {
            throw new ValidationException("The rental record no longer exists.");
        }
        try (Connection connection = openConnection()) {
            beginTransaction(connection);
            try {
                Vehicle vehicle = vehicleDao.lockByIdForUpdate(connection, initial.getVehicleId());
                Rental rental = rentalDao.findById(rentalId);
                if (vehicle == null || rental == null
                        || !Constants.RENTAL_CONFIRMED.equals(rental.getStatus())) {
                    throw new ValidationException("Only a confirmed rental can be started.");
                }
                if (!pickupDateTime.isBefore(rental.getExpectedReturnDatetime())) {
                    throw new ValidationException("Pickup must be before expected return.");
                }
                validateCustomerAndDriver(rental, rental.getExpectedReturnDatetime());
                if (!availabilityService.isVehicleAvailable(rental.getVehicleId(),
                        pickupDateTime, rental.getExpectedReturnDatetime(),
                        Integer.valueOf(rentalId), null)) {
                    throw new VehicleNotAvailableException(
                            "The vehicle has another commitment during this rental.");
                }
                if (rental.getDriverId() != null
                        && !availabilityService.isDriverAvailable(rental.getDriverId().intValue(),
                        pickupDateTime, rental.getExpectedReturnDatetime(),
                        Integer.valueOf(rentalId))) {
                    throw new IneligibleCustomerException(
                            "The assigned chauffeur is not available for this period.");
                }
                rental.setActualPickupDatetime(pickupDateTime);
                rental.setOdometerOut(Integer.valueOf(odometerOut));
                rental.setConditionOut(conditionOut.trim());
                rental.setStatus(Constants.RENTAL_ACTIVE);
                if (!rentalDao.update(connection, rental)) {
                    throw new ValidationException("The rental record could not be started.");
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
            throw databaseFailure("close the rental transaction", exception);
        }
    }

    public void completeRental(int rentalId, LocalDateTime returnDateTime,
            int odometerIn, String conditionIn) throws LfrlmsException {
        if (returnDateTime == null || !Validator.isNonNegative(Integer.valueOf(odometerIn))
                || !Validator.isValid(conditionIn)) {
            throw new ValidationException("Enter the return time, odometer and condition.");
        }
        Rental rental = rentalDao.findById(rentalId);
        if (rental == null || !Constants.RENTAL_ACTIVE.equals(rental.getStatus())) {
            throw new ValidationException("Only an active rental can be completed.");
        }
        if (rental.getOdometerOut() == null || odometerIn < rental.getOdometerOut().intValue()
                || rental.getActualPickupDatetime() == null
                || !returnDateTime.isAfter(rental.getActualPickupDatetime())) {
            throw new ValidationException("Return details must follow the pickup details.");
        }
        rentalDao.completeRental(rentalId, returnDateTime, odometerIn, conditionIn.trim());
    }

    public void cancelRental(int rentalId) throws LfrlmsException {
        Rental rental = rentalDao.findById(rentalId);
        if (rental == null || !Constants.RENTAL_CONFIRMED.equals(rental.getStatus())) {
            throw new ValidationException("Only a confirmed rental can be cancelled.");
        }
        rental.setStatus(Constants.RENTAL_CANCELLED);
        if (!rentalDao.update(rental)) {
            throw new ValidationException("The rental record could not be cancelled.");
        }
    }

    private int createRental(Rental rental, Payment advancePayment) throws LfrlmsException {
        validateRentalShape(rental);
        try (Connection connection = openConnection()) {
            beginTransaction(connection);
            try {
                Vehicle vehicle = vehicleDao.lockByIdForUpdate(connection, rental.getVehicleId());
                if (vehicle == null || vehicle.getRentalRate() == null
                        || Constants.VEHICLE_MAINTENANCE.equals(vehicle.getStatus())
                        || Constants.VEHICLE_INACTIVE.equals(vehicle.getStatus())) {
                    throw new VehicleNotAvailableException(
                            "The selected vehicle cannot be rented.");
                }
                validateCustomerAndDriver(rental, rental.getExpectedReturnDatetime());
                LocalDateTime from = Constants.RENTAL_ACTIVE.equals(rental.getStatus())
                        ? rental.getActualPickupDatetime() : rental.getScheduledStartDatetime();
                if (!availabilityService.isVehicleAvailable(rental.getVehicleId(), from,
                        rental.getExpectedReturnDatetime(), null, null)) {
                    throw new VehicleNotAvailableException(
                            "The vehicle is not available for the full requested period.");
                }
                if (rental.getDriverId() != null
                        && !availabilityService.isDriverAvailable(rental.getDriverId().intValue(),
                        from, rental.getExpectedReturnDatetime(), null)) {
                    throw new IneligibleCustomerException(
                            "The assigned chauffeur is not available for this period.");
                }
                rental.setDailyRateApplied(vehicle.getRentalRate());
                int rentalId = rentalDao.insert(connection, rental);
                rental.setRentalId(rentalId);
                if (advancePayment != null) {
                    advancePayment.setRentalId(Integer.valueOf(rentalId));
                    advancePayment.setLeaseId(null);
                    paymentService.recordAdvanceForConfirmedRental(
                            connection, advancePayment, rental);
                }
                commit(connection);
                return rentalId;
            } catch (LfrlmsException exception) {
                rollback(connection, exception);
                throw exception;
            } catch (RuntimeException exception) {
                rollback(connection, exception);
                throw exception;
            }
        } catch (SQLException exception) {
            throw databaseFailure("close the rental transaction", exception);
        }
    }

    private void validateRentalShape(Rental rental) throws LfrlmsException {
        if (rental == null || rental.getCustomerId() <= 0 || rental.getVehicleId() <= 0
                || rental.getScheduledStartDatetime() == null
                || rental.getExpectedReturnDatetime() == null
                || !rental.getExpectedReturnDatetime().isAfter(rental.getScheduledStartDatetime())
                || (!Constants.RENTAL_CONFIRMED.equals(rental.getStatus())
                && !Constants.RENTAL_ACTIVE.equals(rental.getStatus()))) {
            throw new ValidationException("Enter a valid confirmed or walk-in rental.");
        }
        if (rental.getScheduledStartDatetime().isBefore(LocalDateTime.now().minusMinutes(5))) {
            throw new ValidationException("A new rental cannot be scheduled in the past.");
        }
        if (Constants.RENTAL_CONFIRMED.equals(rental.getStatus())
                && (rental.getActualPickupDatetime() != null || rental.getOdometerOut() != null
                || rental.getConditionOut() != null)) {
            throw new ValidationException("A confirmed rental cannot contain pickup details.");
        }
        if (Constants.RENTAL_ACTIVE.equals(rental.getStatus())
                && (rental.getActualPickupDatetime() == null || rental.getOdometerOut() == null
                || !Validator.isNonNegative(rental.getOdometerOut())
                || !Validator.isValid(rental.getConditionOut())
                || !rental.getScheduledStartDatetime().equals(
                rental.getActualPickupDatetime())
                || rental.getActualPickupDatetime().isBefore(LocalDateTime.now().minusMinutes(5))
                || rental.getActualPickupDatetime().isAfter(LocalDateTime.now().plusMinutes(5)))) {
            throw new ValidationException("A walk-in rental needs complete pickup details.");
        }
    }

    private void validateCustomerAndDriver(Rental rental, LocalDateTime contractEnd)
            throws LfrlmsException {
        Customer customer = customerDao.findById(rental.getCustomerId());
        if (customer == null) {
            throw new IneligibleCustomerException("Select an existing customer.");
        }
        if (customer instanceof IndividualCustomer) {
            IndividualCustomer individual = (IndividualCustomer) customer;
            if (!Validator.isEligibleThrough(individual.getLicenseExpiry(),
                    contractEnd.toLocalDate())) {
                throw new IneligibleCustomerException(
                        "The individual's licence must remain valid through the rental end date.");
            }
        } else if (customer instanceof CorporateCustomer && rental.getDriverId() == null) {
            throw new IneligibleCustomerException(
                    "Every corporate rental must have an active chauffeur.");
        }
        if (rental.getDriverId() != null) {
            Driver driver = driverDao.findById(rental.getDriverId().intValue());
            if (driver == null || !Constants.DRIVER_ACTIVE.equals(driver.getStatus())) {
                throw new IneligibleCustomerException("The assigned chauffeur is not active.");
            }
        }
    }
}
