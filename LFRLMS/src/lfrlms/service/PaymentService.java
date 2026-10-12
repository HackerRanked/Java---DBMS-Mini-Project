package lfrlms.service;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import lfrlms.dao.LeaseDao;
import lfrlms.dao.PaymentDao;
import lfrlms.dao.RentalDao;
import lfrlms.dao.ReportDao;
import lfrlms.exception.LfrlmsException;
import lfrlms.exception.ValidationException;
import lfrlms.model.Lease;
import lfrlms.model.Payment;
import lfrlms.model.Rental;
import lfrlms.util.Constants;
import lfrlms.util.Validator;

/** Applies payment reference, positive-amount and outstanding-balance rules. */
public class PaymentService extends AbstractService {
    private static final int PAGE_SIZE = 100;
    private final PaymentDao paymentDao;
    private final RentalDao rentalDao;
    private final LeaseDao leaseDao;
    private final ReportDao reportDao;

    public PaymentService() {
        this(new PaymentDao(), new RentalDao(), new LeaseDao(), new ReportDao());
    }

    public PaymentService(PaymentDao paymentDao, RentalDao rentalDao,
            LeaseDao leaseDao, ReportDao reportDao) {
        this.paymentDao = paymentDao;
        this.rentalDao = rentalDao;
        this.leaseDao = leaseDao;
        this.reportDao = reportDao;
    }

    public int recordPayment(Payment payment) throws LfrlmsException {
        validateShape(payment);
        try (Connection connection = openConnection()) {
            beginTransaction(connection);
            try {
                validateOutstanding(payment, null);
                int paymentId = paymentDao.insert(connection, payment);
                commit(connection);
                return paymentId;
            } catch (LfrlmsException exception) {
                rollback(connection, exception);
                throw exception;
            } catch (RuntimeException exception) {
                rollback(connection, exception);
                throw exception;
            }
        } catch (SQLException exception) {
            throw databaseFailure("close the payment transaction", exception);
        }
    }

    public BigDecimal customerOutstanding(int customerId) throws LfrlmsException {
        return reportDao.customerOutstanding(customerId);
    }

    public void refund(int paymentId) throws LfrlmsException {
        Payment payment = paymentDao.findById(paymentId);
        if (payment == null || Constants.PAYMENT_REFUNDED.equals(payment.getStatus())) {
            throw new ValidationException("Choose an existing, unrefunded payment.");
        }
        payment.setStatus(Constants.PAYMENT_REFUNDED);
        if (!paymentDao.update(payment)) {
            throw new ValidationException("The payment could not be updated.");
        }
    }

    void recordAdvanceForConfirmedRental(Connection connection, Payment payment,
            Rental rental) throws LfrlmsException {
        validateShape(payment);
        BigDecimal due = outstandingForRental(rental, null);
        if (payment.getAmount().compareTo(due) > 0) {
            throw new ValidationException("The advance exceeds the rental's outstanding balance.");
        }
        paymentDao.insert(connection, payment);
    }

    private void validateOutstanding(Payment payment, Integer ignoredPaymentId)
            throws LfrlmsException {
        BigDecimal due;
        if (payment.getRentalId() != null) {
            Rental rental = rentalDao.findById(payment.getRentalId().intValue());
            if (rental == null) {
                throw new ValidationException("Select an existing rental.");
            }
            due = outstandingForRental(rental, ignoredPaymentId);
        } else {
            Lease lease = leaseDao.findById(payment.getLeaseId().intValue());
            if (lease == null) {
                throw new ValidationException("Select an existing lease.");
            }
            due = outstandingForLease(lease, ignoredPaymentId);
        }
        if (payment.getAmount().compareTo(due) > 0) {
            throw new ValidationException("The payment exceeds the outstanding balance.");
        }
    }

    private BigDecimal outstandingForRental(Rental rental, Integer ignoredPaymentId)
            throws LfrlmsException {
        if (!Constants.RENTAL_ACTIVE.equals(rental.getStatus())
                && !Constants.RENTAL_COMPLETED.equals(rental.getStatus())) {
            return BigDecimal.ZERO.setScale(2);
        }
        BigDecimal total = rental.calculateCharge();
        return total.subtract(successfulPayments(rental.getRentalId(), null, ignoredPaymentId));
    }

    private BigDecimal outstandingForLease(Lease lease, Integer ignoredPaymentId)
            throws LfrlmsException {
        if (!Constants.LEASE_ACTIVE.equals(lease.getStatus())
                && !Constants.LEASE_COMPLETED.equals(lease.getStatus())) {
            return BigDecimal.ZERO.setScale(2);
        }
        BigDecimal total = lease.calculateCharge();
        return total.subtract(successfulPayments(null, lease.getLeaseId(), ignoredPaymentId));
    }

    private BigDecimal successfulPayments(Integer rentalId, Integer leaseId,
            Integer ignoredPaymentId) throws LfrlmsException {
        BigDecimal total = BigDecimal.ZERO.setScale(2);
        HashSet<Integer> seen = new HashSet<Integer>();
        int pageNumber = 1;
        while (true) {
            ArrayList<Payment> page = paymentDao.search(null, null, null, null, false,
                    "paymentDate", true, pageNumber, PAGE_SIZE);
            for (Payment row : page) {
                if (!seen.add(Integer.valueOf(row.getPaymentId()))) {
                    continue;
                }
                boolean sameRental = rentalId != null
                        && rentalId.equals(row.getRentalId());
                boolean sameLease = leaseId != null && leaseId.equals(row.getLeaseId());
                boolean ignored = ignoredPaymentId != null
                        && ignoredPaymentId.intValue() == row.getPaymentId();
                if (!ignored && Constants.PAYMENT_SUCCESS.equals(row.getStatus())
                        && (sameRental || sameLease)) {
                    total = total.add(row.getAmount());
                }
            }
            if (page.size() < PAGE_SIZE) {
                return total;
            }
            pageNumber++;
        }
    }

    private void validateShape(Payment payment) throws ValidationException {
        if (payment == null || !Validator.isPositive(payment.getAmount())) {
            throw new ValidationException("Payment amount must be greater than zero.");
        }
        boolean rentalReference = payment.getRentalId() != null;
        boolean leaseReference = payment.getLeaseId() != null;
        if (rentalReference == leaseReference) {
            throw new ValidationException("Choose exactly one rental or lease reference.");
        }
        if (!isMethod(payment.getMethod()) || !isPaymentStatus(payment.getStatus())) {
            throw new ValidationException("Choose a valid payment method and status.");
        }
        if (payment.getPaymentDate() == null) {
            payment.setPaymentDate(LocalDateTime.now());
        }
    }

    private boolean isMethod(String method) {
        return Constants.PAYMENT_CASH.equals(method) || Constants.PAYMENT_CARD.equals(method)
                || Constants.PAYMENT_BANK_TRANSFER.equals(method)
                || Constants.PAYMENT_UPI.equals(method);
    }

    private boolean isPaymentStatus(String status) {
        return Constants.PAYMENT_SUCCESS.equals(status)
                || Constants.PAYMENT_PENDING.equals(status)
                || Constants.PAYMENT_FAILED.equals(status)
                || Constants.PAYMENT_REFUNDED.equals(status);
    }
}
