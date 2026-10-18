package lfrlms.service;

import java.util.ArrayList;
import java.util.HashMap;
import lfrlms.dao.InsightsDao;
import lfrlms.exception.LfrlmsException;
import lfrlms.util.QueryLog;

/**
 * Purpose: Provides read-only schema and query diagnostics to the GUI.
 * Concept: Service and DAO separation.
 */
public class InsightsService {
    private final InsightsDao insightsDao;

    public InsightsService() {
        this(new InsightsDao());
    }

    public InsightsService(InsightsDao insightsDao) {
        this.insightsDao = insightsDao;
    }

    public ArrayList<String> listTableNames() {
        return insightsDao.listTableNames();
    }

    public ArrayList<QueryLog.QueryEntry> recentQueries() {
        return insightsDao.recentQueries();
    }

    public ArrayList<HashMap<String, Object>> explain(QueryLog.QueryEntry entry)
            throws LfrlmsException {
        return insightsDao.explain(entry);
    }

    public String showCreateTable(String tableName) throws LfrlmsException {
        return insightsDao.showCreateTable(tableName);
    }

    public ArrayList<HashMap<String, Object>> listViews() throws LfrlmsException {
        return insightsDao.listViews();
    }

    public ArrayList<HashMap<String, Object>> listFunctions() throws LfrlmsException {
        return insightsDao.listRoutines("FUNCTION");
    }

    public ArrayList<HashMap<String, Object>> listProcedures() throws LfrlmsException {
        return insightsDao.listRoutines("PROCEDURE");
    }

    public ArrayList<HashMap<String, Object>> listTriggers() throws LfrlmsException {
        return insightsDao.listTriggers();
    }

    public ArrayList<FeatureExample> featureChecklist() {
        ArrayList<FeatureExample> features = new ArrayList<FeatureExample>();
        features.add(new FeatureExample("Constraints",
                "ck_payment_exactly_one_parent; ck_rental_actual_return"));
        features.add(new FeatureExample("Indexes",
                "ix_rental_vehicle_dates; ix_vehicle_status; uq_vehicle_registration"));
        features.add(new FeatureExample("Views",
                "vw_available_fleet; vw_active_leases; vw_rental_balance"));
        features.add(new FeatureExample("Functions",
                "fn_rental_charge; fn_lease_total; fn_customer_outstanding"));
        features.add(new FeatureExample("Procedures",
                "sp_available_vehicles; sp_complete_rental"));
        features.add(new FeatureExample("Triggers",
                "trg_rental_after_insert; trg_rental_after_update"));
        features.add(new FeatureExample("Transactions",
                "RentalService.createReservation; LeaseService.startLease"));
        features.add(new FeatureExample("Date-overlap availability",
                "sp_available_vehicles; vw_available_fleet; VehicleDao.findAvailableVehicles"));
        return features;
    }

    /** Keeps each SQL feature paired with a viva-friendly object example. */
    public static class FeatureExample {
        private final String feature;
        private final String example;

        public FeatureExample(String feature, String example) {
            this.feature = feature;
            this.example = example;
        }

        public String getFeature() {
            return feature;
        }

        public String getExample() {
            return example;
        }
    }
}
