package lfrlms.dao;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import lfrlms.exception.LfrlmsException;
import lfrlms.model.IndividualCustomer;
import lfrlms.util.QueryBuilder;
import lfrlms.util.QueryLog;

/**
 * Purpose: Reads and writes individual-only customer subtype columns.
 * Concept: JDBC joins, inheritance mapping and prepared statements.
 */
public class IndividualCustomerDao extends AbstractDao {
    private static final String SELECT_SQL = "SELECT c.customer_id, c.first_name, c.last_name, "
            + "c.email, c.phone, c.street, c.city, c.pincode, c.created_at, c.updated_at, "
            + "i.date_of_birth, i.license_no, i.license_expiry "
            + "FROM individual_customer i JOIN customer c ON c.customer_id = i.customer_id ";
    private static final Map<String, String> SORT_COLUMNS = createSortColumns();

    public IndividualCustomerDao() {
        super();
    }

    public IndividualCustomerDao(QueryLog queryLog) {
        super(queryLog);
    }

    public boolean insert(IndividualCustomer customer) throws LfrlmsException {
        String sql = "INSERT INTO individual_customer "
                + "(customer_id, date_of_birth, license_no, license_expiry) VALUES (?, ?, ?, ?)";
        return executeUpdate(sql, Integer.valueOf(customer.getCustomerId()),
                customer.getDateOfBirth(), customer.getLicenseNo(),
                customer.getLicenseExpiry()) > 0;
    }

    public IndividualCustomer findById(int customerId) throws LfrlmsException {
        return toIndividual(firstRow(queryRows(SELECT_SQL + "WHERE i.customer_id = ?",
                Integer.valueOf(customerId))));
    }

    public ArrayList<IndividualCustomer> findPage(String sortKey, boolean ascending,
            int pageNumber, int pageSize) throws LfrlmsException {
        QueryBuilder query = new QueryBuilder(SELECT_SQL + "WHERE 1 = 1");
        query.appendOrderBy(sortKey, SORT_COLUMNS, "lastName", ascending)
                .appendLimitOffset(pageSize, pageNumber);
        ArrayList<HashMap<String, Object>> rows =
                queryRows(query.getSql(), query.getParameters().toArray());
        ArrayList<IndividualCustomer> customers = new ArrayList<IndividualCustomer>();
        for (HashMap<String, Object> row : rows) {
            customers.add(toIndividual(row));
        }
        return customers;
    }

    public long countAll() throws LfrlmsException {
        return queryCount("SELECT COUNT(*) AS row_count FROM individual_customer");
    }

    public boolean update(IndividualCustomer customer) throws LfrlmsException {
        String sql = "UPDATE individual_customer SET date_of_birth = ?, license_no = ?, "
                + "license_expiry = ? WHERE customer_id = ?";
        return executeUpdate(sql, customer.getDateOfBirth(), customer.getLicenseNo(),
                customer.getLicenseExpiry(), Integer.valueOf(customer.getCustomerId())) > 0;
    }

    public boolean delete(int customerId) throws LfrlmsException {
        return executeUpdate("DELETE FROM individual_customer WHERE customer_id = ?",
                Integer.valueOf(customerId)) > 0;
    }

    private IndividualCustomer toIndividual(HashMap<String, Object> row) {
        if (row == null) {
            return null;
        }
        IndividualCustomer customer = new IndividualCustomer(intValue(row, "customer_id"),
                stringValue(row, "first_name"), stringValue(row, "last_name"),
                stringValue(row, "email"), stringValue(row, "phone"),
                stringValue(row, "street"), stringValue(row, "city"),
                stringValue(row, "pincode"), dateValue(row, "date_of_birth"),
                stringValue(row, "license_no"), dateValue(row, "license_expiry"));
        customer.setCreatedAt(dateTimeValue(row, "created_at"));
        customer.setUpdatedAt(dateTimeValue(row, "updated_at"));
        return customer;
    }

    private static Map<String, String> createSortColumns() {
        HashMap<String, String> columns = new HashMap<String, String>();
        columns.put("lastname", "c.last_name, c.first_name");
        columns.put("firstname", "c.first_name, c.last_name");
        columns.put("licenseexpiry", "i.license_expiry");
        columns.put("id", "c.customer_id");
        return columns;
    }
}
