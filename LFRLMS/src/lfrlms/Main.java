package lfrlms;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import com.formdev.flatlaf.FlatLightLaf;
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
            if (FlatLightLaf.setup()) {
                startWindow();
                return;
            }
            AppLogger.logException(Main.class.getName(),
                    "FlatLaf setup returned false; using the system look and feel.",
                    new IllegalStateException("FlatLaf setup returned false."));
        } catch (Exception exception) {
            AppLogger.logException(Main.class.getName(),
                    "FlatLaf could not be loaded; using the system look and feel.", exception);
        } catch (LinkageError error) {
            AppLogger.logException(Main.class.getName(),
                    "FlatLaf is unavailable; using the system look and feel.", error);
        }
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception exception) {
            AppLogger.logException(Main.class.getName(),
                    "The system look and feel could not be loaded.", exception);
        }
        startWindow();
    }

    private static void startWindow() {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                MainFrame frame = new MainFrame();
                frame.setVisible(true);
            }
        });
    }
}
