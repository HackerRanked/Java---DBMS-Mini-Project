package lfrlms.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.TreeMap;
import lfrlms.dao.ReportDao;
import lfrlms.exception.LfrlmsException;

/** Exposes the report queries without moving SQL or database work into the GUI. */
public class ReportService {
    private final ReportDao reportDao;

    public ReportService() {
        this(new ReportDao());
    }

    public ReportService(ReportDao reportDao) {
        this.reportDao = reportDao;
    }

    public ArrayList<HashMap<String, Object>> availableVehicles(LocalDateTime from,
            LocalDateTime to, Integer branchId, Integer categoryId)
            throws LfrlmsException {
        return reportDao.availableVehicles(from, to, branchId, categoryId);
    }

    public ArrayList<HashMap<String, Object>> activeRentals(int pageNumber, int pageSize)
            throws LfrlmsException {
        return reportDao.activeRentals(pageNumber, pageSize);
    }

    public ArrayList<HashMap<String, Object>> activeLeases(int pageNumber, int pageSize)
            throws LfrlmsException {
        return reportDao.activeLeases(pageNumber, pageSize);
    }

    public TreeMap<String, BigDecimal> revenueByMonth(LocalDate from, LocalDate to)
            throws LfrlmsException {
        return reportDao.revenueByMonth(from, to);
    }

    public ArrayList<HashMap<String, Object>> outstandingBalances(int pageNumber,
            int pageSize) throws LfrlmsException {
        return reportDao.outstandingBalances(pageNumber, pageSize);
    }

    public ArrayList<HashMap<String, Object>> mostRentedVehicles(int limit)
            throws LfrlmsException {
        return reportDao.mostRentedVehicles(limit);
    }

    public ArrayList<HashMap<String, Object>> maintenanceDue(LocalDate throughDate,
            int pageNumber, int pageSize) throws LfrlmsException {
        return reportDao.maintenanceDue(throughDate, pageNumber, pageSize);
    }

    public ArrayList<HashMap<String, Object>> customerTransactionHistory(int customerId,
            int pageNumber, int pageSize) throws LfrlmsException {
        return reportDao.customerTransactionHistory(customerId, pageNumber, pageSize);
    }

    public long countCustomerTransactionHistory(int customerId) throws LfrlmsException {
        return reportDao.countCustomerTransactionHistory(customerId);
    }

    public BigDecimal customerOutstanding(int customerId) throws LfrlmsException {
        return reportDao.customerOutstanding(customerId);
    }

    public ArrayList<HashMap<String, Object>> rentalStatusSummary()
            throws LfrlmsException {
        return reportDao.rentalStatusSummary();
    }
}
