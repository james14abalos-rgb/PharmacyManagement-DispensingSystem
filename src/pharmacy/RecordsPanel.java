package pharmacy;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.KeyEvent;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;

/** Read-only report screen used for both the sales Transactions and the Controlled Substance Log. */
public class RecordsPanel extends JPanel implements Refreshable {
    private final PharmacyService service;
    private final boolean controlledOnly;
    private final JTextField txtSearch = Theme.field(22);
    private final DefaultTableModel model = Theme.model("Txn ID", "Receipt", "Date / Time", "Medicine", "Batch", "Expiry",
            "Qty", "Unit Price", "Total", "Rx ID", "Patient", "Prescriber", "Staff");
    private final JTable table = Theme.table(model);
    private final JLabel lblSummary = new JLabel(" ");

    public RecordsPanel(PharmacyService service, boolean controlledOnly) {
        this.service = service;
        this.controlledOnly = controlledOnly;
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        setBackground(Theme.BACKGROUND);

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        top.setOpaque(false);
        top.add(Theme.label("Search:"));
        txtSearch.setToolTipText("Filter by receipt, medicine, patient, prescription ID, staff or date");
        Theme.onChange(txtSearch, this::refresh);
        top.add(txtSearch);
        JButton btnRefresh = Theme.button("Refresh", Theme.PRIMARY, KeyEvent.VK_F, "Reload the records (Alt+F)");
        btnRefresh.addActionListener(e -> refresh());
        top.add(btnRefresh);
        add(top, BorderLayout.NORTH);

        JPanel card = Theme.card(controlledOnly ? "Controlled Substance Log (newest first)" : "Sales Transactions (newest first)");
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        Theme.widths(table, 80, 85, 140, 190, 85, 85, 40, 85, 90, 90, 120, 120, 80);
        card.add(Theme.scroll(table), BorderLayout.CENTER);
        add(card, BorderLayout.CENTER);

        lblSummary.setFont(Theme.FONT_BOLD);
        lblSummary.setForeground(Theme.PRIMARY_DARK);
        add(lblSummary, BorderLayout.SOUTH);
        refresh();
    }

    @Override
    public void refresh() {
        model.setRowCount(0);
        List<Transaction> list = controlledOnly ? service.getControlledLog() : service.getTransactions();
        String q = txtSearch.getText().trim();
        double total = 0;
        int units = 0, shown = 0;
        for (int i = list.size() - 1; i >= 0; i--) {     // newest first
            Transaction t = list.get(i);
            if (!q.isEmpty() && !t.matches(q)) continue;
            model.addRow(t.toRow());
            total += t.getTotal();
            units += t.getQuantity();
            shown++;
        }
        lblSummary.setText("Records: " + shown + "   |   Units dispensed: " + units + "   |   Total sales: " + Theme.peso(total));
    }
}
