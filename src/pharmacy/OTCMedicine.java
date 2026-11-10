package pharmacy;

import java.time.LocalDate;

/** INHERITANCE: Over-the-counter medicine - no prescription needed. */
public class OTCMedicine extends Medicine {

    public OTCMedicine(String id, String name, double price, int stock, String batchNo, LocalDate expiry) {
        super(id, name, price, stock, batchNo, expiry);
    }

    @Override public String getType() { return "OTC"; }
    @Override public boolean requiresPrescription() { return false; }
    @Override public int getMaxQtyPerTransaction() { return 20; }
}
