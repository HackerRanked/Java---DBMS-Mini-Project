package lfrlms;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import lfrlms.dao.BrandDao;
import lfrlms.dao.ReportDao;
import lfrlms.dao.VehicleDao;
import lfrlms.exception.LfrlmsException;
import lfrlms.exception.RecordInUseException;
import lfrlms.model.Brand;
import lfrlms.model.Vehicle;
import lfrlms.util.AppLogger;
import lfrlms.util.QueryBuilder;
import lfrlms.util.QueryLog;

/**
 * Purpose: Exercises key DAO reads, writes, calls and query logging.
 * Concept: JDBC smoke checks, checked exceptions and anonymous classes.
 */
public class DaoSmokeTest {
    private static final int REPORT_PAGE_SIZE = 100;
    private static final QueryLog QUERY_LOG = new QueryLog();
    private static final BrandDao BRAND_DAO = new BrandDao(QUERY_LOG);
    private static final VehicleDao VEHICLE_DAO = new VehicleDao(QUERY_LOG);
    private static final ReportDao REPORT_DAO = new ReportDao(QUERY_LOG);

    private DaoSmokeTest() {
    }

    public static void main(String[] args) {
        testVehicleSearch();
        testBrandCrudOutput();
        testReferencedBrandOutput();
        testAvailabilityProcedure();
        testOutstandingFunction();
        testReportPageCounts();
        testQueryLogOutput();
    }

    private static void testVehicleSearch() {
        runTest("filtered vehicle search, pagination and matching count", new SmokeCheck() {
            @Override
            public String run() throws Exception {
                ArrayList<Vehicle> page = VEHICLE_DAO.search("Lexus", 1, 2);
                long count = VEHICLE_DAO.countSearchResults("Lexus", null, null, null,
                        null, null, null, null, null, null, null);
                if (page.size() > 2 || page.isEmpty() || count < page.size()) {
                    throw new Exception("Search page and matching count were inconsistent.");
                }
                String pattern = QueryBuilder.escapeLikeWildcards("Lexus%_!");
                if (!"Lexus!%!_!!".equals(pattern)) {
                    throw new Exception("LIKE wildcard characters were not escaped.");
                }
                return "page rows=" + page.size() + ", matching rows=" + count;
            }
        });
    }

    private static void testBrandCrudOutput() {
        runTest("insert, read, update and delete a throwaway brand", new SmokeCheck() {
            @Override
            public String run() throws Exception {
                testBrandCrud();
                return "all four CRUD operations passed";
            }
        });
    }

    private static void testReferencedBrandOutput() {
        runTest("referenced brand deletion returns RecordInUseException", new SmokeCheck() {
            @Override
            public String run() throws Exception {
                testReferencedBrandDelete();
                return "caught the expected checked exception";
            }
        });
    }

    private static void testAvailabilityProcedure() {
        runTest("sp_available_vehicles CallableStatement", new SmokeCheck() {
            @Override
            public String run() throws Exception {
                LocalDateTime from = LocalDateTime.now().plusDays(100);
                LocalDateTime to = from.plusDays(1);
                int rows = VEHICLE_DAO.findAvailableVehicles(from, to, null, null).size();
                return "procedure returned " + rows + " rows";
            }
        });
    }

    private static void testOutstandingFunction() {
        runTest("fn_customer_outstanding CallableStatement", new SmokeCheck() {
            @Override
            public String run() throws Exception {
                BigDecimal outstanding = REPORT_DAO.customerOutstanding(1);
                if (outstanding == null) {
                    throw new Exception("Function returned a null balance.");
                }
                return "customer 1 balance=" + outstanding.toPlainString();
            }
        });
    }

    private static void testReportPageCounts() {
        checkReportCount("active rentals count matches all pages", new ReportCountCheck() {
            public long count() throws Exception {
                return REPORT_DAO.countActiveRentals();
            }

            public ArrayList<HashMap<String, Object>> page(int pageNumber, int pageSize)
                    throws Exception {
                return REPORT_DAO.activeRentals(pageNumber, pageSize);
            }
        });
        checkReportCount("active leases count matches all pages", new ReportCountCheck() {
            public long count() throws Exception {
                return REPORT_DAO.countActiveLeases();
            }

            public ArrayList<HashMap<String, Object>> page(int pageNumber, int pageSize)
                    throws Exception {
                return REPORT_DAO.activeLeases(pageNumber, pageSize);
            }
        });
        checkReportCount("outstanding balances count matches all pages",
                new ReportCountCheck() {
            public long count() throws Exception {
                return REPORT_DAO.countOutstandingBalances();
            }

            public ArrayList<HashMap<String, Object>> page(int pageNumber, int pageSize)
                    throws Exception {
                return REPORT_DAO.outstandingBalances(pageNumber, pageSize);
            }
        });
        final LocalDate throughDate = LocalDate.now().plusYears(10);
        checkReportCount("maintenance due count matches all pages", new ReportCountCheck() {
            public long count() throws Exception {
                return REPORT_DAO.countMaintenanceDue(throughDate);
            }

            public ArrayList<HashMap<String, Object>> page(int pageNumber, int pageSize)
                    throws Exception {
                return REPORT_DAO.maintenanceDue(throughDate, pageNumber, pageSize);
            }
        });
        checkReportCount("customer history count matches all pages",
                new ReportCountCheck() {
            public long count() throws Exception {
                return REPORT_DAO.countCustomerTransactionHistory(1);
            }

            public ArrayList<HashMap<String, Object>> page(int pageNumber, int pageSize)
                    throws Exception {
                return REPORT_DAO.customerTransactionHistory(1, pageNumber, pageSize);
            }
        });
    }

