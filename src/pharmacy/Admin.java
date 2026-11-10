package pharmacy;

/** INHERITANCE: Administrator - full access (inventory, users, unblocking accounts). */
public class Admin extends User {

    public Admin(String id, String username, String passwordHash, String fullName, boolean blocked) {
        super(id, username, passwordHash, fullName, blocked);
    }

    @Override public String getRole() { return "ADMIN"; }
    @Override public boolean canManageUsers() { return true; }
    @Override public boolean canDispenseRx() { return true; }
    @Override public boolean canViewRecords() { return true; }
    @Override public boolean canManageInventory() { return true; }
}
