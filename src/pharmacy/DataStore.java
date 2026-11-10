package pharmacy;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * The "database": plain .txt files in the data/ folder (no MySQL/XAMPP).
 * Uses java.util.Scanner to read and PrintWriter to write.
 */
public final class DataStore {
    public static final String DIR = "data";
    public static final String USERS = DIR + File.separator + "users.txt";
    public static final String MEDICINES = DIR + File.separator + "medicines.txt";
    public static final String TRANSACTIONS = DIR + File.separator + "transactions.txt";
    public static final String CONTROLLED_LOG = DIR + File.separator + "controlled_log.txt";

    private DataStore() { }

    /** Reads every non-empty line of a text file with a Scanner. Missing file = empty list. */
    public static List<String> readLines(String path) throws PharmacyException {
        List<String> lines = new ArrayList<>();
        File file = new File(path);
        if (!file.exists()) {
            return lines;
        }
        try (Scanner sc = new Scanner(file, "UTF-8")) {
            while (sc.hasNextLine()) {
                String line = sc.nextLine().trim();
                if (!line.isEmpty()) {
                    lines.add(line);
                }
            }
        } catch (IOException e) {
            throw new PharmacyException("Cannot read file '" + path + "': " + e.getMessage());
        }
        return lines;
    }

    /** Overwrites a text file with the given lines (creates the data folder if needed). */
    public static void writeLines(String path, List<String> lines) throws PharmacyException {
        File file = new File(path);
        File dir = file.getParentFile();
        if (dir != null && !dir.exists()) {
            dir.mkdirs();
        }
        try (PrintWriter pw = new PrintWriter(new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8))) {
            for (String line : lines) {
                pw.println(line);
            }
        } catch (IOException e) {
            throw new PharmacyException("Cannot write file '" + path + "': " + e.getMessage());
        }
    }

    /** Removes characters that would break the pipe-delimited record format. */
    public static String clean(String text) {
        return text == null ? "" : text.replace("|", "/").replace("\r", " ").replace("\n", " ").trim();
    }
}
