package pharmacy;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.table.DefaultTableModel;

/** The key transaction screen: pick medicines -> cart -> prescription details -> complete sale. */
public class DispensingPanel extends JPanel implements Refreshable {
    private final PharmacyService service;
    private final User user;

    private final JTextField txtSearch = Theme.field(22);
    private final DefaultTableModel medModel = Theme.model("ID", "Medicine", "Type", "Price", "Stock", "Batch", "Expiry", "Status");
    private final JTable medTable = Theme.table(medModel);
    private final JSpinner spQty = new JSpinner(new SpinnerNumberModel(1, 1, 999, 1));

    private final DefaultTableModel cartModel = Theme.model("Medicine", "Batch", "Expiry", "Qty", "Unit Price", "Subtotal");
    private final JTable cartTable = Theme.table(cartModel);
    private final JTextField txtRx = Theme.field(14);
    private final JTextField txtPatient = Theme.field(14);
    private final JTextField txtPrescriber = Theme.field(14);
    private final JLabel lblTotal = new JLabel("Total: PHP 0.00");
    private final List<CartItem> cart = new ArrayList<>();

    public DispensingPanel(PharmacyService service, User user) {
        this.service = service;
        this.user = user;
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        setBackground(Theme.BACKGROUND);

        add(buildSearchBar(), BorderLayout.NORTH);
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, buildMedicinePanel(), buildCartPanel());
        split.setResizeWeight(0.58);
        split.setDividerLocation(700);
        split.setBorder(null);
        split.setOpaque(false);
        add(split, BorderLayout.CENTER);

