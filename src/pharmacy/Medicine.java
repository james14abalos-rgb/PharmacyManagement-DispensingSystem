package pharmacy;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * ABSTRACTION + ENCAPSULATION: base class of every medicine sold by the pharmacy.
 * Subclasses (OTC / Prescription / Controlled) override the dispensing rules (POLYMORPHISM).
 */
public abstract class Medicine {
    public static final int LOW_STOCK_LEVEL = 20;
    public static final int NEAR_EXPIRY_DAYS = 90;

    private final String id;
    private String name;
    private double price;
    private int stock;
    private String batchNo;
    private LocalDate expiry;

    protected Medicine(String id, String name, double price, int stock, String batchNo, LocalDate expiry) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.stock = stock;
        this.batchNo = batchNo;
        this.expiry = expiry;
    }

    // ---- getters ----
    public String getId() { return id; }
    public String getName() { return name; }
    public double getPrice() { return price; }
    public int getStock() { return stock; }
    public String getBatchNo() { return batchNo; }
    public LocalDate getExpiry() { return expiry; }

    // ---- rules each medicine category must define ----
    public abstract String getType();
    public abstract boolean requiresPrescription();
    public abstract int getMaxQtyPerTransaction();

    public boolean isControlled() { return false; }

    // ---- stock / expiry logic ----
    public void deduct(int qty) {
        if (qty <= 0 || qty > stock) {
            throw new IllegalArgumentException("Invalid quantity to deduct");
        }
        stock -= qty;
    }

    public long daysToExpiry() {
        return ChronoUnit.DAYS.between(LocalDate.now(), expiry);
    }

    public boolean isExpired() {
        return daysToExpiry() < 0;
    }

    public String getStatus() {
        if (isExpired()) return "EXPIRED";
        if (stock == 0) return "OUT OF STOCK";
        if (daysToExpiry() <= NEAR_EXPIRY_DAYS) return "NEAR EXPIRY";
        if (stock <= LOW_STOCK_LEVEL) return "LOW STOCK";
        return "OK";
    }

    // ---- text-file persistence ----
    public String toFileString() {
        return String.join("|", id, getType(), DataStore.clean(name), String.valueOf(price),
                String.valueOf(stock), DataStore.clean(batchNo), expiry.toString());
    }

    /** Factory: builds the correct subclass from the type code. */
    public static Medicine create(String type, String id, String name, double price,
                                  int stock, String batchNo, LocalDate expiry) {
        switch (type) {
            case "CONTROLLED":
                return new ControlledMedicine(id, name, price, stock, batchNo, expiry);
            case "RX":
                return new PrescriptionMedicine(id, name, price, stock, batchNo, expiry);
            default:
                return new OTCMedicine(id, name, price, stock, batchNo, expiry);
        }
    }

    public static Medicine fromFileString(String line) {
        String[] p = line.split("\\|", -1);
        if (p.length < 7) {
            throw new IllegalArgumentException("Corrupt medicine record");
        }
        return create(p[1], p[0], p[2], Double.parseDouble(p[3]), Integer.parseInt(p[4]),
                p[5], LocalDate.parse(p[6]));
    }
}
