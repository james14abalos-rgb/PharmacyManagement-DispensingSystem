package pharmacy;

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.KeyEvent;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

/**
 * Account registration form with password verification.
 * adminMode = false: public sign-up (always creates STAFF).
 * adminMode = true : opened by an admin, can choose the level of access (STAFF / ADMIN).
 */
public class SignUpDialog extends JDialog {
    private final PharmacyService service;
    private final boolean adminMode;
    private final JTextField txtName = Theme.field(20);
    private final JTextField txtUser = Theme.field(20);
    private final JPasswordField txtPass = Theme.passwordField(20);
    private final JPasswordField txtConfirm = Theme.passwordField(20);
    private final JComboBox<String> cbRole = new JComboBox<>(new String[]{"STAFF", "ADMIN"});

    public SignUpDialog(Window owner, PharmacyService service, boolean adminMode) {
        super(owner, adminMode ? "Add System User" : "Create Staff Account", ModalityType.APPLICATION_MODAL);
        this.service = service;
        this.adminMode = adminMode;
        buildUi();
        pack();
        setResizable(false);
        setLocationRelativeTo(owner);
    }

    private void buildUi() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBackground(Color.WHITE);
        p.setBorder(BorderFactory.createEmptyBorder(20, 28, 20, 28));
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 2;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(0, 0, 10, 0);
        JLabel head = new JLabel(adminMode ? "Add a new system user" : "Register a new staff account");
        head.setFont(Theme.FONT_TITLE.deriveFont(18f));
        head.setForeground(Theme.PRIMARY_DARK);
        p.add(head, c);
        c.gridwidth = 1;

        int row = 1;
        row = addRow(p, row, "Full name *", txtName, "Your complete name");
        row = addRow(p, row, "Username *", txtUser, "4-15 characters: letters, numbers, underscore");
        row = addRow(p, row, "Password *", txtPass, "At least 6 characters with a letter and a number");
        row = addRow(p, row, "Confirm password *", txtConfirm, "Re-type the password to verify it");
        if (adminMode) {
            cbRole.setFont(Theme.FONT);
            row = addRow(p, row, "Access level *", cbRole, "ADMIN = full access, STAFF = dispensing only");
        }

        JButton btnSave = Theme.button("Register", Theme.PRIMARY, KeyEvent.VK_R, "Create the account (Alt+R)");
        JButton btnCancel = Theme.button("Cancel", Theme.NEUTRAL, KeyEvent.VK_C, "Close without saving (Alt+C)");
        btnSave.addActionListener(e -> register());
        btnCancel.addActionListener(e -> dispose());
        JPanel btns = new JPanel(new java.awt.GridLayout(1, 2, 10, 0));
        btns.setOpaque(false);
        btns.add(btnSave);
        btns.add(btnCancel);
        c.gridx = 0;
        c.gridy = row;
        c.gridwidth = 2;
        c.insets = new Insets(14, 0, 0, 0);
        p.add(btns, c);
        getRootPane().setDefaultButton(btnSave);
        setContentPane(p);
    }

    private int addRow(JPanel p, int row, String label, JComponent comp, String tip) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridy = row;
        c.insets = new Insets(4, 0, 4, 12);
        c.anchor = GridBagConstraints.WEST;
        c.gridx = 0;
        p.add(Theme.label(label), c);
        c.gridx = 1;
        c.insets = new Insets(4, 0, 4, 0);
        c.fill = GridBagConstraints.HORIZONTAL;
        comp.setToolTipText(tip);
        p.add(comp, c);
        return row + 1;
    }

    private void register() {
        try {
            String role = adminMode ? (String) cbRole.getSelectedItem() : "STAFF";
            service.registerUser(role, txtName.getText(), txtUser.getText(),
                    new String(txtPass.getPassword()), new String(txtConfirm.getPassword()));
            Theme.info(this, "Account created successfully!");
            dispose();
        } catch (PharmacyException ex) {
            Theme.error(this, ex.getMessage());
        }
    }
}
