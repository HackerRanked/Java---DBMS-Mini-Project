# Lease timing policy

- Early lease starts are allowed only when the service rechecks availability from the earlier of now and the scheduled start through the day after the inclusive end date, excluding the lease being started.
- Early lease completion is allowed. Do not add a date restriction that prevents it.

## Thread concepts

| Concept | Class and location | How it is used |
|---|---|---|
| `extends Thread` | `lfrlms.gui.ClockThread` (`src/lfrlms/gui/ClockThread.java`), `run()` | Updates the status clock on a dedicated thread. |
| `Runnable` | `lfrlms.gui.AlertMonitor` (`src/lfrlms/gui/AlertMonitor.java`), `run()` | Runs periodic alert queries in a `Thread` created by `lfrlms.gui.MainFrame` (`src/lfrlms/gui/MainFrame.java`). |
| Background workers | `lfrlms.gui.BackgroundTask` (`src/lfrlms/gui/BackgroundTask.java`), `execute()` | Creates a daemon `Thread` for a service call and posts its completion to Swing's event thread. |
| `synchronized` | `lfrlms.util.QueryLog` (`src/lfrlms/util/QueryLog.java`), `add()` | Protects the shared query history while DAOs log from different threads. |
| `volatile` | `lfrlms.gui.ClockThread.running`, `lfrlms.gui.AlertMonitor.running` | Makes each stop request visible to its worker thread. |
| `sleep` | `lfrlms.gui.ClockThread.run()`, `lfrlms.gui.AlertMonitor.run()` | Waits between clock updates and alert polls; the monitor sleeps only after service/DAO calls return. |
| `interrupt` | `lfrlms.gui.ClockThread.requestStop()` (`src/lfrlms/gui/ClockThread.java`); `lfrlms.gui.MainFrame.stopBackgroundThreads()` and its Retry connection handler (`src/lfrlms/gui/MainFrame.java`) | On shutdown, `lfrlms.gui.ClockThread.requestStop()` interrupts the clock. `lfrlms.gui.MainFrame.stopBackgroundThreads()` calls `lfrlms.gui.AlertMonitor.requestStop()` (`src/lfrlms/gui/AlertMonitor.java`) to clear its running flag, then interrupts `lfrlms.gui.MainFrame.alertMonitorThread`; the Retry connection handler also interrupts that alert thread to trigger an immediate poll. |
| `join` | `lfrlms.gui.MainFrame.joinBriefly()` | Waits up to 500 ms for each daemon thread during window shutdown. |
