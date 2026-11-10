package pharmacy;

/** INHERITANCE: Pharmacy staff - can dispense and view records, but cannot manage users or edit inventory. */
public class Staff extends User {

    public Staff(String id, String username, String passwordHash, String fullName, boolean blocked) {
        super(id, username, passwordHash, fullName, blocked);
    }

    @Override public String getRole() { return "STAFF"; }
    @Override public boolean canManageUsers() { return false; }
    @Override public boolean canDispenseRx() { return true; }
    @Override public boolean canViewRecords() { return true; }
    @Override public boolean canManageInventory() { return false; }
}
