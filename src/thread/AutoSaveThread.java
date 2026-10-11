package thread;

import model.Snippet;
import service.SnippetService;
import util.FileUtil;

import java.util.ArrayList;

/**
 * Copies the snippets to backup.txt every few seconds.
 * This thread is the ONLY writer of backup.txt; the main thread only writes snippets.txt,
 * so the two threads never write to the same file.
 */
public class AutoSaveThread extends Thread {
    private final SnippetService service;
    private final long intervalMillis;
    private int saveCount = 0;

    public AutoSaveThread(SnippetService service, long intervalMillis) {
        super("AutoSave");
        this.service = service;
        this.intervalMillis = intervalMillis;
        setDaemon(true);
    }

    public int getSaveCount() {
        return saveCount;
    }

    @Override
    public void run() {
        while (!isInterrupted()) {
            try {
                Thread.sleep(intervalMillis);
            } catch (InterruptedException e) {
                break;                              // main thread asked us to stop
            }
            saveNow();
        }
    }

    private void saveNow() {
        try {
            ArrayList<Snippet> snapshot = new ArrayList<>(service.getAll());
            FileUtil.writeDatabase(snapshot, FileUtil.AUTOSAVE_FILE);
            saveCount++;
        } catch (Exception e) {
            System.out.println("\n[AutoSave] skipped this round: " + e.getMessage());
        }
    }
}
