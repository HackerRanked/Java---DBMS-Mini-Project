package lfrlms.service;

import java.sql.Connection;
import java.sql.SQLException;
import lfrlms.dao.CorporateCustomerDao;
import lfrlms.dao.CustomerDao;
import lfrlms.dao.IndividualCustomerDao;
import lfrlms.exception.LfrlmsException;
import lfrlms.exception.ValidationException;
import lfrlms.model.CorporateCustomer;
import lfrlms.model.Customer;
import lfrlms.model.IndividualCustomer;
import lfrlms.util.Validator;

/** Saves each customer parent row and its single subtype in one transaction. */
public class CustomerService extends AbstractService {
    private final CustomerDao customerDao;
    private final IndividualCustomerDao individualDao;
    private final CorporateCustomerDao corporateDao;

    public CustomerService() {
        this(new CustomerDao(), new IndividualCustomerDao(), new CorporateCustomerDao());
    }

    public CustomerService(CustomerDao customerDao, IndividualCustomerDao individualDao,
            CorporateCustomerDao corporateDao) {
        this.customerDao = customerDao;
        this.individualDao = individualDao;
        this.corporateDao = corporateDao;
    }

    public int save(Customer customer) throws LfrlmsException {
        validate(customer);
        try (Connection connection = openConnection()) {
            beginTransaction(connection);
            try {
                int customerId = customer.getCustomerId();
                if (customerId <= 0) {
                    customerId = customerDao.insert(connection, customer);
                    customer.setCustomerId(customerId);
                    insertSubtype(connection, customer);
                } else {
                    if (!customerDao.update(connection, customer)) {
                        throw new ValidationException("The customer record no longer exists.");
                    }
                    synchronizeSubtype(connection, customer);
                }
                commit(connection);
                return customerId;
            } catch (LfrlmsException exception) {
                rollback(connection, exception);
                throw exception;
            } catch (RuntimeException exception) {
                rollback(connection, exception);
                throw exception;
            }
        } catch (SQLException exception) {
            throw databaseFailure("close the customer transaction", exception);
        }
    }

    public boolean delete(int customerId) throws LfrlmsException {
        try (Connection connection = openConnection()) {
            beginTransaction(connection);
            try {
                individualDao.delete(connection, customerId);
                corporateDao.delete(connection, customerId);
                boolean deleted = customerDao.delete(connection, customerId);
                commit(connection);
                return deleted;
            } catch (LfrlmsException exception) {
                rollback(connection, exception);
                throw exception;
            } catch (RuntimeException exception) {
                rollback(connection, exception);
                throw exception;
            }
        } catch (SQLException exception) {
            throw databaseFailure("close the customer transaction", exception);
        }
    }

    public Customer findById(int customerId) throws LfrlmsException {
        return customerDao.findById(customerId);
    }

    private void synchronizeSubtype(Connection connection, Customer customer)
            throws LfrlmsException {
        if (customer instanceof IndividualCustomer) {
            corporateDao.delete(connection, customer.getCustomerId());
            IndividualCustomer individual = (IndividualCustomer) customer;
            if (!individualDao.update(connection, individual)) {
                individualDao.insert(connection, individual);
            }
        } else if (customer instanceof CorporateCustomer) {
            individualDao.delete(connection, customer.getCustomerId());
            CorporateCustomer corporate = (CorporateCustomer) customer;
            if (!corporateDao.update(connection, corporate)) {
                corporateDao.insert(connection, corporate);
            }
        }
    }

    private void insertSubtype(Connection connection, Customer customer)
            throws LfrlmsException {
        if (customer instanceof IndividualCustomer) {
            individualDao.insert(connection, (IndividualCustomer) customer);
        } else if (customer instanceof CorporateCustomer) {
            corporateDao.insert(connection, (CorporateCustomer) customer);
        } else {
            throw new ValidationException("Choose an individual or corporate customer.");
        }
    }

    private void validate(Customer customer) throws ValidationException {
        if (customer == null || !Validator.isValid(customer.getFirstName())
                || !Validator.isValid(customer.getLastName())
                || !Validator.isValidEmail(customer.getEmail())
                || !Validator.isValidPhone(customer.getPhone())
                || !Validator.isValid(customer.getStreet())
                || !Validator.isValid(customer.getCity())
                || !Validator.isValidPincode(customer.getPincode())) {
            throw new ValidationException("Enter valid customer contact details.");
        }
        if (customer instanceof IndividualCustomer) {
            IndividualCustomer individual = (IndividualCustomer) customer;
            if (individual.getDateOfBirth() == null
                    || !Validator.isValidLicenseNumber(individual.getLicenseNo())
                    || individual.getLicenseExpiry() == null) {
                throw new ValidationException("Enter the individual's birth and licence details.");
            }
        } else if (customer instanceof CorporateCustomer) {
            CorporateCustomer corporate = (CorporateCustomer) customer;
            if (!Validator.isValid(corporate.getCompanyName())
                    || !Validator.isValidGstNo(corporate.getGstNo())) {
                throw new ValidationException("Enter a valid company name and GST number.");
            }
        } else {
            throw new ValidationException("Choose an individual or corporate customer.");
        }
    }
}
