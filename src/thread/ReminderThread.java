package thread;

import model.Snippet;
import service.SnippetService;

import java.util.ArrayList;

/** Periodically tells the user how many snippets are waiting for revision. */
public class ReminderThread implements Runnable {
    private final SnippetService service;
    private final int today;
    private final long intervalMillis;

    public ReminderThread(SnippetService service, int today, long intervalMillis) {
        this.service = service;
        this.today = today;
        this.intervalMillis = intervalMillis;
    }

    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                Thread.sleep(intervalMillis);
            } catch (InterruptedException e) {
                break;
            }
            int due = 0;
            for (Snippet s : new ArrayList<>(service.getAll())) {
                if (s.isDue(today)) {
                    due++;
                }
            }
            if (due > 0) {
                System.out.println("\n[Reminder] " + due + " snippet(s) due for revision. Choose 'Revise now'.");
            }
        }
    }
}
