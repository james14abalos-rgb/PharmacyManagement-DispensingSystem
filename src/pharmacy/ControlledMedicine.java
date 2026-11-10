package pharmacy;

import java.time.LocalDate;

/**
 * INHERITANCE (multi-level): Controlled substance is a prescription medicine with stricter rules -
 * smaller quantity limit, prescriber required, one-time prescription use, and it is written to the controlled log.
 */
public class ControlledMedicine extends PrescriptionMedicine {

    public ControlledMedicine(String id, String name, double price, int stock, String batchNo, LocalDate expiry) {
        super(id, name, price, stock, batchNo, expiry);
    }

    @Override public String getType() { return "CONTROLLED"; }
    @Override public boolean isControlled() { return true; }
    @Override public int getMaxQtyPerTransaction() { return 5; }
}
