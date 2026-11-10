package pharmacy;

/** Checked exception used for every validation / business-rule / file error in the system. */
public class PharmacyException extends Exception {
    public PharmacyException(String message) {
        super(message);
    }
}
