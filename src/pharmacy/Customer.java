package pharmacy;

/**
 * INHERITANCE: a walk-in customer, like in a physical pharmacy - NO sign-up and NO password.
 * It is a temporary "guest" user that is never saved to users.txt.
 * Customers can browse and buy OTC medicines only; the pharmacist handles prescriptions.
 */
public class Customer extends User {

    public Customer() {
        super("GUEST", "self-service", "", "Customer (Self-Service)", false);
    }

    @Override public String getRole() { return "CUSTOMER"; }
    @Override public boolean canManageUsers() { return false; }
    @Override public boolean canManageInventory() { return false; }
    @Override public boolean canDispenseRx() { return false; }
    @Override public boolean canViewRecords() { return false; }
    @Override public boolean checkPassword(String plain) { return false; }   // guests can never "log in"
}
