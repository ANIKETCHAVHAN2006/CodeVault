package model;

/** Abstract base for everything stored in the vault. */
public abstract class Item {
    public static final String SEPARATOR = "<#>";
    public static final String NEWLINE_TOKEN = "<NL>";

    private static int idCounter = 1;

    private int id;
    private String title;
    private int createdDay;

    /** Creates a brand-new item with a fresh ID. */
    protected Item(String title, int createdDay) {
        this.id = idCounter++;
        this.title = title;
        this.createdDay = createdDay;
    }

    /** Recreates an item loaded from a file, keeping its saved ID. */
    protected Item(int id, String title, int createdDay) {
        this.id = id;
        this.title = title;
        this.createdDay = createdDay;
        if (id >= idCounter) {
            idCounter = id + 1;
        }
    }

    /** Gives this item a new unique ID (used by clone and import). */
    public void assignNewId() {
        this.id = idCounter++;
    }

    public int getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public int getCreatedDay() {
        return createdDay;
    }

    /** Prints the item to the console. Each subclass formats itself. */
    public abstract void display();

    /** One-line text form used for saving. */
    public abstract String export();
}
