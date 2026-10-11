package service;

import model.RevisionLog;
import model.Snippet;
import util.SortUtil;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

/** Counts and report text: snippets per language, top tags, revision streak. */
public class StatisticsService {
    private final SnippetService service;
    private final SearchService search;

    public StatisticsService(SnippetService service, SearchService search) {
        this.service = service;
        this.search = search;
    }

    public HashMap<String, Integer> languageCounts() {
        HashMap<String, Integer> counts = new HashMap<>();
        for (Snippet s : service.getAll()) {
            String lang = s.getLanguage().toLowerCase();
            counts.put(lang, counts.getOrDefault(lang, 0) + 1);
        }
        return counts;
    }

    /** Consecutive days (ending today, or yesterday if nothing yet today) with at least one revision. */
    public int calculateStreak(List<RevisionLog> logs, int today) {
        HashSet<Integer> days = new HashSet<>();
        for (RevisionLog log : logs) {
            days.add(log.getDay());
        }
        int day = today;
        if (!days.contains(day)) {
            day = today - 1;
        }
        int streak = 0;
        while (days.contains(day)) {
            streak++;
            day--;
        }
        return streak;
    }

    /** Revisions per day for the last 7 days. Index 6 is today, index 0 is six days ago. */
    public int[] weeklyActivity(List<RevisionLog> logs, int today) {
        int[] week = new int[7];
        for (RevisionLog log : logs) {
            int age = today - log.getDay();
            if (age >= 0 && age < 7) {
                week[6 - age]++;
            }
        }
        return week;
    }

    public String buildReport(List<RevisionLog> logs, int today) {
        StringBuilder sb = new StringBuilder();
        int total = service.size();
        sb.append("Total snippets : ").append(total).append('\n');

        int[] types = search.countByType();
        sb.append("By type        : ").append(types[0]).append(" code, ")
          .append(types[1]).append(" note, ").append(types[2]).append(" command\n");
        sb.append("Lines of code  : ").append(search.totalCodeLines()).append('\n');

        if (total > 0) {
            int sum = 0;
            int due = 0;
            for (Snippet s : service.getAll()) {
                sum += s.getDifficulty();
                if (s.isDue(today)) {
                    due++;
                }
            }
            double average = Math.round(sum * 10.0 / total) / 10.0;
            sb.append("Avg difficulty : ").append(average).append('\n');
            sb.append("Due today      : ").append(due).append('\n');
        }

        // snippets per language
        sb.append("\nSnippets per language\n");
        HashMap<String, Integer> langs = languageCounts();
        if (langs.isEmpty()) {
            sb.append("   (none)\n");
        }
        for (String lang : langs.keySet()) {
            sb.append(String.format("   %-12s %d%n", lang, langs.get(lang)));
        }

        // most-used tags (top 5)
        sb.append("\nMost-used tags\n");
        String[] names = service.getTagIndex().keySet().toArray(new String[0]);
        int[] counts = new int[names.length];
        for (int i = 0; i < names.length; i++) {
            counts[i] = service.getTagIndex().get(names[i]).size();
        }
        SortUtil.sortCountsDescending(names, counts);
        if (names.length == 0) {
            sb.append("   (none)\n");
        }
        int shown = Math.min(5, names.length);
        for (int i = 0; i < shown; i++) {
            sb.append(String.format("   %-12s %d%n", names[i], counts[i]));
        }

        // streak and weekly array
        sb.append("\nRevision streak: ").append(calculateStreak(logs, today)).append(" day(s)\n");
        sb.append("Last 7 days (oldest to today):\n");
        int[] week = weeklyActivity(logs, today);
        for (int i = 0; i < week.length; i++) {
            StringBuilder bar = new StringBuilder();
            for (int k = 0; k < week[i]; k++) {
                bar.append('#');
            }
            sb.append(String.format("   Day %-4d %-3d %s%n", today - (6 - i), week[i], bar));
        }
        sb.append("Total revisions logged: ").append(logs.size()).append('\n');
        return sb.toString();
    }
}
