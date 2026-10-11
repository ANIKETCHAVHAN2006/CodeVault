package exception;

/** Thrown when a snippet ID does not exist in the vault. */
public class SnippetNotFoundException extends Exception {
    private final int id;

    public SnippetNotFoundException(int id) {
        super("Snippet with ID " + id + " was not found.");
        this.id = id;
    }

    public int getId() {
        return id;
    }
}
