package lfrlms.gui;

import java.awt.Component;
import java.awt.Cursor;
import javax.swing.SwingUtilities;

/**
 * Purpose: Runs one service operation away from Swing's event thread.
 * Concept: Thread, inner classes and safe Event Dispatch Thread updates.
 */
public class BackgroundTask<T> {
    public interface Work<T> {
        T run() throws Exception;
    }

    public interface Completion<T> {
        void completed(T result, Exception failure);
    }

    private int requestNumber;

    public void execute(final Component waitTarget, final Work<T> work,
            final Completion<T> completion) {
        final int requestId = nextRequest();
        if (waitTarget != null) {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    waitTarget.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                }
            });
        }
        Thread worker = new Thread(new Runnable() {
            @Override
            public void run() {
                T result = null;
                Exception failure = null;
                try {
                    result = work.run();
                } catch (Exception exception) {
                    failure = exception;
                }
                final T completedResult = result;
                final Exception completedFailure = failure;
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        if (!isCurrent(requestId)) {
                            return;
                        }
                        if (waitTarget != null) {
                            waitTarget.setCursor(Cursor.getDefaultCursor());
                        }
                        completion.completed(completedResult, completedFailure);
                    }
                });
            }
        }, "lfrlms-background-" + requestId);
        worker.setDaemon(true);
        worker.start();
    }

    public synchronized void ignorePendingResults() {
        requestNumber++;
    }

    private synchronized int nextRequest() {
        requestNumber++;
        return requestNumber;
    }

    private synchronized boolean isCurrent(int requestId) {
        return requestNumber == requestId;
    }
}
