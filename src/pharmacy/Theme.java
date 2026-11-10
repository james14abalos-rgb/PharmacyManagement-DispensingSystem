package pharmacy;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

/** Shared look-and-feel (java.awt Color / Font) and small UI helpers so every screen is consistent. */
public final class Theme {
    public static final Color PRIMARY = new Color(0x0F766E);
    public static final Color PRIMARY_DARK = new Color(0x115E59);
    public static final Color BACKGROUND = new Color(0xF0FDFA);
    public static final Color SUCCESS = new Color(0x15803D);
    public static final Color DANGER = new Color(0xB91C1C);
    public static final Color NEUTRAL = new Color(0x475569);
    public static final Font FONT = new Font("Segoe UI", Font.PLAIN, 13);
    public static final Font FONT_BOLD = new Font("Segoe UI", Font.BOLD, 13);
    public static final Font FONT_TITLE = new Font("Segoe UI", Font.BOLD, 24);

    private Theme() { }

    public static String peso(double v) {
        return String.format("PHP %,.2f", v);
    }

    // ---------- components ----------
    public static JButton button(String text, Color bg, int mnemonic, String tooltip) {
        // The button paints its own coloured background so it looks the same in every Look and Feel.
        JButton b = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                Color c = getBackground();
                if (!isEnabled()) c = new Color(0x94A3B8);
                else if (getModel().isPressed()) c = c.darker();
                else if (getModel().isRollover()) c = c.brighter();
                g2.setColor(c);
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.dispose();
                super.paintComponent(g);
            }
        };
        b.setFont(FONT_BOLD);
        b.setForeground(Color.WHITE);
        b.setBackground(bg);
        b.setOpaque(false);
        b.setContentAreaFilled(false);
        b.setBorderPainted(false);
        b.setRolloverEnabled(true);
        b.setFocusPainted(false);
        b.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        if (mnemonic != 0) b.setMnemonic(mnemonic);
        b.setToolTipText(tooltip);
        return b;
    }

    private static javax.swing.border.Border inputBorder() {
        return BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(new Color(0xCBD5E1)),
                BorderFactory.createEmptyBorder(5, 8, 5, 8));
    }

    public static JTextField field(int cols) {
        JTextField f = new JTextField(cols);
        f.setFont(FONT);
        f.setBorder(inputBorder());
        return f;
    }

    public static JPasswordField passwordField(int cols) {
        JPasswordField f = new JPasswordField(cols);
        f.setFont(FONT);
        f.setBorder(inputBorder());
        return f;
    }

    public static JLabel label(String text) {
        JLabel l = new JLabel(text);
        l.setFont(FONT_BOLD);
        l.setForeground(NEUTRAL);
        return l;
    }

    public static JPanel card(String title) {
        JPanel p = new JPanel(new BorderLayout(8, 8));
        p.setBackground(Color.WHITE);
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(BorderFactory.createLineBorder(PRIMARY), " " + title + " ",
                        javax.swing.border.TitledBorder.LEFT, javax.swing.border.TitledBorder.TOP, FONT_BOLD, PRIMARY_DARK),
                BorderFactory.createEmptyBorder(6, 8, 8, 8)));
        return p;
    }

    public static void onChange(JTextField f, Runnable r) {
        f.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { r.run(); }
            @Override public void removeUpdate(DocumentEvent e) { r.run(); }
            @Override public void changedUpdate(DocumentEvent e) { r.run(); }
        });
    }

    // ---------- tables ----------
    public static DefaultTableModel model(String... columns) {
        return new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
            @Override public Class<?> getColumnClass(int c) {
                return getRowCount() > 0 && getValueAt(0, c) != null ? getValueAt(0, c).getClass() : Object.class;
            }
        };
    }

    public static JTable table(DefaultTableModel m) {
        JTable t = new JTable(m);
        t.setFont(FONT);
        t.setRowHeight(26);
        t.setAutoCreateRowSorter(true);
        t.setSelectionBackground(new Color(0x99F6E4));
        t.setSelectionForeground(Color.BLACK);
        t.setGridColor(new Color(0xE2E8F0));
        t.getTableHeader().setReorderingAllowed(false);
        t.getTableHeader().setDefaultRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object v, boolean s, boolean f, int r, int c) {
                super.getTableCellRendererComponent(table, v, s, f, r, c);
                setBackground(PRIMARY);
                setForeground(Color.WHITE);
                setFont(FONT_BOLD);
                setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
                return this;
            }
        });
        return t;
    }

    public static JScrollPane scroll(JTable t) {
        JScrollPane sp = new JScrollPane(t);
        sp.setBorder(BorderFactory.createLineBorder(new Color(0xCBD5E1)));
        sp.getViewport().setBackground(Color.WHITE);
        return sp;
    }

    public static void widths(JTable t, int... w) {
        for (int i = 0; i < w.length && i < t.getColumnCount(); i++) {
            t.getColumnModel().getColumn(i).setPreferredWidth(w[i]);
        }
    }

    /** Colours rows by the text in the status column (expired / near expiry / low stock). */
    public static void colorByStatus(JTable t, int statusCol) {
        DefaultTableCellRenderer r = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean sel, boolean focus,
                                                           int row, int col) {
                Component c = super.getTableCellRendererComponent(table, value, sel, focus, row, col);
                if (!sel) {
                    String s = String.valueOf(table.getValueAt(row, statusCol));
                    Color bg = Color.WHITE;
                    if (s.equals("EXPIRED") || s.equals("OUT OF STOCK")) bg = new Color(0xFEE2E2);
                    else if (s.equals("NEAR EXPIRY")) bg = new Color(0xFEF3C7);
                    else if (s.equals("LOW STOCK")) bg = new Color(0xFFEDD5);
                    c.setBackground(bg);
                    c.setForeground(Color.BLACK);
                }
                return c;
            }
        };
        t.setDefaultRenderer(Object.class, r);
        t.setDefaultRenderer(Integer.class, r);
    }

    // ---------- dialogs ----------
    public static void error(Component p, String m) { JOptionPane.showMessageDialog(p, m, "Error", JOptionPane.ERROR_MESSAGE); }
    public static void warn(Component p, String m) { JOptionPane.showMessageDialog(p, m, "Notice", JOptionPane.WARNING_MESSAGE); }
    public static void info(Component p, String m) { JOptionPane.showMessageDialog(p, m, "PharmaCare", JOptionPane.INFORMATION_MESSAGE); }

    public static boolean confirm(Component p, String m) {
        return JOptionPane.showConfirmDialog(p, m, "Please Confirm", JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION;
    }

    /** Asks for the admin password; returns null if cancelled. */
    public static String askPassword(Component p, String message) {
        JPasswordField pf = passwordField(18);
        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.add(new JLabel(message), BorderLayout.NORTH);
        panel.add(pf, BorderLayout.CENTER);
        int r = JOptionPane.showConfirmDialog(p, panel, "Administrator Authorization",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        return r == JOptionPane.OK_OPTION ? new String(pf.getPassword()) : null;
    }
}
