package pharmacy;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.KeyEvent;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;

/** Admin-only: add users with an access level, unblock/block accounts, delete users (needs admin password). */
public class UsersPanel extends JPanel implements Refreshable {
    private final PharmacyService service;
    private final User admin;
    private final DefaultTableModel model = Theme.model("ID", "Username", "Full Name", "Access Level", "Status");
    private final JTable table = Theme.table(model);

    public UsersPanel(PharmacyService service, User admin) {
        this.service = service;
        this.admin = admin;
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        setBackground(Theme.BACKGROUND);

        JPanel card = Theme.card("System Users");
        card.add(Theme.scroll(table), BorderLayout.CENTER);
        add(card, BorderLayout.CENTER);

        JButton add = Theme.button("Add User", Theme.PRIMARY, KeyEvent.VK_A, "Create a new admin or staff account (Alt+A)");
        JButton toggle = Theme.button("Block / Unblock", Theme.NEUTRAL, KeyEvent.VK_B, "Block or unblock the selected account (Alt+B)");
        JButton del = Theme.button("Delete User", Theme.DANGER, KeyEvent.VK_D, "Delete the selected account - needs admin password (Alt+D)");
        add.addActionListener(e -> {
            new SignUpDialog(SwingUtilities.getWindowAncestor(this), service, true).setVisible(true);
            refresh();
        });
        toggle.addActionListener(e -> toggleBlock());
        del.addActionListener(e -> deleteSelected());

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        btns.setOpaque(false);
        btns.add(add);
        btns.add(toggle);
        btns.add(del);
        add(btns, BorderLayout.SOUTH);
        refresh();
    }

    @Override
    public void refresh() {
        model.setRowCount(0);
        for (User u : service.getUsers()) {
            model.addRow(new Object[]{u.getId(), u.getUsername(), u.getFullName(), u.getRole(),
                u.isBlocked() ? "BLOCKED" : "ACTIVE"});
        }
    }

    private User selected() {
        int row = table.getSelectedRow();
        if (row < 0) return null;
        return service.findUserById((String) model.getValueAt(table.convertRowIndexToModel(row), 0));
    }

    private void toggleBlock() {
        User u = selected();
        if (u == null) {
            Theme.warn(this, "Select a user first.");
            return;
        }
        try {
            service.setBlocked(u, !u.isBlocked());
            refresh();
            Theme.info(this, "Account '" + u.getUsername() + "' is now " + (u.isBlocked() ? "BLOCKED." : "ACTIVE."));
        } catch (PharmacyException ex) {
            Theme.error(this, ex.getMessage());
        }
    }

    private void deleteSelected() {
        User u = selected();
        if (u == null) {
            Theme.warn(this, "Select a user to delete.");
            return;
        }
        if (!Theme.confirm(this, "Delete the account '" + u.getUsername() + "'?")) return;
        String pw = Theme.askPassword(this, "Enter your ADMIN password to confirm:");
        if (pw == null) return;
        try {
            service.deleteUser(u.getId(), admin, pw);
            refresh();
            Theme.info(this, "User deleted.");
        } catch (PharmacyException ex) {
            Theme.error(this, ex.getMessage());
        }
    }
}
