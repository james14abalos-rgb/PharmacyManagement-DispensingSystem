package pharmacy;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Business-logic layer: accounts, inventory, and the key transaction
 * (validate stock -> check batch/expiry -> record prescription -> log sale -> update stock).
 * All data lives in ArrayLists that are saved to / loaded from txt files.
 */
public class PharmacyService {
    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final List<User> users = new ArrayList<>();
    private final List<Medicine> medicines = new ArrayList<>();
    private final List<Transaction> transactions = new ArrayList<>();
    private final List<Transaction> controlledLog = new ArrayList<>();

    public PharmacyService() throws PharmacyException {
        loadAll();
    }

    // =====================================================================
    //  LOAD / SAVE
    // =====================================================================
    private void loadAll() throws PharmacyException {
        for (String line : DataStore.readLines(DataStore.USERS)) {
            try { users.add(User.fromFileString(line)); } catch (RuntimeException ex) { /* skip corrupt line */ }
        }
        if (users.isEmpty()) {   // first run: create the default administrator
            users.add(new Admin("USR-001", "admin", PasswordUtil.hash("admin123"), "System Administrator", false));
            saveUsers();
        }

        for (String line : DataStore.readLines(DataStore.MEDICINES)) {
            try { medicines.add(Medicine.fromFileString(line)); } catch (RuntimeException ex) { /* skip */ }
        }
        if (medicines.isEmpty()) {
            seedMedicines();
            saveMedicines();
        }

        for (String line : DataStore.readLines(DataStore.TRANSACTIONS)) {
            try { transactions.add(Transaction.fromFileString(line)); } catch (RuntimeException ex) { /* skip */ }
        }
        for (String line : DataStore.readLines(DataStore.CONTROLLED_LOG)) {
            try { controlledLog.add(Transaction.fromFileString(line)); } catch (RuntimeException ex) { /* skip */ }
        }
    }

    private void seedMedicines() {
        seed("OTC", "Paracetamol 500mg Tablet", 2.50, 500, "PCM-2601", LocalDate.of(2028, 3, 31));
        seed("OTC", "Ibuprofen 200mg Tablet", 4.00, 300, "IBU-2517", LocalDate.of(2027, 11, 30));
        seed("OTC", "Cetirizine 10mg Tablet", 6.00, 150, "CTZ-2602", LocalDate.of(2028, 1, 31));
        seed("OTC", "Vitamin C 500mg Tablet", 5.00, 80, "VTC-2503", LocalDate.of(2026, 12, 15));
        seed("OTC", "Loperamide 2mg Capsule", 7.50, 60, "LPR-2410", LocalDate.of(2026, 9, 30));
        seed("RX", "Amoxicillin 500mg Capsule", 12.00, 200, "AMX-2605", LocalDate.of(2027, 8, 31));
        seed("RX", "Metformin 500mg Tablet", 8.50, 250, "MTF-2604", LocalDate.of(2028, 2, 28));
        seed("RX", "Losartan 50mg Tablet", 15.00, 18, "LSR-2606", LocalDate.of(2027, 10, 31));
        seed("CONTROLLED", "Tramadol 50mg Capsule", 25.00, 100, "TRM-2607", LocalDate.of(2027, 9, 30));
        seed("CONTROLLED", "Diazepam 5mg Tablet", 18.00, 40, "DZP-2608", LocalDate.of(2027, 12, 31));
    }

    private void seed(String type, String name, double price, int stock, String batch, LocalDate expiry) {
        medicines.add(Medicine.create(type, nextMedicineId(), name, price, stock, batch, expiry));
    }

    private void saveUsers() throws PharmacyException {
        DataStore.writeLines(DataStore.USERS, users.stream().map(User::toFileString).collect(Collectors.toList()));
    }

    private void saveMedicines() throws PharmacyException {
        DataStore.writeLines(DataStore.MEDICINES, medicines.stream().map(Medicine::toFileString).collect(Collectors.toList()));
    }

    private void saveTransactions() throws PharmacyException {
        DataStore.writeLines(DataStore.TRANSACTIONS, transactions.stream().map(Transaction::toFileString).collect(Collectors.toList()));
    }

    private void saveControlledLog() throws PharmacyException {
        DataStore.writeLines(DataStore.CONTROLLED_LOG, controlledLog.stream().map(Transaction::toFileString).collect(Collectors.toList()));
    }

