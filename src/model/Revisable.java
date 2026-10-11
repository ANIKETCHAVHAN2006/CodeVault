package model;

/** Anything that takes part in spaced revision. */
public interface Revisable {
    /** Revision gaps in days: after 1, 3, 7 and 15 days. */
    int[] INTERVALS = {1, 3, 7, 15};

    boolean isDue(int today);

    void applyRecall(int rating, int today);

    int getNextRevisionDay();
}
