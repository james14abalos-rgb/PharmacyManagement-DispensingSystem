package pharmacy;

import java.time.LocalDate;

/** INHERITANCE: Prescription-only medicine - needs a valid prescription ID and patient name. */
public class PrescriptionMedicine extends Medicine {

    public PrescriptionMedicine(String id, String name, double price, int stock, String batchNo, LocalDate expiry) {
        super(id, name, price, stock, batchNo, expiry);
    }

    @Override public String getType() { return "RX"; }
    @Override public boolean requiresPrescription() { return true; }
    @Override public int getMaxQtyPerTransaction() { return 30; }
}