    // =====================================================================
    //  READ-ONLY ACCESS (encapsulation: callers cannot modify the lists)
    // =====================================================================
    public List<User> getUsers() { return Collections.unmodifiableList(users); }
    public List<Medicine> getMedicines() { return Collections.unmodifiableList(medicines); }
    public List<Transaction> getTransactions() { return Collections.unmodifiableList(transactions); }
    public List<Transaction> getControlledLog() { return Collections.unmodifiableList(controlledLog); }

    public User findUser(String username) {
        for (User u : users) {
            if (u.getUsername().equalsIgnoreCase(username.trim())) return u;
        }
        return null;
    }

    public User findUserById(String id) {
        for (User u : users) {
            if (u.getId().equals(id)) return u;
        }
        return null;
    }

    public Medicine findMedicine(String id) {
        for (Medicine m : medicines) {
            if (m.getId().equals(id)) return m;
        }
        return null;
    }

    // =====================================================================
    //  AUTO-NUMBERING
    // =====================================================================
    private int maxSuffix(Collection<String> ids) {
        int max = 0;
        for (String id : ids) {
            try {
                max = Math.max(max, Integer.parseInt(id.substring(id.lastIndexOf('-') + 1)));
            } catch (NumberFormatException ignored) { /* not a numbered id */ }
        }
        return max;
    }

    private String nextUserId() {
        return String.format("USR-%03d", maxSuffix(users.stream().map(User::getId).collect(Collectors.toList())) + 1);
    }

    private String nextMedicineId() {
        return String.format("MED-%03d", maxSuffix(medicines.stream().map(Medicine::getId).collect(Collectors.toList())) + 1);
    }

    private String nextTxnId() {
        return String.format("TXN-%05d", maxSuffix(transactions.stream().map(Transaction::getTxnId).collect(Collectors.toList())) + 1);
    }

    private String nextReceiptNo() {
        return String.format("RCP-%05d", maxSuffix(transactions.stream().map(Transaction::getReceiptNo).collect(Collectors.toList())) + 1);
    }

    // =====================================================================
    //  ACCOUNTS & SECURITY
    // =====================================================================
    public User registerUser(String role, String fullName, String username, String password, String confirm)
            throws PharmacyException {
        fullName = fullName.trim();
        username = username.trim();
        if (fullName.length() < 3) {
            throw new PharmacyException("Full name is required (at least 3 characters).");
        }
        if (!username.matches("[A-Za-z0-9_]{4,15}")) {
            throw new PharmacyException("Username must be 4-15 characters: letters, numbers or underscore only.");
        }
        if (findUser(username) != null) {
            throw new PharmacyException("That username is already taken.");
        }
        String problem = PasswordUtil.validateStrength(password);
        if (problem != null) {
            throw new PharmacyException(problem);
        }
        if (!password.equals(confirm)) {
            throw new PharmacyException("Passwords do not match.");
        }
        User u = User.create(role, nextUserId(), username, PasswordUtil.hash(password), fullName, false);
        users.add(u);
        saveUsers();
        return u;
    }

    public void changePassword(User user, String oldPw, String newPw, String confirm) throws PharmacyException {
        if (!user.checkPassword(oldPw)) {
            throw new PharmacyException("Current password is incorrect.");
        }
        String problem = PasswordUtil.validateStrength(newPw);
        if (problem != null) {
            throw new PharmacyException(problem);
        }
        if (!newPw.equals(confirm)) {
            throw new PharmacyException("New password and confirmation do not match.");
        }
        if (newPw.equals(oldPw)) {
            throw new PharmacyException("New password must be different from the current password.");
        }
        user.setPasswordHash(PasswordUtil.hash(newPw));
        saveUsers();
    }

    /** Every delete requires the ADMIN's own password before proceeding. */
    public void confirmAdminPassword(User actor, String password) throws PharmacyException {
        if (actor == null || !actor.canManageUsers()) {
            throw new PharmacyException("Only a system administrator can perform this action.");
        }
        if (!actor.checkPassword(password)) {
            throw new PharmacyException("Incorrect administrator password. Action cancelled.");
        }
    }

    public void deleteUser(String userId, User actor, String adminPassword) throws PharmacyException {
        confirmAdminPassword(actor, adminPassword);
        User target = findUserById(userId);
        if (target == null) {
            throw new PharmacyException("User not found.");
        }
        if (target.getId().equals(actor.getId())) {
            throw new PharmacyException("You cannot delete the account you are currently using.");
        }
        users.remove(target);
        saveUsers();
    }