    private static void checkReportCount(String name, final ReportCountCheck report) {
        runTest(name, new SmokeCheck() {
            public String run() throws Exception {
                long expected = report.count();
                long fetched = fetchAllReportRows(report);
                if (expected != fetched) {
                    throw new IllegalStateException("COUNT returned " + expected
                            + " but paging fetched " + fetched + " rows.");
                }
                return "count=" + expected + ", fetched rows=" + fetched;
            }
        });
    }

    private static long fetchAllReportRows(ReportCountCheck report) throws Exception {
        long total = 0;
        int pageNumber = 1;
        while (true) {
            ArrayList<HashMap<String, Object>> page =
                    report.page(pageNumber, REPORT_PAGE_SIZE);
            total += page.size();
            if (page.size() < REPORT_PAGE_SIZE) {
                return total;
            }
            pageNumber++;
        }
    }

    private static void testQueryLogOutput() {
        runTest("QueryLog records DAO statements", new SmokeCheck() {
            @Override
            public String run() throws Exception {
                if (QUERY_LOG.size() < 8 || QUERY_LOG.getRecentEntries().isEmpty()) {
                    throw new Exception("The shared QueryLog did not record the DAO calls.");
                }
                return "recorded " + QUERY_LOG.size() + " statements";
            }
        });
    }

    private static void testBrandCrud() throws LfrlmsException {
        int brandId = 0;
        boolean deleted = false;
        String originalName = "DAO Smoke " + System.nanoTime();
        try {
            brandId = BRAND_DAO.insert(new Brand(0, originalName));
            if (brandId <= 0 || BRAND_DAO.findById(brandId) == null) {
                throw new IllegalStateException("Inserted brand could not be read back.");
            }
            String updatedName = originalName + " Updated";
            if (!BRAND_DAO.update(new Brand(brandId, updatedName))) {
                throw new IllegalStateException("Brand update did not affect a row.");
            }
            Brand updated = BRAND_DAO.findById(brandId);
            if (updated == null || !updatedName.equals(updated.getBrandName())) {
                throw new IllegalStateException("Updated brand value did not persist.");
            }
            deleted = BRAND_DAO.delete(brandId);
            if (!deleted || BRAND_DAO.findById(brandId) != null) {
                throw new IllegalStateException("Deleted brand was still present.");
            }
        } finally {
            if (brandId > 0 && !deleted) {
                try {
                    BRAND_DAO.delete(brandId);
                } catch (LfrlmsException cleanupException) {
                    AppLogger.logException(DaoSmokeTest.class.getName(),
                            "Could not clean up the throwaway brand.", cleanupException);
                }
            }
        }
    }

    private static void testReferencedBrandDelete() throws LfrlmsException {
        ArrayList<Vehicle> vehicles = VEHICLE_DAO.search(null, 1, 1);
        if (vehicles.isEmpty()) {
            throw new IllegalStateException("No vehicle exists for the foreign-key check.");
        }
        int referencedBrandId = vehicles.get(0).getBrandId();
        try {
            BRAND_DAO.delete(referencedBrandId);
        } catch (RecordInUseException expected) {
            if (!"Deactivate it instead".equals(expected.getMessage())) {
                throw new IllegalStateException("RecordInUseException message did not match.");
            }
            return;
        }
        throw new IllegalStateException("Referenced brand deletion was not rejected.");
    }

    private static void runTest(String name, SmokeCheck check) {
        try {
            String detail = check.run();
            System.out.println("PASS: " + name + " (" + detail + ")");
        } catch (Exception exception) {
            AppLogger.logException(DaoSmokeTest.class.getName(),
                    "Smoke check failed: " + name, exception);
            System.out.println("FAIL: " + name + " (" + exception.getMessage() + ")");
        }
    }

    private interface SmokeCheck {
        String run() throws Exception;
    }

    private interface ReportCountCheck {
        long count() throws Exception;
        ArrayList<HashMap<String, Object>> page(int pageNumber, int pageSize)
                throws Exception;
    }
}
