# Lease timing policy

- Early lease starts are allowed only when the service rechecks availability from the earlier of now and the scheduled start through the day after the inclusive end date, excluding the lease being started.
- Early lease completion is allowed. Do not add a date restriction that prevents it.

## Thread concepts

| Concept | Class and location | How it is used |
|---|---|---|
| `extends Thread` | `gui.ClockThread`, `run()` | Updates the status clock on a dedicated thread. |
| `Runnable` | `gui.AlertMonitor`, `run()` | Runs periodic alert queries in a `Thread` created by `gui.MainFrame`. |
| `synchronized` | `util.QueryLog`, `add()` | Protects the shared query history while DAOs log from different threads. |
| `volatile` | `gui.ClockThread.running`, `gui.AlertMonitor.running` | Makes each stop request visible to its worker thread. |
| `sleep` | `gui.ClockThread.run()`, `gui.AlertMonitor.run()` | Waits between clock updates and alert polls; the monitor sleeps only after service/DAO calls return. |
| `interrupt` | `gui.ClockThread.requestStop()`, `gui.MainFrame.stopBackgroundThreads()` | Wakes sleeping workers so they can stop promptly. The Retry button also interrupts the alert thread to request an immediate poll. |
| `join` | `gui.MainFrame.joinBriefly()` | Waits up to 500 ms for each daemon thread during window shutdown. |
