package pharmacy;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.KeyEvent;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;

/** "My Account": shows the logged-in user and lets them change their own password. */
public class AccountPanel extends JPanel {
    private final PharmacyService service;
    private final User user;
    private final JPasswordField txtOld = Theme.passwordField(20);
    private final JPasswordField txtNew = Theme.passwordField(20);
    private final JPasswordField txtConfirm = Theme.passwordField(20);

    public AccountPanel(PharmacyService service, User user) {
        this.service = service;
        this.user = user;
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        setBackground(Theme.BACKGROUND);

        JPanel card = Theme.card("My Account - Change Password");
        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(6, 6, 6, 12);

        int row = 0;
        row = info(form, c, row, "Full name", user.getFullName());
        row = info(form, c, row, "Username", user.getUsername());
        row = info(form, c, row, "Access level", user.getRole());
        txtOld.setToolTipText("Your current password");
        txtNew.setToolTipText("At least 6 characters with a letter and a number");
        txtConfirm.setToolTipText("Re-type the new password");
        row = field(form, c, row, "Current password *", txtOld);
        row = field(form, c, row, "New password *", txtNew);
        row = field(form, c, row, "Confirm new password *", txtConfirm);

        JButton btn = Theme.button("Change Password", Theme.PRIMARY, KeyEvent.VK_P, "Save the new password (Alt+P)");
        btn.addActionListener(e -> change());
        c.gridx = 1;
        c.gridy = row;
        form.add(btn, c);

        card.add(form, BorderLayout.NORTH);
        add(card, BorderLayout.CENTER);
    }

    private int info(JPanel p, GridBagConstraints c, int row, String label, String value) {
        c.gridx = 0;
        c.gridy = row;
        p.add(Theme.label(label), c);
        c.gridx = 1;
        p.add(new JLabel(value), c);
        return row + 1;
    }

    private int field(JPanel p, GridBagConstraints c, int row, String label, JPasswordField f) {
        c.gridx = 0;
        c.gridy = row;
        p.add(Theme.label(label), c);
        c.gridx = 1;
        p.add(f, c);
        return row + 1;
    }

    private void change() {
        try {
            service.changePassword(user, new String(txtOld.getPassword()), new String(txtNew.getPassword()),
                    new String(txtConfirm.getPassword()));
            txtOld.setText("");
            txtNew.setText("");
            txtConfirm.setText("");
            Theme.info(this, "Password changed successfully.");
        } catch (PharmacyException ex) {
            Theme.error(this, ex.getMessage());
        }
    }
}
