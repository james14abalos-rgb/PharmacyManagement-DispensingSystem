package pharmacy;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.KeyStroke;

/** Main window shown after login. Admins get extra tabs (Users); staff see a reduced set of features. */
public class MainFrame extends JFrame {
    private final PharmacyService service;
    private final User user;
    private final JTabbedPane tabs = new JTabbedPane();

    public MainFrame(PharmacyService service, User user) {
        this.service = service;
        this.user = user;
        setTitle("PharmaCare - Pharmacy Management & Dispensing System");
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) { exitApp(); }
        });
        setMinimumSize(new Dimension(1050, 650));
        setSize(1280, 740);
        setJMenuBar(buildMenu());
        add(buildHeader(), BorderLayout.NORTH);
        add(buildTabs(), BorderLayout.CENTER);
        setLocationRelativeTo(null);
    }

    private JPanel buildHeader() {
        JPanel h = new JPanel(new BorderLayout());
        h.setBackground(Theme.PRIMARY);
        h.setBorder(BorderFactory.createEmptyBorder(12, 18, 12, 18));
        JLabel title = new JLabel("PharmaCare  |  Pharmacy Management & Dispensing System");
        title.setFont(Theme.FONT_TITLE.deriveFont(20f));
        title.setForeground(Color.WHITE);
        JLabel who = new JLabel(user.canViewRecords() ? "Logged in: " + user.getFullName() + "  [" + user.getRole() + "]"
                : "Welcome, customer! No account needed.");
        who.setFont(Theme.FONT_BOLD);
        who.setForeground(new Color(0xCCFBF1));
        h.add(title, BorderLayout.WEST);
        h.add(who, BorderLayout.EAST);
        return h;
    }

    private JTabbedPane buildTabs() {
        tabs.setFont(Theme.FONT_BOLD);
        boolean pharmacy = user.canViewRecords();           // admin / staff
        addTab(pharmacy ? "Dispensing" : "Buy Medicines", new DispensingPanel(service, user),
                pharmacy ? "Sell / dispense medicines" : "Pick medicines and pay at the counter");
        addTab(pharmacy ? "Inventory" : "Medicine List", new InventoryPanel(service, user),
                pharmacy ? "View or manage stock, batches and expiry" : "Browse available medicines");
        if (pharmacy) {
            addTab("Transactions", new RecordsPanel(service, false), "All sales records");
            addTab("Controlled Log", new RecordsPanel(service, true), "Controlled-substance log");
        }
        if (user.canManageUsers()) {
            addTab("Users", new UsersPanel(service, user), "Manage system users");
        }
        if (pharmacy) {
            addTab("My Account", new AccountPanel(service, user), "Change your password");
        }
        tabs.addChangeListener(e -> {
            Component c = tabs.getSelectedComponent();
            if (c instanceof Refreshable) {
                ((Refreshable) c).refresh();
            }
        });
        return tabs;
    }

    private void addTab(String title, Component content, String tip) {
        int i = tabs.getTabCount();
        tabs.addTab(title, content);
        tabs.setMnemonicAt(i, KeyEvent.VK_1 + i);
        tabs.setToolTipTextAt(i, tip + " (Alt+" + (i + 1) + ")");
    }

    private JMenuBar buildMenu() {
        int mask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();
        JMenuBar bar = new JMenuBar();

        JMenu session = new JMenu("Session");
        session.setMnemonic(KeyEvent.VK_S);
        JMenuItem logout = new JMenuItem(user.canViewRecords() ? "Logout" : "Back to Start Screen");
        logout.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_L, mask));
        logout.addActionListener(e -> logout());
        JMenuItem exit = new JMenuItem("Exit");
        exit.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Q, mask));
        exit.addActionListener(e -> exitApp());
        session.add(logout);
        session.addSeparator();
        session.add(exit);

        JMenu help = new JMenu("Help");
        help.setMnemonic(KeyEvent.VK_H);
        JMenuItem keys = new JMenuItem("Keyboard Shortcuts");
        keys.addActionListener(e -> Theme.info(this,
                "Ctrl+L  Logout / back to start\nCtrl+Q  Exit\nAlt+1..6  Switch tabs\n"
                + "Alt+<underlined letter>  Press a button\nDouble-click a medicine  Add it to the cart"));
        JMenuItem about = new JMenuItem("About");
        about.addActionListener(e -> Theme.info(this,
                "PharmaCare v1.0\nPharmacy Management & Dispensing System\n"
                + "Transaction Processing System - Java Swing + OOP\nData stored in text files (data/ folder)."));
        help.add(keys);
        help.add(about);

        bar.add(session);
        bar.add(help);
        return bar;
    }

    private void logout() {
        if (Theme.confirm(this, user.canViewRecords() ? "Log out of " + user.getUsername() + "?" : "Return to the start screen?")) {
            dispose();
            new LoginFrame(service).setVisible(true);
        }
    }

    private void exitApp() {
        if (Theme.confirm(this, "Exit PharmaCare?")) {
            System.exit(0);
        }
    }
}
