package pharmacy;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/** Password hashing (SHA-256) and strength validation. Passwords are never stored in plain text. */
public final class PasswordUtil {

    private PasswordUtil() { }

    public static String hash(String plain) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] bytes = md.digest(plain.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    /** @return null when the password is acceptable, otherwise the reason it is rejected. */
    public static String validateStrength(String pw) {
        if (pw == null || pw.length() < 6) {
            return "Password must be at least 6 characters long.";
        }
        boolean letter = false, digit = false;
        for (char c : pw.toCharArray()) {
            if (Character.isLetter(c)) letter = true;
            if (Character.isDigit(c)) digit = true;
        }
        if (!letter || !digit) {
            return "Password must contain at least one letter and one number.";
        }
        if (pw.contains("|")) {
            return "Password must not contain the '|' character.";
        }
        return null;
    }
}
