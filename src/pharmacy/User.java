package pharmacy;

/**
 * ABSTRACTION + ENCAPSULATION: base class for every system user.
 * All fields are private; subclasses (Admin, Staff) define their own access level.
 */
public abstract class User {
    private final String id;
    private String username;
    private String passwordHash;
    private String fullName;
    private boolean blocked;

    protected User(String id, String username, String passwordHash, String fullName, boolean blocked) {
        this.id = id;
        this.username = username;
        this.passwordHash = passwordHash;
        this.fullName = fullName;
        this.blocked = blocked;
    }

    // ---- getters / setters (encapsulation) ----
    public String getId() { return id; }
    public String getUsername() { return username; }
    public String getFullName() { return fullName; }
    public boolean isBlocked() { return blocked; }
    public void setBlocked(boolean blocked) { this.blocked = blocked; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public boolean checkPassword(String plain) {
        return plain != null && passwordHash.equals(PasswordUtil.hash(plain));
    }

    // ---- abstract behaviour: each role decides its own access level (POLYMORPHISM) ----
    public abstract String getRole();
    public abstract boolean canManageUsers();
    public abstract boolean canManageInventory();
    /** Prescription / controlled medicines may only be dispensed by a pharmacist (admin or staff). */
    public abstract boolean canDispenseRx();
    /** Transactions, controlled log and account pages are for admin/staff only. */
    public abstract boolean canViewRecords();

    // ---- text-file persistence ----
    public String toFileString() {
        return String.join("|", id, username, passwordHash, getRole(),
                DataStore.clean(fullName), blocked ? "BLOCKED" : "ACTIVE");
    }

    public static User create(String role, String id, String username, String hash, String fullName, boolean blocked) {
        if ("ADMIN".equalsIgnoreCase(role)) {
            return new Admin(id, username, hash, fullName, blocked);
        }
        return new Staff(id, username, hash, fullName, blocked);
    }

    public static User fromFileString(String line) {
        String[] p = line.split("\\|", -1);
        if (p.length < 6) {
            throw new IllegalArgumentException("Corrupt user record");
        }
        return create(p[3], p[0], p[1], p[2], p[4], "BLOCKED".equals(p[5]));
    }

    @Override
    public String toString() {
        return fullName + " (" + getRole() + ")";
    }
}
