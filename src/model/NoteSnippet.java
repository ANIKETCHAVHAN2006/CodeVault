package model;

/** A plain-text knowledge note. Displayed as ordinary text. */
public class NoteSnippet extends Snippet {

    public NoteSnippet(String title, String language, String tags, String body,
                       int difficulty, int createdDay) {
        super(title, language, tags, body, difficulty, createdDay);
    }

    public NoteSnippet(int id, String title, String language, String tags, String body,
                       int difficulty, int stage, int nextDay, int times, int createdDay) {
        super(id, title, language, tags, body, difficulty, stage, nextDay, times, createdDay);
    }

    @Override
    public String getType() {
        return "NOTE";
    }

    public int getWordCount() {
        String text = getBody().trim();
        if (text.isEmpty()) {
            return 0;
        }
        return text.split(" ").length;
    }

    @Override
    public void display() {
        printHeader();
        System.out.println(getBody());
        System.out.println("(" + getWordCount() + " words)");
        printFooter();
    }

    @Override
    public String export() {
        return buildRecord();
    }
}
