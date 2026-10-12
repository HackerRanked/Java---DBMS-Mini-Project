package lfrlms.service;

import lfrlms.dao.DriverDao;
import lfrlms.exception.LfrlmsException;
import lfrlms.exception.ValidationException;
import lfrlms.model.Driver;
import lfrlms.util.Constants;
import lfrlms.util.Validator;

/** Validates active chauffeurs before assignments are handled by RentalService. */
public class DriverService {
    private final DriverDao driverDao;

    public DriverService() {
        this(new DriverDao());
    }

    public DriverService(DriverDao driverDao) {
        this.driverDao = driverDao;
    }

    public int save(Driver driver) throws LfrlmsException {
        if (driver == null || !Validator.isValid(driver.getFirstName())
                || !Validator.isValid(driver.getLastName())
                || !Validator.isValidPhone(driver.getPhone())
                || !Validator.isValidLicenseNumber(driver.getLicenseNo())
                || (!Constants.DRIVER_ACTIVE.equals(driver.getStatus())
                && !Constants.DRIVER_INACTIVE.equals(driver.getStatus()))) {
            throw new ValidationException("Enter valid chauffeur details.");
        }
        if (driver.getDriverId() <= 0) {
            return driverDao.insert(driver);
        }
        if (!driverDao.update(driver)) {
            throw new ValidationException("The chauffeur record could not be updated.");
        }
        return driver.getDriverId();
    }

    public void deactivate(int driverId) throws LfrlmsException {
        Driver driver = driverDao.findById(driverId);
        if (driver == null) {
            throw new ValidationException("The chauffeur record no longer exists.");
        }
        driver.setStatus(Constants.DRIVER_INACTIVE);
        driverDao.update(driver);
    }

    public boolean delete(int driverId) throws LfrlmsException {
        return driverDao.delete(driverId);
    }
}
