package pharmacy;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/** Entry point. Run this class in NetBeans (Shift+F6). */
public class Main {
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) { /* fall back to default look and feel */ }

        SwingUtilities.invokeLater(() -> {
            try {
                PharmacyService service = new PharmacyService();
                new LoginFrame(service).setVisible(true);
            } catch (PharmacyException e) {
                JOptionPane.showMessageDialog(null, "The system could not start:\n" + e.getMessage(),
                        "Startup Error", JOptionPane.ERROR_MESSAGE);
                System.exit(1);
            }
        });
    }
}
