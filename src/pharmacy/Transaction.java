package pharmacy;

/** An immutable record of one dispensed/sold line item (saved to transactions.txt / controlled_log.txt). */
public class Transaction {
    private final String txnId, receiptNo, dateTime, medicineId, medicineName, batchNo, expiry;
    private final int quantity;
    private final double unitPrice, total;
    private final String rxId, patient, prescriber, staff;

    public Transaction(String txnId, String receiptNo, String dateTime, String medicineId, String medicineName,
                       String batchNo, String expiry, int quantity, double unitPrice, double total,
                       String rxId, String patient, String prescriber, String staff) {
        this.txnId = txnId;
        this.receiptNo = receiptNo;
        this.dateTime = dateTime;
        this.medicineId = medicineId;
        this.medicineName = medicineName;
        this.batchNo = batchNo;
        this.expiry = expiry;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.total = total;
        this.rxId = rxId;
        this.patient = patient;
        this.prescriber = prescriber;
        this.staff = staff;
    }

    public String getTxnId() { return txnId; }
    public String getReceiptNo() { return receiptNo; }
    public String getMedicineId() { return medicineId; }
    public int getQuantity() { return quantity; }
    public double getTotal() { return total; }
    public String getRxId() { return rxId; }

    public Object[] toRow() {
        return new Object[]{txnId, receiptNo, dateTime, medicineName, batchNo, expiry, quantity,
                Theme.peso(unitPrice), Theme.peso(total), rxId, patient, prescriber, staff};
    }

    public boolean matches(String filter) {
        String all = (txnId + receiptNo + dateTime + medicineName + batchNo + rxId + patient + prescriber + staff)
                .toLowerCase();
        return all.contains(filter.toLowerCase());
    }

    public String toFileString() {
        return String.join("|", txnId, receiptNo, dateTime, medicineId, DataStore.clean(medicineName),
                DataStore.clean(batchNo), expiry, String.valueOf(quantity), String.valueOf(unitPrice),
                String.valueOf(total), rxId, DataStore.clean(patient), DataStore.clean(prescriber), staff);
    }

    public static Transaction fromFileString(String line) {
        String[] p = line.split("\\|", -1);
        if (p.length < 14) {
            throw new IllegalArgumentException("Corrupt transaction record");
        }
        return new Transaction(p[0], p[1], p[2], p[3], p[4], p[5], p[6], Integer.parseInt(p[7]),
                Double.parseDouble(p[8]), Double.parseDouble(p[9]), p[10], p[11], p[12], p[13]);
    }
}
