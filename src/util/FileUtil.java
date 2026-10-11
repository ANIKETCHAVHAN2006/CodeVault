package util;

import exception.InvalidInputException;
import model.RevisionLog;
import model.Snippet;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

/** Every file and folder operation in the project lives here. */
public class FileUtil {

    public static final String VAULT_DIR = "vault";
    public static final String SNIPPET_DIR = VAULT_DIR + File.separator + "snippets";
    public static final String BACKUP_DIR = VAULT_DIR + File.separator + "backups";
    public static final String ARCHIVE_DIR = BACKUP_DIR + File.separator + "archive";
    public static final String LOG_DIR = VAULT_DIR + File.separator + "logs";

    public static final String DB_FILE = SNIPPET_DIR + File.separator + "snippets.txt";
    public static final String AUTOSAVE_FILE = BACKUP_DIR + File.separator + "backup.txt";
    public static final String LOG_FILE = LOG_DIR + File.separator + "revision.log";
    public static final String DAY_FILE = VAULT_DIR + File.separator + "day.txt";
    public static final String PASSWORD_FILE = VAULT_DIR + File.separator + "master.txt";

    private FileUtil() {
    }

    // ------------------------------------------------------------------
    // File class: folders, existence, listing, deleting
    // ------------------------------------------------------------------
    public static void initDirectories() {
        String[] folders = {SNIPPET_DIR, BACKUP_DIR, ARCHIVE_DIR, LOG_DIR};
        for (String folder : folders) {
            File dir = new File(folder);
            if (!dir.exists()) {
                dir.mkdirs();
            }
        }
    }

    public static boolean exists(String path) {
        return new File(path).exists();
    }

