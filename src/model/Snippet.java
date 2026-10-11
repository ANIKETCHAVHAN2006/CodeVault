package model;

import exception.InvalidInputException;

/** Base class for all snippet types. Holds the shared data and revision logic. */
public abstract class Snippet extends Item implements Exportable, Searchable, Revisable, Cloneable {
    private String language;
    private String tags;          // comma separated, e.g. "loops,array"
    private String body;
    private int difficulty;       // 1 (easy) to 5 (hard)
    private int stage;            // index into INTERVALS
    private int nextRevisionDay;
    private int timesRevised;

    /** Constructor for a new snippet. */
    protected Snippet(String title, String language, String tags, String body,
                      int difficulty, int createdDay) {
        super(title, createdDay);
        this.language = language;
        this.tags = tags;
        this.body = body;
        this.difficulty = difficulty;
        this.stage = 0;
        this.timesRevised = 0;
        this.nextRevisionDay = createdDay + INTERVALS[0];
    }

    /** Constructor used when loading a saved snippet. */
    protected Snippet(int id, String title, String language, String tags, String body,
                      int difficulty, int stage, int nextRevisionDay,
                      int timesRevised, int createdDay) {
        super(id, title, createdDay);
        this.language = language;
        this.tags = tags;
        this.body = body;
        this.difficulty = difficulty;
        this.stage = stage;
        this.nextRevisionDay = nextRevisionDay;
        this.timesRevised = timesRevised;
    }

    // ---------- getters / setters (encapsulation) ----------
    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }

    public String getTags() { return tags; }
    public void setTags(String tags) { this.tags = tags; }

    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }

    public int getDifficulty() { return difficulty; }
    public void setDifficulty(int difficulty) { this.difficulty = difficulty; }

    public int getStage() { return stage; }
    public int getTimesRevised() { return timesRevised; }

    @Override
    public int getNextRevisionDay() { return nextRevisionDay; }

    /** "CODE", "NOTE" or "COMMAND". */
    public abstract String getType();

    /** Splits the tag string into an array. Empty tag string gives an empty array. */
    public String[] getTagList() {
        if (tags == null || tags.isEmpty()) {
            return new String[0];
        }
        return tags.split(",");
    }

    // ---------- Revisable ----------
    @Override
    public boolean isDue(int today) {
        return nextRevisionDay <= today;
    }

    /**
     * rating 1 = forgot (back to the 1-day gap), 2 = hard (repeat same gap),
     * 3 = easy (move to the next gap).
     */
    @Override
    public void applyRecall(int rating, int today) {
        if (rating <= 1) {
            stage = 0;
        } else if (rating >= 3) {
            stage = Math.min(stage + 1, INTERVALS.length - 1);
        }
        nextRevisionDay = today + INTERVALS[stage];
        timesRevised++;
    }

    /** Restarts the schedule (used for fresh copies). */
    public void resetSchedule(int today) {
        stage = 0;
        timesRevised = 0;
        nextRevisionDay = today + INTERVALS[0];
    }

    // ---------- Searchable ----------
    @Override
    public boolean matches(String keyword) {
        String k = keyword.toLowerCase();
        return getTitle().toLowerCase().contains(k)
                || language.toLowerCase().contains(k)
                || tags.toLowerCase().contains(k)
                || body.toLowerCase().contains(k);
    }

    /** Custom duplicate check: same title, language and body (ignoring case/whitespace at the ends). */
    public boolean isDuplicate(Snippet s) {
        if (s == null || s == this) {
            return false;
        }
        return getTitle().trim().equalsIgnoreCase(s.getTitle().trim())
                && language.trim().equalsIgnoreCase(s.language.trim())
                && body.trim().equals(s.body.trim());
    }

    // ---------- Cloneable ----------
    /** Makes an independent copy with a new ID and the title marked "(copy)". */
    @Override
    public Snippet clone() {
        try {
            Snippet copy = (Snippet) super.clone();
            copy.assignNewId();
            copy.setTitle(getTitle() + " (copy)");
            return copy;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError("Snippet implements Cloneable, so this cannot happen");
        }
    }

    // ---------- shared formatting helpers ----------
    protected String stars() {
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i <= 5; i++) {
            sb.append(i <= difficulty ? '*' : '.');
        }
        return sb.toString();
    }

    protected void printHeader() {
        System.out.println("============================================");
        System.out.println("#" + getId() + "  " + getTitle() + "  [" + getType() + "]");
        System.out.println("Language: " + language + " | Difficulty: " + stars()
                + " | Tags: " + (tags.isEmpty() ? "-" : tags));
        System.out.println("Stage: " + (stage + 1) + "/" + INTERVALS.length
                + " | Revised: " + timesRevised + "x | Next revision: day " + nextRevisionDay);
        System.out.println("--------------------------------------------");
    }

    protected void printFooter() {
        System.out.println("============================================");
    }

    /** One-line summary for list views. */
    public String summaryLine() {
        return String.format("#%-3d %-10s %-28s %-10s diff:%d  next:day %d  tags:%s",
                getId(), "[" + getType() + "]", cut(getTitle(), 28), cut(language, 10),
                difficulty, nextRevisionDay, tags.isEmpty() ? "-" : tags);
    }

    private static String cut(String text, int max) {
        if (text.length() <= max) {
            return text;
        }
        return text.substring(0, max - 1) + "~";
    }

    /** Builds the saved record line. Each subclass supplies its own type tag through getType(). */
    protected String buildRecord() {
        StringBuilder sb = new StringBuilder();
        sb.append(getType()).append(SEPARATOR)
          .append(getId()).append(SEPARATOR)
          .append(getTitle()).append(SEPARATOR)
          .append(language).append(SEPARATOR)
          .append(tags).append(SEPARATOR)
          .append(difficulty).append(SEPARATOR)
          .append(stage).append(SEPARATOR)
          .append(nextRevisionDay).append(SEPARATOR)
          .append(timesRevised).append(SEPARATOR)
          .append(getCreatedDay()).append(SEPARATOR)
          .append(body.replace("\r", "").replace("\n", NEWLINE_TOKEN));
        return sb.toString();
    }

    /** Rebuilds the right subclass from a saved record line. */
    public static Snippet fromRecord(String line) throws InvalidInputException {
        String[] p = line.split(SEPARATOR, -1);
        if (p.length < 11) {
            throw new InvalidInputException("Corrupt record (too few fields).");
        }
        try {
            String type = p[0];
            int id = Integer.parseInt(p[1]);
            String title = p[2];
            String language = p[3];
            String tags = p[4];
            int difficulty = Integer.parseInt(p[5]);
            int stage = Integer.parseInt(p[6]);
            int nextDay = Integer.parseInt(p[7]);
            int times = Integer.parseInt(p[8]);
            int created = Integer.parseInt(p[9]);
            String body = p[10].replace(NEWLINE_TOKEN, "\n");

            if (stage < 0 || stage >= INTERVALS.length) {
                stage = 0;
            }
            switch (type) {
                case "CODE":
                    return new CodeSnippet(id, title, language, tags, body, difficulty,
                            stage, nextDay, times, created);
                case "NOTE":
                    return new NoteSnippet(id, title, language, tags, body, difficulty,
                            stage, nextDay, times, created);
                case "COMMAND":
                    return new CommandSnippet(id, title, language, tags, body, difficulty,
                            stage, nextDay, times, created);
                default:
                    throw new InvalidInputException("Unknown snippet type: " + type);
            }
        } catch (NumberFormatException e) {
            throw new InvalidInputException("Corrupt record (bad number).");
        }
    }
}
