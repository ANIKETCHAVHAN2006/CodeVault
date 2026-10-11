package model;

/** A piece of source code. Displayed with line numbers. */
public class CodeSnippet extends Snippet {

    public CodeSnippet(String title, String language, String tags, String body,
                       int difficulty, int createdDay) {
        super(title, language, tags, body, difficulty, createdDay);
    }

    public CodeSnippet(int id, String title, String language, String tags, String body,
                       int difficulty, int stage, int nextDay, int times, int createdDay) {
        super(id, title, language, tags, body, difficulty, stage, nextDay, times, createdDay);
    }

    @Override
    public String getType() {
        return "CODE";
    }

    public int getLineCount() {
        return getBody().split("\n").length;
    }

    @Override
    public void display() {
        printHeader();
        String[] lines = getBody().split("\n");
        for (int i = 0; i < lines.length; i++) {
            System.out.println(String.format("%3d | %s", i + 1, lines[i]));
        }
        printFooter();
    }

    @Override
    public String export() {
        return buildRecord();
    }
}
