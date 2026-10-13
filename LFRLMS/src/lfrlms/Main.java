package lfrlms;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import lfrlms.gui.MainFrame;
import lfrlms.util.AppLogger;

/**
 * Purpose: Starts the Swing desktop application on its event thread.
 * Concept: Application entry point and safe UI initialization.
 */
public final class Main {
    private Main() {
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception exception) {
            AppLogger.logException(Main.class.getName(),
                    "The system look and feel could not be loaded.", exception);
        }
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                MainFrame frame = new MainFrame();
                frame.setVisible(true);
            }
        });
    }
}
