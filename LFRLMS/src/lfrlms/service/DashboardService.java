package lfrlms.service;

import java.util.HashMap;
import lfrlms.dao.DashboardDao;
import lfrlms.exception.LfrlmsException;

/** Combines database dashboard totals with the periodic alert counts. */
public class DashboardService {
    private final DashboardDao dashboardDao;
    private final AlertService alertService;

    public DashboardService() {
        this(new DashboardDao(), new AlertService());
    }

    public DashboardService(DashboardDao dashboardDao, AlertService alertService) {
        this.dashboardDao = dashboardDao;
        this.alertService = alertService;
    }

    public HashMap<String, Object> getSummary() throws LfrlmsException {
        HashMap<String, Object> summary = dashboardDao.getSummary();
        if (summary == null) {
            summary = new HashMap<String, Object>();
        }
        HashMap<String, Integer> alerts = alertService.getAlertCounts();
        summary.putAll(alerts);
        return summary;
    }
}