    public void setBlocked(User target, boolean blocked) throws PharmacyException {
        if (blocked && target.canManageUsers()) {
            throw new PharmacyException("Administrator accounts cannot be blocked.");
        }
        target.setBlocked(blocked);
        saveUsers();
    }

    // =====================================================================
    //  INVENTORY (CREATE / EDIT / DELETE)
    // =====================================================================
    private Medicine buildMedicine(String id, String type, String name, String priceTxt, String stockTxt,
                                   String batch, String expiryTxt) throws PharmacyException {
        name = name.trim();
        batch = batch.trim();
        if (name.length() < 2) {
            throw new PharmacyException("Medicine name is required.");
        }
        if (batch.isEmpty()) {
            throw new PharmacyException("Batch number is required.");
        }
        double price;
        int stock;
        LocalDate expiry;
        try {
            price = Double.parseDouble(priceTxt.trim());
        } catch (NumberFormatException e) {
            throw new PharmacyException("Price must be a valid number (e.g. 12.50).");
        }
        if (price <= 0) {
            throw new PharmacyException("Price must be greater than zero.");
        }
        try {
            stock = Integer.parseInt(stockTxt.trim());
        } catch (NumberFormatException e) {
            throw new PharmacyException("Stock must be a whole number.");
        }
        if (stock < 0) {
            throw new PharmacyException("Stock cannot be negative.");
        }
        try {
            expiry = LocalDate.parse(expiryTxt.trim());
        } catch (DateTimeParseException e) {
            throw new PharmacyException("Expiry date must use the format yyyy-MM-dd (e.g. 2028-12-31).");
        }
        for (Medicine m : medicines) {
            if (!m.getId().equals(id) && m.getName().equalsIgnoreCase(name) && m.getBatchNo().equalsIgnoreCase(batch)) {
                throw new PharmacyException("This medicine and batch number already exist in the inventory.");
            }
        }
        return Medicine.create(type, id, name, price, stock, batch, expiry);
    }

    public Medicine addMedicine(String type, String name, String price, String stock, String batch, String expiry)
            throws PharmacyException {
        Medicine m = buildMedicine(nextMedicineId(), type, name, price, stock, batch, expiry);
        if (m.isExpired()) {
            throw new PharmacyException("Cannot add a batch that has already expired.");
        }
        medicines.add(m);
        saveMedicines();
        return m;
    }

    public void updateMedicine(String id, String type, String name, String price, String stock, String batch,
                               String expiry) throws PharmacyException {
        Medicine old = findMedicine(id);
        if (old == null) {
            throw new PharmacyException("Medicine not found.");
        }
        Medicine updated = buildMedicine(id, type, name, price, stock, batch, expiry);
        medicines.set(medicines.indexOf(old), updated);
        saveMedicines();
    }

    public void deleteMedicine(String id, User actor, String adminPassword) throws PharmacyException {
        confirmAdminPassword(actor, adminPassword);
        Medicine m = findMedicine(id);
        if (m == null) {
            throw new PharmacyException("Medicine not found.");
        }
        medicines.remove(m);
        saveMedicines();
    }

    // =====================================================================
    //  KEY TRANSACTION: DISPENSING
    // =====================================================================
    /** Validates one cart line (expiry, per-transaction limit, stock). */
    public void validateLine(Medicine m, int qty, int alreadyInCart) throws PharmacyException {
        if (qty <= 0) {
            throw new PharmacyException("Quantity must be at least 1.");
        }
        if (m.isExpired()) {
            throw new PharmacyException(m.getName() + " (batch " + m.getBatchNo() + ") EXPIRED on " + m.getExpiry()
                    + ". It cannot be dispensed.");
        }
        int wanted = qty + alreadyInCart;
        if (wanted > m.getMaxQtyPerTransaction()) {
            throw new PharmacyException(m.getType() + " medicines are limited to " + m.getMaxQtyPerTransaction()
                    + " units per transaction.");
        }
        if (wanted > m.getStock()) {
            throw new PharmacyException("Insufficient stock for " + m.getName() + ". Available: " + m.getStock()
                    + ", requested: " + wanted + ".");
        }
    }

    private boolean isRxAlreadyUsed(String rxId, String medicineId) {
        for (Transaction t : controlledLog) {
            if (t.getRxId().equalsIgnoreCase(rxId) && t.getMedicineId().equals(medicineId)) return true;
        }
        return false;
    }

