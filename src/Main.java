import exception.InvalidInputException;
import exception.SnippetNotFoundException;
import model.CodeSnippet;
import model.CommandSnippet;
import model.NoteSnippet;
import model.RevisionLog;
import model.Snippet;
import service.AuthService;
import service.RevisionScheduler;
import service.SearchService;
import service.SnippetService;
import service.StatisticsService;
import thread.AutoSaveThread;
import thread.ReminderThread;
import util.FileUtil;
import util.InputUtil;
import util.SortUtil;
import util.ValidationUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Main {

    private static final String[] MENU = {
        "Exit",
        "Add snippet",
        "View all snippets",
        "View one snippet",
        "Edit snippet",
        "Delete snippet",
        "Duplicate snippet (clone)",
        "Search",
        "Revise now",
        "Revision schedule",
        "Statistics",
        "Export snippets (text file)",
        "Import snippets (text file)",
        "Export report (PrintWriter)",
        "Binary backup",
        "Manage backups",
        "Recent revision logs"
    };

    private static final long AUTOSAVE_MILLIS = 30_000;
    private static final long REMINDER_MILLIS = 120_000;

    private static final SnippetService service = new SnippetService();
    private static final SearchService search = new SearchService(service);
    private static final RevisionScheduler scheduler = new RevisionScheduler();
    private static final StatisticsService stats = new StatisticsService(service, search);

    private static int today;

    public static void main(String[] args) {
        FileUtil.initDirectories();

        System.out.println("=== CodeVault ===");
        try {
            AuthService auth = new AuthService();
            if (!auth.authenticate()) {
                System.out.println("Too many failed attempts. Goodbye.");
                return;
            }
            today = FileUtil.readDay() + 1;      // each program start counts as a new "day"
            FileUtil.writeDay(today);
            service.loadFromDisk();
        } catch (IOException e) {
            System.out.println("Could not start: " + e.getMessage());
            return;
        }

        System.out.println("Session day " + today + " | " + service.size() + " snippet(s) loaded.");
        int due = scheduler.countDue(service.getAll(), today);
        if (due > 0) {
            System.out.println(">> " + due + " snippet(s) are due for revision today!");
        }

        AutoSaveThread autoSave = new AutoSaveThread(service, AUTOSAVE_MILLIS);
        Thread reminder = new Thread(new ReminderThread(service, today, REMINDER_MILLIS), "Reminder");
        reminder.setDaemon(true);
        autoSave.start();
        reminder.start();

        int choice = -1;
        do {
            printMenu();
            try {
                choice = ValidationUtil.parseInt(InputUtil.prompt("Choose: "), "Menu choice", 0, MENU.length - 1);
                handle(choice);
            } catch (InvalidInputException | SnippetNotFoundException | IOException e) {
                System.out.println("Error: " + e.getMessage());
                choice = -1;
            }
        } while (choice != 0);

        // shutdown: final save, then stop and wait for the helper threads
        try {
            service.saveToDisk();
            System.out.println("Vault saved.");
        } catch (IOException e) {
            System.out.println("Could not save: " + e.getMessage());
        } finally {
            autoSave.interrupt();
            reminder.interrupt();
            try {
                autoSave.join();
                reminder.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            System.out.println("Goodbye!");
        }
    }

    // ------------------------------------------------------------------
    // menu
    // ------------------------------------------------------------------
    private static void printMenu() {
        System.out.println("\n----- MENU (day " + today + ") -----");
        for (int i = 1; i < MENU.length; i++) {
            System.out.println(String.format("%2d. %s", i, MENU[i]));
        }
        System.out.println(" 0. " + MENU[0]);
    }

    private static void handle(int choice) throws InvalidInputException, SnippetNotFoundException, IOException {
        switch (choice) {
            case 0:
                break;
            case 1:
                addSnippet();
                break;
            case 2:
                viewAll();
                break;
            case 3:
                askId("View which ID? ");
                currentSnippet().display();
                break;
            case 4:
                editSnippet();
                break;
            case 5:
                deleteSnippet();
                break;
            case 6:
                duplicateSnippet();
                break;
            case 7:
                searchMenu();
                break;
            case 8:
                reviseNow();
                break;
            case 9:
                System.out.println(scheduler.preview(service.getAll(), today));
                break;
            case 10:
                System.out.println(stats.buildReport(FileUtil.readAllLogs(), today));
                break;
            case 11:
                exportText();
                break;
            case 12:
                importText();
                break;
            case 13:
                exportReport();
                break;
            case 14:
                binaryBackup();
                break;
            case 15:
                manageBackups();
                break;
            case 16:
                showLogs();
                break;
            default:
                System.out.println("Unknown option.");
        }
    }

    // ------------------------------------------------------------------
    // helpers
    // ------------------------------------------------------------------
    private static int pickedId;

    private static void askId(String message) throws InvalidInputException {
        pickedId = ValidationUtil.parseInt(InputUtil.prompt(message), "ID", 1, Integer.MAX_VALUE);
    }

    private static Snippet currentSnippet() throws SnippetNotFoundException {
        return service.findById(pickedId);
    }

    private static void printList(List<Snippet> list) {
        if (list.isEmpty()) {
            System.out.println("(no snippets)");
            return;
        }
        for (Snippet s : list) {
            System.out.println(s.summaryLine());
        }
        System.out.println(list.size() + " snippet(s).");
    }

    // ------------------------------------------------------------------
    // features
    // ------------------------------------------------------------------
    private static void addSnippet() throws InvalidInputException, IOException {
        System.out.println("Type: 1) Code  2) Note  3) Command");
        int type = ValidationUtil.parseInt(InputUtil.prompt("Choose type: "), "Type", 1, 3);

        String title = ValidationUtil.requireText(InputUtil.prompt("Title: "), "Title");
        String language = "general";
        if (type != 2) {
            language = ValidationUtil.requireText(InputUtil.prompt("Language (e.g. java, bash): "), "Language")
                    .toLowerCase();
        }
        String tags = ValidationUtil.parseTags(InputUtil.prompt("Tags (comma separated): "));
        int difficulty = ValidationUtil.parseInt(InputUtil.prompt("Difficulty (1-5): "), "Difficulty", 1, 5);

        System.out.println("Enter the content. Finish with a line containing only END");
        String body = ValidationUtil.requireText(InputUtil.readMultiLine(), "Content");

        Snippet snippet;
        if (type == 1) {
            snippet = new CodeSnippet(title, language, tags, body, difficulty, today);
        } else if (type == 2) {
            snippet = new NoteSnippet(title, language, tags, body, difficulty, today);
        } else {
            snippet = new CommandSnippet(title, language, tags, body, difficulty, today);
        }

        service.add(snippet);
        service.saveToDisk();
        System.out.println("Added snippet #" + snippet.getId() + ". First revision on day "
                + snippet.getNextRevisionDay() + ".");
    }

    private static void viewAll() throws InvalidInputException {
        System.out.println("Sort by: 1) Title  2) Difficulty  3) Next revision  4) Order added");
        int sort = ValidationUtil.parseInt(InputUtil.prompt("Choose: "), "Sort option", 1, 4);

        ArrayList<Snippet> copy = new ArrayList<>(service.getAll());
        switch (sort) {
            case 1:
                SortUtil.sortByTitle(copy);
                break;
            case 2:
                SortUtil.sortByDifficulty(copy);
                break;
            case 3:
                SortUtil.sortByNextRevision(copy);
                break;
            default:
                break;
        }
        printList(copy);
    }

    private static void editSnippet() throws InvalidInputException, SnippetNotFoundException, IOException {
        askId("Edit which ID? ");
        Snippet s = currentSnippet();
        s.display();
        System.out.println("Press Enter on a field to keep its current value.");

        String title = InputUtil.prompt("New title: ");
        if (!title.isEmpty()) {
            s.setTitle(ValidationUtil.requireText(title, "Title"));
        }
        String tags = InputUtil.prompt("New tags: ");
        if (!tags.isEmpty()) {
            s.setTags(ValidationUtil.parseTags(tags));
        }
        String diff = InputUtil.prompt("New difficulty (1-5): ");
        if (!diff.isEmpty()) {
            s.setDifficulty(ValidationUtil.parseInt(diff, "Difficulty", 1, 5));
        }
        System.out.println("New content (finish with END; END alone keeps the old content):");
        String body = InputUtil.readMultiLine();
        if (!body.trim().isEmpty()) {
            s.setBody(ValidationUtil.requireText(body, "Content"));
        }

        service.refreshIndex();
        service.saveToDisk();
        System.out.println("Snippet #" + s.getId() + " updated.");
    }

    private static void deleteSnippet() throws InvalidInputException, SnippetNotFoundException, IOException {
        askId("Delete which ID? ");
        Snippet s = currentSnippet();
        String answer = InputUtil.prompt("Delete \"" + s.getTitle() + "\"? (y/n): ");
        if (answer.toLowerCase().startsWith("y")) {
            service.delete(pickedId);
            service.saveToDisk();
            System.out.println("Deleted.");
        } else {
            System.out.println("Cancelled.");
        }
    }

    private static void duplicateSnippet() throws InvalidInputException, SnippetNotFoundException, IOException {
        askId("Duplicate which ID? ");
        Snippet copy = service.duplicate(pickedId, today);
        service.saveToDisk();
        System.out.println("Created copy #" + copy.getId() + ": " + copy.getTitle());
    }

    private static void searchMenu() throws InvalidInputException {
        System.out.println("Search by: 1) Keyword  2) Language  3) Tag  4) Max difficulty");
        int mode = ValidationUtil.parseInt(InputUtil.prompt("Choose: "), "Search mode", 1, 4);
        ArrayList<Snippet> results;
        switch (mode) {
            case 1:
                results = search.byKeyword(ValidationUtil.requireText(InputUtil.prompt("Keyword: "), "Keyword"));
                break;
            case 2:
                results = search.byLanguage(ValidationUtil.requireText(InputUtil.prompt("Language: "), "Language"));
                break;
            case 3:
                System.out.println("Known tags: " + service.getUniqueTags());
                results = search.byTag(ValidationUtil.requireText(InputUtil.prompt("Tag: "), "Tag"));
                break;
            default:
                int max = ValidationUtil.parseInt(InputUtil.prompt("Max difficulty (1-5): "), "Difficulty", 1, 5);
                results = search.byMaxDifficulty(max);
                break;
        }
        printList(results);
    }

    private static void reviseNow() throws IOException {
        scheduler.rebuild(service.getAll(), today);
        if (scheduler.queueSize() == 0) {
            System.out.println("Nothing is due today. Nice work!");
            return;
        }
        System.out.println(scheduler.queueSize() + " snippet(s) due. Try to recall each one before revealing it.");

        int revised = 0;
        Snippet s;
        while ((s = scheduler.nextDue()) != null) {
            System.out.println("\n>> #" + s.getId() + " " + s.getTitle() + " [" + s.getLanguage() + "]");
            InputUtil.prompt("Press Enter to reveal... ");
            s.display();

            int rating = -1;
            do {
                try {
                    rating = ValidationUtil.parseInt(
                            InputUtil.prompt("How well did you recall it? 1=forgot 2=hard 3=easy (0=stop): "),
                            "Rating", 0, 3);
                } catch (InvalidInputException e) {
                    System.out.println(e.getMessage());
                }
            } while (rating < 0);

            if (rating == 0) {
                break;
            }
            scheduler.recordRecall(s, rating, today);
            revised++;
            System.out.println("Next revision on day " + s.getNextRevisionDay() + ".");
            service.saveToDisk();
        }
        System.out.println("\nSession finished. Revised " + revised + " snippet(s).");
    }

    private static void exportText() throws InvalidInputException, IOException {
        String name = ValidationUtil.requireFileName(InputUtil.prompt("Export file name (e.g. mysnippets): "));
        if (!name.endsWith(".txt")) {
            name = name + ".txt";
        }
        String path = FileUtil.SNIPPET_DIR + java.io.File.separator + name;
        FileUtil.exportText(service.getAll(), path);
        System.out.println("Exported " + service.size() + " snippet(s) to " + path);
    }

    private static void importText() throws InvalidInputException, IOException {
        String name = ValidationUtil.requireFileName(InputUtil.prompt("Import file name (inside vault/snippets): "));
        String path = FileUtil.SNIPPET_DIR + java.io.File.separator + name;
        if (!FileUtil.exists(path)) {
            System.out.println("File not found: " + path);
            return;
        }
        List<Snippet> loaded = FileUtil.loadDatabase(path);
        int added = service.importAll(loaded);
        service.saveToDisk();
        System.out.println("Read " + loaded.size() + ", added " + added + " (duplicates skipped).");
    }

    private static void exportReport() throws IOException {
        String path = FileUtil.SNIPPET_DIR + java.io.File.separator + "report_day" + today + ".txt";
        FileUtil.writeReport(path, "CodeVault report - day " + today,
                stats.buildReport(FileUtil.readAllLogs(), today));
        System.out.println("Report written to " + path);
    }

    private static void binaryBackup() throws IOException {
        service.saveToDisk();
        String target = FileUtil.BACKUP_DIR + java.io.File.separator + "snippets_day" + today + ".bak";
        long bytes = FileUtil.binaryCopy(FileUtil.DB_FILE, target);
        System.out.println("Backup created: " + target + " (" + bytes + " bytes)");
    }

    private static void manageBackups() throws InvalidInputException, IOException {
        List<String> names = FileUtil.listBackups();
        System.out.println("Files in " + FileUtil.BACKUP_DIR + ":");
        if (names.isEmpty()) {
            System.out.println("   (none)");
        }
        for (String n : names) {
            System.out.println("   " + n);
        }
        System.out.println("1) Delete a backup  2) Archive old backups (keep newest 2)  3) Back");
        int option = ValidationUtil.parseInt(InputUtil.prompt("Choose: "), "Option", 1, 3);
        if (option == 1) {
            String name = InputUtil.prompt("File name to delete: ");
            System.out.println(FileUtil.deleteBackup(name) ? "Deleted." : "No such backup file.");
        } else if (option == 2) {
            int moved = FileUtil.archiveOldBackups(2);
            System.out.println("Moved " + moved + " file(s) to " + FileUtil.ARCHIVE_DIR);
        }
    }

    private static void showLogs() throws InvalidInputException, IOException {
        int n = ValidationUtil.parseInt(InputUtil.prompt("How many recent entries? "), "Count", 1, 1000);
        List<RevisionLog> logs = FileUtil.readLastLogs(n);
        if (logs.isEmpty()) {
            System.out.println("No revisions logged yet.");
            return;
        }
        for (RevisionLog log : logs) {
            System.out.println(log.display());
        }
        String answer = InputUtil.prompt("Correct the rating of the LAST entry? (y/n): ");
        if (answer.toLowerCase().startsWith("y")) {
            int rating = ValidationUtil.parseInt(InputUtil.prompt("New rating (1-3): "), "Rating", 1, 3);
            FileUtil.updateLogRating(FileUtil.countLogs() - 1, rating);
            System.out.println("Updated in place.");
        }
    }
}
