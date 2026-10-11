package lfrlms.dao;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import lfrlms.exception.LfrlmsException;
import lfrlms.model.VehicleCategory;
import lfrlms.util.QueryBuilder;
import lfrlms.util.QueryLog;

/**
 * Purpose: Reads and writes the vehicle category lookup table.
 * Concept: JDBC CRUD and server-side pagination.
 */
public class VehicleCategoryDao extends AbstractDao {
    private static final Map<String, String> SORT_COLUMNS = createSortColumns();

    public VehicleCategoryDao() {
        super();
    }

    public VehicleCategoryDao(QueryLog queryLog) {
        super(queryLog);
    }

    public int insert(VehicleCategory category) throws LfrlmsException {
        String sql = "INSERT INTO vehicle_category (category_name, description) VALUES (?, ?)";
        return executeInsert(sql, category.getCategoryName(), category.getDescription());
    }

    public VehicleCategory findById(int categoryId) throws LfrlmsException {
        String sql = "SELECT category_id, category_name, description "
                + "FROM vehicle_category WHERE category_id = ?";
        return toCategory(firstRow(queryRows(sql, Integer.valueOf(categoryId))));
    }

    public ArrayList<VehicleCategory> findPage(String sortKey, boolean ascending,
            int pageNumber, int pageSize) throws LfrlmsException {
        QueryBuilder query = new QueryBuilder(
                "SELECT category_id, category_name, description "
                        + "FROM vehicle_category WHERE 1 = 1");
        query.appendOrderBy(sortKey, SORT_COLUMNS, "categoryName", ascending)
                .appendLimitOffset(pageSize, pageNumber);
        ArrayList<HashMap<String, Object>> rows =
                queryRows(query.getSql(), query.getParameters().toArray());
        ArrayList<VehicleCategory> categories = new ArrayList<VehicleCategory>();
        for (HashMap<String, Object> row : rows) {
            categories.add(toCategory(row));
        }
        return categories;
    }

    public long countAll() throws LfrlmsException {
        return queryCount("SELECT COUNT(*) AS row_count FROM vehicle_category");
    }

    public boolean update(VehicleCategory category) throws LfrlmsException {
        String sql = "UPDATE vehicle_category SET category_name = ?, description = ? "
                + "WHERE category_id = ?";
        return executeUpdate(sql, category.getCategoryName(), category.getDescription(),
                Integer.valueOf(category.getCategoryId())) > 0;
    }

    public boolean delete(int categoryId) throws LfrlmsException {
        return executeUpdate("DELETE FROM vehicle_category WHERE category_id = ?",
                Integer.valueOf(categoryId)) > 0;
    }

    private VehicleCategory toCategory(HashMap<String, Object> row) {
        if (row == null) {
            return null;
        }
        return new VehicleCategory(intValue(row, "category_id"),
                stringValue(row, "category_name"), stringValue(row, "description"));
    }

    private static Map<String, String> createSortColumns() {
        HashMap<String, String> columns = new HashMap<String, String>();
        columns.put("categoryname", "category_name");
        columns.put("id", "category_id");
        return columns;
    }
}
