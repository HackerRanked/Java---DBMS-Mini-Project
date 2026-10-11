package lfrlms.util;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;

/**
 * Purpose: Keeps a bounded history of SQL activity for diagnostics.
 * Concept: Queue collections, a private nested class and synchronized methods.
 */
public class QueryLog {
    private static final int MAX_ENTRIES = 200;
    private final Queue<QueryRecord> records;

    public QueryLog() {
        records = new LinkedList<QueryRecord>();
    }

    public synchronized void add(String sql, String parameters,
            long elapsedMillis, int rowCount) {
        if (records.size() == MAX_ENTRIES) {
            records.remove();
        }
        records.add(new QueryRecord(LocalDateTime.now(), sql, parameters,
                elapsedMillis, rowCount));
    }

    public synchronized ArrayList<String> getRecentEntries() {
        ArrayList<String> result = new ArrayList<String>();
        for (QueryRecord record : records) {
            result.add(record.toDisplayString());
        }
        return result;
    }

    public synchronized int size() {
        return records.size();
    }

    public synchronized void clear() {
        records.clear();
    }

    /** Purpose: Keeps one immutable SQL activity entry inside the bounded queue. */
    private static class QueryRecord {
        private final LocalDateTime recordedAt;
        private final String sql;
        private final String parameters;
        private final long elapsedMillis;
        private final int rowCount;

        private QueryRecord(LocalDateTime recordedAt, String sql, String parameters,
                long elapsedMillis, int rowCount) {
            this.recordedAt = recordedAt;
            this.sql = sql;
            this.parameters = parameters;
            this.elapsedMillis = elapsedMillis;
            this.rowCount = rowCount;
        }

        private String toDisplayString() {
            return recordedAt + " | " + sql + " | params=" + parameters
                    + " | " + elapsedMillis + " ms | rows=" + rowCount;
        }
    }
}
