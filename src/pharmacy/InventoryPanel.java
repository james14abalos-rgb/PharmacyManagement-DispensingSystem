package pharmacy;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.KeyEvent;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;

/** Inventory screen: everyone can view; only admins can add, edit and delete (delete needs admin password). */
public class InventoryPanel extends JPanel implements Refreshable {
    private final PharmacyService service;
    private final User user;
    private final JTextField txtSearch = Theme.field(22);
    private final DefaultTableModel model = Theme.model("ID", "Type", "Medicine", "Price", "Stock", "Batch No.", "Expiry", "Status");
    private final JTable table = Theme.table(model);

    public InventoryPanel(PharmacyService service, User user) {
        this.service = service;
        this.user = user;
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        setBackground(Theme.BACKGROUND);

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        top.setOpaque(false);
        top.add(Theme.label("Search:"));
        txtSearch.setToolTipText("Filter by name, batch, ID or type");
        Theme.onChange(txtSearch, this::refresh);
        top.add(txtSearch);
        add(top, BorderLayout.NORTH);

        JPanel card = Theme.card("Medicine Inventory (Batch & Expiry)");
        card.add(Theme.scroll(table), BorderLayout.CENTER);
        add(card, BorderLayout.CENTER);

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        btns.setOpaque(false);
        if (user.canManageInventory()) {
            JButton add = Theme.button("Add Medicine", Theme.PRIMARY, KeyEvent.VK_A, "Add a new medicine batch (Alt+A)");
            JButton edit = Theme.button("Edit", Theme.NEUTRAL, KeyEvent.VK_E, "Edit the selected medicine (Alt+E)");
            JButton del = Theme.button("Delete", Theme.DANGER, KeyEvent.VK_D, "Delete the selected medicine - needs admin password (Alt+D)");
            add.addActionListener(e -> showForm(null));
            edit.addActionListener(e -> {
                Medicine m = selected();
                if (m == null) Theme.warn(this, "Select a medicine to edit."); else showForm(m);
            });
            del.addActionListener(e -> deleteSelected());
            btns.add(add);
            btns.add(edit);
            btns.add(del);
        } else {
            btns.add(new JLabel(user.canViewRecords() ? "View only - only administrators can add, edit or delete inventory."
                    : "Browse our medicines. Prescription (RX / CONTROLLED) items are given by the pharmacist."));
        }
        add(btns, BorderLayout.SOUTH);

        Theme.colorByStatus(table, 7);
        Theme.widths(table, 70, 90, 260, 90, 60, 110, 100, 110);
        refresh();
    }

    @Override
    public void refresh() {
        model.setRowCount(0);
        String q = txtSearch.getText().trim().toLowerCase();
        for (Medicine m : service.getMedicines()) {
            String all = (m.getId() + m.getType() + m.getName() + m.getBatchNo()).toLowerCase();
            if (!q.isEmpty() && !all.contains(q)) continue;
            model.addRow(new Object[]{m.getId(), m.getType(), m.getName(), Theme.peso(m.getPrice()),
                m.getStock(), m.getBatchNo(), m.getExpiry(), m.getStatus()});
        }
    }

    private Medicine selected() {
        int row = table.getSelectedRow();
        if (row < 0) return null;
        return service.findMedicine((String) model.getValueAt(table.convertRowIndexToModel(row), 0));
    }

    private void showForm(Medicine existing) {
        JComboBox<String> cbType = new JComboBox<>(new String[]{"OTC", "RX", "CONTROLLED"});
        JTextField name = Theme.field(18), price = Theme.field(18), stock = Theme.field(18),
                batch = Theme.field(18), expiry = Theme.field(18);
        cbType.setToolTipText("OTC = no prescription, RX = prescription, CONTROLLED = logged controlled substance");
        name.setToolTipText("Generic/brand name with strength, e.g. Paracetamol 500mg Tablet");
        price.setToolTipText("Price per unit, e.g. 2.50");
        stock.setToolTipText("Units on hand (whole number)");
        batch.setToolTipText("Manufacturer batch / lot number");
        expiry.setToolTipText("Format: yyyy-MM-dd");
        if (existing != null) {
            cbType.setSelectedItem(existing.getType());
            name.setText(existing.getName());
            price.setText(String.valueOf(existing.getPrice()));
            stock.setText(String.valueOf(existing.getStock()));
            batch.setText(existing.getBatchNo());
            expiry.setText(existing.getExpiry().toString());
        }
        JPanel p = new JPanel(new GridLayout(0, 2, 8, 8));
        p.add(Theme.label("Type *"));            p.add(cbType);
        p.add(Theme.label("Medicine name *"));   p.add(name);
        p.add(Theme.label("Unit price *"));      p.add(price);
        p.add(Theme.label("Stock quantity *"));  p.add(stock);
        p.add(Theme.label("Batch number *"));    p.add(batch);
        p.add(Theme.label("Expiry (yyyy-MM-dd) *")); p.add(expiry);

        String title = existing == null ? "Add Medicine" : "Edit Medicine " + existing.getId();
        while (true) {
            int r = JOptionPane.showConfirmDialog(this, p, title, JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (r != JOptionPane.OK_OPTION) return;
            try {
                String type = (String) cbType.getSelectedItem();
                if (existing == null) {
                    service.addMedicine(type, name.getText(), price.getText(), stock.getText(), batch.getText(), expiry.getText());
                } else {
                    service.updateMedicine(existing.getId(), type, name.getText(), price.getText(), stock.getText(),
                            batch.getText(), expiry.getText());
                }
                refresh();
                return;
            } catch (PharmacyException ex) {
                Theme.error(this, ex.getMessage());   // keep the form open so the user can correct it
            }
        }
    }

    private void deleteSelected() {
        Medicine m = selected();
        if (m == null) {
            Theme.warn(this, "Select a medicine to delete.");
            return;
        }
        if (!Theme.confirm(this, "Delete '" + m.getName() + "' (batch " + m.getBatchNo() + ")?\nThis cannot be undone.")) {
            return;
        }
        String pw = Theme.askPassword(this, "Enter your ADMIN password to confirm the deletion:");
        if (pw == null) return;
        try {
            service.deleteMedicine(m.getId(), user, pw);
            refresh();
            Theme.info(this, "Medicine deleted.");
        } catch (PharmacyException ex) {
            Theme.error(this, ex.getMessage());
        }
    }
}
