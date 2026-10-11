package model;

/** A shell / terminal command. Displayed with a "$" prompt. */
public class CommandSnippet extends Snippet {

    public CommandSnippet(String title, String language, String tags, String body,
                          int difficulty, int createdDay) {
        super(title, language, tags, body, difficulty, createdDay);
    }

    public CommandSnippet(int id, String title, String language, String tags, String body,
                          int difficulty, int stage, int nextDay, int times, int createdDay) {
        super(id, title, language, tags, body, difficulty, stage, nextDay, times, createdDay);
    }

    @Override
    public String getType() {
        return "COMMAND";
    }

    @Override
    public void display() {
        printHeader();
        String[] lines = getBody().split("\n");
        for (String line : lines) {
            System.out.println("$ " + line);
        }
        printFooter();
    }

    @Override
    public String export() {
        return buildRecord();
    }
}