    /**
     * Completes the sale: validates every line, records the prescription, logs each sale line
     * (and the controlled-substance log), deducts stock, saves the files and returns a printable receipt.
     */
    public String dispense(List<CartItem> cart, String rxId, String patient, String prescriber, User staff)
            throws PharmacyException {
        if (cart == null || cart.isEmpty()) {
            throw new PharmacyException("The cart is empty.");
        }
        rxId = rxId == null ? "" : rxId.trim().toUpperCase();
        patient = patient == null ? "" : patient.trim();
        prescriber = prescriber == null ? "" : prescriber.trim();

        // ---- 1. validate everything before changing anything ----
        boolean needsRx = false, hasControlled = false;
        List<Medicine> live = new ArrayList<>();
        for (CartItem item : cart) {
            Medicine m = findMedicine(item.getMedicine().getId());
            if (m == null) {
                throw new PharmacyException(item.getMedicine().getName() + " no longer exists in the inventory.");
            }
            validateLine(m, item.getQuantity(), 0);
            live.add(m);
            needsRx |= m.requiresPrescription();
            hasControlled |= m.isControlled();
        }
        if (needsRx && !staff.canDispenseRx()) {
            throw new PharmacyException("Prescription medicines must be dispensed by our pharmacist at the counter.");
        }
        if (needsRx) {
            if (!rxId.matches("RX-\\d{4,10}")) {
                throw new PharmacyException("A valid Prescription ID is required (format: RX-0001).");
            }
            if (patient.length() < 3) {
                throw new PharmacyException("Patient name is required for prescription medicines.");
            }
        }
        if (hasControlled) {
            if (prescriber.length() < 3) {
                throw new PharmacyException("Prescriber (doctor) name is required for controlled substances.");
            }
            for (Medicine m : live) {
                if (m.isControlled() && isRxAlreadyUsed(rxId, m.getId())) {
                    throw new PharmacyException("Prescription " + rxId + " was already used to dispense "
                            + m.getName() + ". Controlled drugs need a new prescription.");
                }
            }
        }

        // ---- 2. apply: deduct stock + log sale lines ----
        String receiptNo = nextReceiptNo();
        String stamp = LocalDateTime.now().format(STAMP);
        StringBuilder sb = new StringBuilder();
        sb.append("==========================================\n");
        sb.append("           PHARMACARE PHARMACY\n");
        sb.append("             OFFICIAL RECEIPT\n");
        sb.append("==========================================\n");
        sb.append("Receipt No  : ").append(receiptNo).append('\n');
        sb.append("Date / Time : ").append(stamp).append('\n');
        sb.append("Dispensed by: ").append(staff.getFullName()).append('\n');
        if (!patient.isEmpty()) sb.append("Patient     : ").append(patient).append('\n');
        if (needsRx) sb.append("Prescription: ").append(rxId).append('\n');
        if (hasControlled) sb.append("Prescriber  : ").append(prescriber).append('\n');
        sb.append("------------------------------------------\n");

        double grand = 0;
        for (int i = 0; i < cart.size(); i++) {
            Medicine m = live.get(i);
            int qty = cart.get(i).getQuantity();
            double lineTotal = m.getPrice() * qty;
            m.deduct(qty);

            Transaction t = new Transaction(nextTxnId(), receiptNo, stamp, m.getId(), m.getName(), m.getBatchNo(),
                    m.getExpiry().toString(), qty, m.getPrice(), lineTotal,
                    m.requiresPrescription() ? rxId : "N/A",
                    patient.isEmpty() ? "Walk-in" : patient,
                    m.isControlled() ? prescriber : "N/A",
                    staff.getUsername());
            transactions.add(t);
            if (m.isControlled()) {
                controlledLog.add(t);
            }
            grand += lineTotal;
            sb.append(m.getName()).append('\n');
            sb.append("  Batch ").append(m.getBatchNo()).append(" | Exp ").append(m.getExpiry())
              .append(" (").append(m.daysToExpiry()).append(" days left)\n");
            sb.append("  ").append(qty).append(" x ").append(Theme.peso(m.getPrice()))
              .append(" = ").append(Theme.peso(lineTotal)).append('\n');
        }
        sb.append("------------------------------------------\n");
        sb.append("TOTAL: ").append(Theme.peso(grand)).append('\n');
        sb.append("==========================================\n");

        // ---- 3. persist ----
        saveMedicines();
        saveTransactions();
        saveControlledLog();
        return sb.toString();
    }
}