        Theme.colorByStatus(medTable, 7);
        Theme.widths(medTable, 70, 200, 90, 80, 50, 85, 90, 100);
        Theme.widths(cartTable, 170, 80, 130, 40, 80, 85);
        loadMedicines();
        refreshCart();
    }

    private JPanel buildSearchBar() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        p.setOpaque(false);
        p.add(Theme.label("Search medicine:"));
        txtSearch.setToolTipText("Type a name, ID or type (OTC / RX / CONTROLLED)");
        Theme.onChange(txtSearch, this::loadMedicines);
        p.add(txtSearch);
        JLabel hint = new JLabel(user.canDispenseRx() ? "Tip: double-click a medicine to add it to the cart.  Red = expired, yellow = near expiry, orange = low stock." : "Double-click a medicine to add it to your cart. RX / CONTROLLED items are given by our pharmacist.");
        hint.setFont(Theme.FONT.deriveFont(Font.ITALIC));
        p.add(hint);
        return p;
    }

    private JPanel buildMedicinePanel() {
        JPanel card = Theme.card("1. Select Medicine");
        card.add(Theme.scroll(medTable), BorderLayout.CENTER);
        medTable.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) addToCart();
            }
        });

        JPanel south = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        south.setOpaque(false);
        south.add(Theme.label("Quantity:"));
        spQty.setFont(Theme.FONT);
        spQty.setPreferredSize(new Dimension(70, 30));
        spQty.setToolTipText("Number of units to dispense");
        south.add(spQty);
        JButton btnAdd = Theme.button("Add to Cart", Theme.PRIMARY, KeyEvent.VK_A, "Add the selected medicine to the cart (Alt+A)");
        btnAdd.addActionListener(e -> addToCart());
        south.add(btnAdd);
        card.add(south, BorderLayout.SOUTH);
        return card;
    }

    private JPanel buildCartPanel() {
        JPanel card = Theme.card(user.canDispenseRx() ? "2. Cart & Prescription Details" : "2. Your Cart");
        card.add(Theme.scroll(cartTable), BorderLayout.CENTER);

        JPanel form = new JPanel(new GridLayout(0, 2, 8, 6));
        form.setOpaque(false);
        txtRx.setToolTipText("Required for RX / CONTROLLED medicines. Format: RX-0001");
        txtPatient.setToolTipText("Required for prescription medicines (optional for OTC)");
        txtPrescriber.setToolTipText("Doctor's name - required for controlled substances");
        form.add(Theme.label("Prescription ID (RX-0001)"));
        form.add(txtRx);
        form.add(Theme.label("Patient name"));
        form.add(txtPatient);
        form.add(Theme.label("Prescriber / Doctor"));
        form.add(txtPrescriber);
        form.setVisible(user.canDispenseRx());   // customers never enter prescription data

        lblTotal.setFont(Theme.FONT_TITLE.deriveFont(22f));
        lblTotal.setForeground(Theme.PRIMARY_DARK);

        JButton btnRemove = Theme.button("Remove Item", Theme.NEUTRAL, KeyEvent.VK_R, "Remove the selected cart line (Alt+R)");
        JButton btnClear = Theme.button("Clear Cart", Theme.DANGER, KeyEvent.VK_C, "Empty the cart (Alt+C)");
        JButton btnDone = Theme.button("Complete Transaction", Theme.SUCCESS, KeyEvent.VK_T, "Validate, record the sale and update stock (Alt+T)");
        btnRemove.addActionListener(e -> removeSelected());
        btnClear.addActionListener(e -> {
            if (!cart.isEmpty() && Theme.confirm(this, "Clear the whole cart?")) clearAll();
        });
        btnDone.addActionListener(e -> completeTransaction());
        JPanel btns = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        btns.setOpaque(false);
        btns.add(btnRemove);
        btns.add(btnClear);
        btns.add(btnDone);

        JPanel south = new JPanel(new BorderLayout(0, 8));
        south.setOpaque(false);
        south.add(form, BorderLayout.NORTH);
        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setOpaque(false);
        bottom.add(lblTotal, BorderLayout.NORTH);
        bottom.add(btns, BorderLayout.SOUTH);
        south.add(bottom, BorderLayout.SOUTH);
        card.add(south, BorderLayout.SOUTH);
        return card;
    }

    // ------------------------------------------------------------------
    @Override
    public void refresh() {
        loadMedicines();
        refreshCart();
    }

    private void loadMedicines() {
        medModel.setRowCount(0);
        String q = txtSearch.getText().trim().toLowerCase();
        for (Medicine m : service.getMedicines()) {
            if (!q.isEmpty() && !(m.getName().toLowerCase().contains(q) || m.getId().toLowerCase().contains(q)
                    || m.getType().toLowerCase().contains(q))) {
                continue;
            }
            medModel.addRow(new Object[]{m.getId(), m.getName(), m.getType(), Theme.peso(m.getPrice()),
                m.getStock(), m.getBatchNo(), m.getExpiry(), m.getStatus()});
        }
    }

    private CartItem findInCart(String id) {
        for (CartItem i : cart) {
            if (i.getMedicine().getId().equals(id)) return i;
        }
        return null;
    }

    private void addToCart() {
        int viewRow = medTable.getSelectedRow();
        if (viewRow < 0) {
            Theme.warn(this, "Please select a medicine from the list first.");
            return;
        }
        String id = (String) medModel.getValueAt(medTable.convertRowIndexToModel(viewRow), 0);
        Medicine m = service.findMedicine(id);
        if (m == null) {
            loadMedicines();
            return;
        }
        if (m.requiresPrescription() && !user.canDispenseRx()) {
            Theme.warn(this, m.getName() + " needs a doctor's prescription.\nPlease present it to our pharmacist at the counter.");
            return;
        }
        int qty = (Integer) spQty.getValue();
        CartItem existing = findInCart(id);
        try {
            service.validateLine(m, qty, existing == null ? 0 : existing.getQuantity());
            if (existing == null) {
                cart.add(new CartItem(m, qty));
            } else {
                existing.addQuantity(qty);
            }
            refreshCart();
            spQty.setValue(1);
        } catch (PharmacyException ex) {
            Theme.error(this, ex.getMessage());
        }
    }

    private void removeSelected() {
        int row = cartTable.getSelectedRow();
        if (row < 0) {
            Theme.warn(this, "Select a cart line to remove.");
            return;
        }
        cart.remove(cartTable.convertRowIndexToModel(row));
        refreshCart();
    }

    private void refreshCart() {
        cartModel.setRowCount(0);
        double total = 0;
        boolean needsRx = false, controlled = false;
        for (CartItem i : cart) {
            Medicine m = i.getMedicine();
            cartModel.addRow(new Object[]{m.getName(), m.getBatchNo(),
                m.getExpiry() + " (" + m.daysToExpiry() + "d)", i.getQuantity(),
                Theme.peso(m.getPrice()), Theme.peso(i.getSubtotal())});
            total += i.getSubtotal();
            needsRx |= m.requiresPrescription();
            controlled |= m.isControlled();
        }
        lblTotal.setText("Total: " + Theme.peso(total));
        txtRx.setEnabled(needsRx);
        txtPrescriber.setEnabled(controlled);
        if (!needsRx) txtRx.setText("");
        if (!controlled) txtPrescriber.setText("");
    }

    private void clearAll() {
        cart.clear();
        txtRx.setText("");
        txtPatient.setText("");
        txtPrescriber.setText("");
        refreshCart();
    }

    private void completeTransaction() {
        if (cart.isEmpty()) {
            Theme.warn(this, "The cart is empty. Add at least one medicine.");
            return;
        }
        if (!Theme.confirm(this, "Complete this transaction?")) {
            return;
        }
        try {
            String receipt = service.dispense(cart, txtRx.getText(), txtPatient.getText(), txtPrescriber.getText(), user);
            JTextArea area = new JTextArea(receipt);
            area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
            area.setEditable(false);
            JScrollPane sp = new JScrollPane(area);
            sp.setPreferredSize(new Dimension(420, 360));
            JOptionPane.showMessageDialog(this, sp, "Transaction Complete", JOptionPane.INFORMATION_MESSAGE);
            clearAll();
            loadMedicines();
        } catch (PharmacyException ex) {
            Theme.error(this, ex.getMessage());
        }
    }
}