    /** Names of the files (not folders) in the backups folder. */
    public static List<String> listBackups() {
        List<String> names = new ArrayList<>();
        File[] files = new File(BACKUP_DIR).listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.isFile()) {
                    names.add(f.getName());
                }
            }
        }
        return names;
    }

    public static boolean deleteBackup(String name) throws InvalidInputException {
        ValidationUtil.requireFileName(name);
        File file = new File(BACKUP_DIR, name);
        return file.isFile() && file.delete();
    }

    // ------------------------------------------------------------------
    // Files / Path (NIO): archive old binary backups
    // ------------------------------------------------------------------
    /** Moves all .bak files except the newest 'keepNewest' into the archive folder. */
    public static int archiveOldBackups(int keepNewest) throws IOException {
        List<File> baks = new ArrayList<>();
        File[] files = new File(BACKUP_DIR).listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.isFile() && f.getName().endsWith(".bak")) {
                    baks.add(f);
                }
            }
        }
        // selection sort, oldest first
        for (int i = 0; i < baks.size() - 1; i++) {
            int min = i;
            for (int j = i + 1; j < baks.size(); j++) {
                if (baks.get(j).lastModified() < baks.get(min).lastModified()) {
                    min = j;
                }
            }
            File temp = baks.get(i);
            baks.set(i, baks.get(min));
            baks.set(min, temp);
        }
        int moved = 0;
        for (int i = 0; i < baks.size() - keepNewest; i++) {
            Path source = baks.get(i).toPath();
            Path target = Paths.get(ARCHIVE_DIR, baks.get(i).getName());
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
            moved++;
        }
        return moved;
    }

    // ------------------------------------------------------------------
    // BufferedWriter: the snippet database, one record per line
    // ------------------------------------------------------------------
    public static void writeDatabase(List<Snippet> snippets, String path) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(path))) {
            for (Snippet s : snippets) {
                writer.write(s.export());
                writer.newLine();
            }
        }
    }

    // ------------------------------------------------------------------
    // FileReader: read a whole text file / load snippets
    // ------------------------------------------------------------------
    public static String readWholeFile(String path) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (FileReader reader = new FileReader(path)) {
            char[] chunk = new char[1024];
            int count;
            while ((count = reader.read(chunk)) != -1) {
                sb.append(chunk, 0, count);
            }
        }
        return sb.toString();
    }

    /** Loads snippets from a text file. Bad lines are skipped with a warning. */
    public static List<Snippet> loadDatabase(String path) throws IOException {
        List<Snippet> result = new ArrayList<>();
        if (!exists(path)) {
            return result;
        }
        String[] lines = readWholeFile(path).split("\n");
        for (String line : lines) {
            String clean = line.replace("\r", "").trim();
            if (clean.isEmpty()) {
                continue;
            }
            try {
                result.add(Snippet.fromRecord(clean));
            } catch (InvalidInputException e) {
                System.out.println("Skipped a bad line: " + e.getMessage());
            }
        }
        return result;
    }

    // ------------------------------------------------------------------
    // FileWriter: plain text export (import uses loadDatabase / FileReader)
    // ------------------------------------------------------------------
    public static void exportText(List<Snippet> snippets, String path) throws IOException {
        try (FileWriter writer = new FileWriter(path)) {
            for (Snippet s : snippets) {
                writer.write(s.export());
                writer.write("\n");
            }
        }
    }

    public static void writeText(String path, String text) throws IOException {
        try (FileWriter writer = new FileWriter(path)) {
            writer.write(text);
        }
    }

    // ------------------------------------------------------------------
    // PrintWriter: formatted report
    // ------------------------------------------------------------------
    public static void writeReport(String path, String title, String reportText) throws IOException {
        try (PrintWriter out = new PrintWriter(new FileWriter(path))) {
            out.println("##########################################");
            out.printf("#  %-37s #%n", title);
            out.println("##########################################");
            out.println();
            out.print(reportText);
            out.println();
            out.println("-- end of report --");
        }
    }

    // ------------------------------------------------------------------
    // FileInputStream / FileOutputStream: binary byte-copy backup
    // ------------------------------------------------------------------
    public static long binaryCopy(String sourcePath, String targetPath) throws IOException {
        long total = 0;
        try (FileInputStream in = new FileInputStream(sourcePath);
             FileOutputStream out = new FileOutputStream(targetPath)) {
            byte[] buffer = new byte[4096];
            int count;
            while ((count = in.read(buffer)) != -1) {
                out.write(buffer, 0, count);
                total += count;
            }
        }
        return total;
    }

    // ------------------------------------------------------------------
    // RandomAccessFile: fixed-length revision log records
    // ------------------------------------------------------------------
    public static void appendLog(RevisionLog log) throws IOException {
        try (RandomAccessFile raf = new RandomAccessFile(LOG_FILE, "rw")) {
            raf.seek(raf.length());
            raf.write(log.toRecord().getBytes(StandardCharsets.US_ASCII));
        }
    }

    public static int countLogs() throws IOException {
        if (!exists(LOG_FILE)) {
            return 0;
        }
        try (RandomAccessFile raf = new RandomAccessFile(LOG_FILE, "r")) {
            return (int) (raf.length() / RevisionLog.RECORD_LENGTH);
        }
    }

    /** Reads the last n log records by seeking straight to them. */
    public static List<RevisionLog> readLastLogs(int n) throws IOException {
        List<RevisionLog> logs = new ArrayList<>();
        int total = countLogs();
        if (total == 0) {
            return logs;
        }
        long start = Math.max(0L, (long) total - n);
        try (RandomAccessFile raf = new RandomAccessFile(LOG_FILE, "r")) {
            raf.seek(start * RevisionLog.RECORD_LENGTH);
            byte[] record = new byte[RevisionLog.RECORD_LENGTH];
            for (long i = start; i < total; i++) {
                raf.readFully(record);
                logs.add(RevisionLog.fromRecord(new String(record, StandardCharsets.US_ASCII)));
            }
        }
        return logs;
    }

    public static List<RevisionLog> readAllLogs() throws IOException {
        return readLastLogs(Integer.MAX_VALUE);
    }

    /** Overwrites only the 2-digit rating of one record, in place. */
    public static void updateLogRating(int index, int newRating) throws IOException {
        try (RandomAccessFile raf = new RandomAccessFile(LOG_FILE, "rw")) {
            long position = (long) index * RevisionLog.RECORD_LENGTH + RevisionLog.RATING_OFFSET;
            raf.seek(position);
            raf.write(String.format("%02d", newRating).getBytes(StandardCharsets.US_ASCII));
        }
    }

    // ------------------------------------------------------------------
    // Session day counter
    // ------------------------------------------------------------------
    public static int readDay() throws IOException {
        if (!exists(DAY_FILE)) {
            return 0;
        }
        try {
            return Integer.parseInt(readWholeFile(DAY_FILE).trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public static void writeDay(int day) throws IOException {
        writeText(DAY_FILE, String.valueOf(day));
    }
}
