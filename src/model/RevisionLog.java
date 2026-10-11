package model;

/**
 * One revision event. Written as a fixed-length record so that
 * RandomAccessFile can jump straight to any entry.
 * Layout: dddddddd|iiiiiiii|rr\n  (8 + 1 + 8 + 1 + 2 + 1 = 21 bytes)
 */
public class RevisionLog {
    public static final int RECORD_LENGTH = 21;
    public static final int RATING_OFFSET = 18;

    private final int day;
    private final int snippetId;
    private final int rating;

    public RevisionLog(int day, int snippetId, int rating) {
        this.day = day;
        this.snippetId = snippetId;
        this.rating = rating;
    }

    public int getDay() { return day; }
    public int getSnippetId() { return snippetId; }
    public int getRating() { return rating; }

    public String toRecord() {
        return String.format("%08d|%08d|%02d\n", day, snippetId, rating);
    }

    public static RevisionLog fromRecord(String record) {
        int day = Integer.parseInt(record.substring(0, 8));
        int id = Integer.parseInt(record.substring(9, 17));
        int rating = Integer.parseInt(record.substring(18, 20));
        return new RevisionLog(day, id, rating);
    }

    public String display() {
        String label = rating == 1 ? "forgot" : (rating == 2 ? "hard" : "easy");
        return "Day " + day + " | snippet #" + snippetId + " | rating " + rating + " (" + label + ")";
    }
}
