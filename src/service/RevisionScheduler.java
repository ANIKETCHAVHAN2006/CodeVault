package service;

import model.RevisionLog;
import model.Snippet;
import util.FileUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.TreeMap;

/**
 * Spaced revision. A TreeMap groups snippets by their due day (sorted automatically),
 * and a plain Queue (LinkedList) holds the snippets that are due now.
 */
public class RevisionScheduler {
    private final Queue<Snippet> queue = new LinkedList<>();
    private final TreeMap<Integer, ArrayList<Snippet>> schedule = new TreeMap<>();

    /** Rebuilds the day map and fills the queue with everything due today or overdue. */
    public void rebuild(List<Snippet> all, int today) {
        schedule.clear();
        queue.clear();

        for (Snippet s : all) {
            int day = s.getNextRevisionDay();
            ArrayList<Snippet> bucket = schedule.get(day);
            if (bucket == null) {
                bucket = new ArrayList<>();
                schedule.put(day, bucket);
            }
            bucket.add(s);
        }

        for (Map.Entry<Integer, ArrayList<Snippet>> entry : schedule.entrySet()) {
            if (entry.getKey() > today) {
                break;                      // keys are sorted, so nothing later is due
            }
            for (Snippet s : entry.getValue()) {
                queue.add(s);               // most overdue first
            }
        }
    }

    public Snippet nextDue() {
        return queue.poll();
    }

    public int queueSize() {
        return queue.size();
    }

    public int countDue(List<Snippet> all, int today) {
        int due = 0;
        for (Snippet s : all) {
            if (s.isDue(today)) {
                due++;
            }
        }
        return due;
    }

    /** Applies the rating, then appends an entry to the revision log file. */
    public RevisionLog recordRecall(Snippet s, int rating, int today) throws IOException {
        s.applyRecall(rating, today);
        RevisionLog log = new RevisionLog(today, s.getId(), rating);
        FileUtil.appendLog(log);
        return log;
    }

    /** Text overview of what is coming up, day by day. */
    public String preview(List<Snippet> all, int today) {
        rebuild(all, today);
        if (schedule.isEmpty()) {
            return "Nothing scheduled yet.\n";
        }
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<Integer, ArrayList<Snippet>> entry : schedule.entrySet()) {
            int day = entry.getKey();
            String when;
            if (day < today) {
                when = "overdue by " + (today - day) + " day(s)";
            } else if (day == today) {
                when = "TODAY";
            } else {
                when = "in " + (day - today) + " day(s)";
            }
            sb.append("Day ").append(day).append(" (").append(when).append("):\n");
            for (Snippet s : entry.getValue()) {
                sb.append("   #").append(s.getId()).append(" ").append(s.getTitle()).append('\n');
            }
        }
        return sb.toString();
    }
}
