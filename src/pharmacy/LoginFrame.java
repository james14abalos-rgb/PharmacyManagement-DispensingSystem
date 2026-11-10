package pharmacy;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.KeyEvent;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

/** Login screen. 3 wrong attempts -> the account is BLOCKED and the program terminates. */
public class LoginFrame extends JFrame {
    private static final int MAX_ATTEMPTS = 3;

    private final PharmacyService service;
    private final JTextField txtUser = Theme.field(18);
    private final JPasswordField txtPass = Theme.passwordField(18);
    private final JCheckBox chkShow = new JCheckBox("Show password");
    private final JLabel lblAttempts = new JLabel(" ");
    private int failedAttempts = 0;

    public LoginFrame(PharmacyService service) {
        this.service = service;
        setTitle("PharmaCare - Login");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setResizable(false);
        buildUi();
        pack();
        setLocationRelativeTo(null);
    }

    private void buildUi() {
        JPanel root = new JPanel(new BorderLayout());

        // ---- brand panel (left) ----
        JPanel brand = new JPanel(new GridBagLayout());
        brand.setBackground(Theme.PRIMARY);
        brand.setPreferredSize(new Dimension(290, 400));
        JLabel title = new JLabel("PharmaCare");
        title.setFont(Theme.FONT_TITLE.deriveFont(34f));
        title.setForeground(Color.WHITE);
        JLabel sub = new JLabel("<html><div style='text-align:center'>Pharmacy Management<br>&amp; Dispensing System</div></html>");
        sub.setFont(Theme.FONT.deriveFont(15f));
        sub.setForeground(new Color(0xCCFBF1));
        GridBagConstraints g = new GridBagConstraints();
        g.gridx = 0;
        g.gridy = 0;
        g.insets = new Insets(4, 4, 4, 4);
        brand.add(title, g);
        g.gridy = 1;
        brand.add(sub, g);

        // ---- form panel (right) ----
        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(Color.WHITE);
        form.setBorder(BorderFactory.createEmptyBorder(30, 36, 30, 36));
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(4, 0, 4, 0);

        JLabel head = new JLabel("Sign in to continue");
        head.setFont(Theme.FONT_TITLE.deriveFont(20f));
        head.setForeground(Theme.PRIMARY_DARK);
        form.add(head, c);

        c.gridy++;
        form.add(Theme.label("Username *"), c);
        txtUser.setToolTipText("Enter your username");
        c.gridy++;
        form.add(txtUser, c);

        c.gridy++;
        form.add(Theme.label("Password *"), c);
        txtPass.setToolTipText("Enter your password");
        c.gridy++;
        form.add(txtPass, c);

        final char defaultEcho = txtPass.getEchoChar();
        chkShow.setOpaque(false);
        chkShow.setFont(Theme.FONT);
        chkShow.setMnemonic(KeyEvent.VK_W);
        chkShow.setToolTipText("Show or hide the password (Alt+W)");
        chkShow.addActionListener(e -> txtPass.setEchoChar(chkShow.isSelected() ? (char) 0 : defaultEcho));
        c.gridy++;
        form.add(chkShow, c);

        lblAttempts.setFont(Theme.FONT);
        lblAttempts.setForeground(Theme.DANGER);
        c.gridy++;
        form.add(lblAttempts, c);

        JButton btnLogin = Theme.button("Login", Theme.PRIMARY, KeyEvent.VK_L, "Log in (Alt+L or Enter)");
        JButton btnSignUp = Theme.button("Staff Sign Up", Theme.NEUTRAL, KeyEvent.VK_S, "Create a new pharmacy staff account (Alt+S)");
        btnLogin.addActionListener(e -> doLogin());
        btnSignUp.addActionListener(e -> new SignUpDialog(this, service, false).setVisible(true));
        JPanel buttons = new JPanel(new java.awt.GridLayout(1, 2, 10, 0));
        buttons.setOpaque(false);
        buttons.add(btnLogin);
        buttons.add(btnSignUp);
        c.gridy++;
        c.insets = new Insets(10, 0, 0, 0);
        form.add(buttons, c);
        JButton btnCustomer = Theme.button("I'm a Customer - Shop Without an Account", Theme.SUCCESS, KeyEvent.VK_C,
                "No sign-up needed. Browse and buy over-the-counter medicines (Alt+C)");
        btnCustomer.addActionListener(e -> {
            dispose();
            new MainFrame(service, new Customer()).setVisible(true);
        });
        c.gridy++;
        c.insets = new Insets(14, 0, 0, 0);
        form.add(btnCustomer, c);
        getRootPane().setDefaultButton(btnLogin);

        root.add(brand, BorderLayout.WEST);
        root.add(form, BorderLayout.CENTER);
        setContentPane(root);
    }

    private void doLogin() {
        String username = txtUser.getText().trim();
        String password = new String(txtPass.getPassword());
        if (username.isEmpty() || password.isEmpty()) {
            Theme.warn(this, "Please fill in both the username and the password.");
            return;
        }
        User user = service.findUser(username);
        if (user != null && user.isBlocked()) {
            Theme.error(this, "This account is BLOCKED.\nPlease ask the system administrator to unblock it.");
            return;
        }
        if (user == null || !user.checkPassword(password)) {
            handleFailedLogin(user);
            return;
        }
        failedAttempts = 0;
        dispose();
        new MainFrame(service, user).setVisible(true);
    }

    private void handleFailedLogin(User user) {
        failedAttempts++;
        txtPass.setText("");
        int left = MAX_ATTEMPTS - failedAttempts;
        if (left > 0) {
            lblAttempts.setText("Invalid login. Attempts remaining: " + left);
            Theme.error(this, "Invalid username or password.\nAttempts remaining: " + left);
            return;
        }
        String msg = "Maximum login attempts reached.\n";
        if (user != null && !user.canManageUsers()) {          // admins can never be locked out
            try {
                service.setBlocked(user, true);
                msg += "The account '" + user.getUsername() + "' has been BLOCKED.\n"
                        + "Only the system administrator can unblock it.\n";
            } catch (PharmacyException ex) {
                msg += ex.getMessage() + "\n";
            }
        }
        JOptionPane.showMessageDialog(this, msg + "The system will now close.", "Access Denied",
                JOptionPane.ERROR_MESSAGE);
        System.exit(0);
    }
}
