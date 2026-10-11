package util;

import model.Snippet;

import java.util.List;

/** Manual sorting algorithms (no Comparator / Comparable). */
public class SortUtil {

    private SortUtil() {
    }

    /** Selection sort by title, A to Z. */
    public static void sortByTitle(List<Snippet> list) {
        for (int i = 0; i < list.size() - 1; i++) {
            int min = i;
            for (int j = i + 1; j < list.size(); j++) {
                if (list.get(j).getTitle().compareToIgnoreCase(list.get(min).getTitle()) < 0) {
                    min = j;
                }
            }
            swap(list, i, min);
        }
    }

    /** Bubble sort by difficulty, easiest first. */
    public static void sortByDifficulty(List<Snippet> list) {
        boolean swapped;
        do {
            swapped = false;
            for (int i = 0; i < list.size() - 1; i++) {
                if (list.get(i).getDifficulty() > list.get(i + 1).getDifficulty()) {
                    swap(list, i, i + 1);
                    swapped = true;
                }
            }
        } while (swapped);
    }

    /** Selection sort by next revision day, soonest first. */
    public static void sortByNextRevision(List<Snippet> list) {
        for (int i = 0; i < list.size() - 1; i++) {
            int min = i;
            for (int j = i + 1; j < list.size(); j++) {
                if (list.get(j).getNextRevisionDay() < list.get(min).getNextRevisionDay()) {
                    min = j;
                }
            }
            swap(list, i, min);
        }
    }

    /** Bubble sort on two parallel arrays, highest count first. */
    public static void sortCountsDescending(String[] names, int[] counts) {
        for (int pass = 0; pass < counts.length - 1; pass++) {
            for (int i = 0; i < counts.length - 1 - pass; i++) {
                if (counts[i] < counts[i + 1]) {
                    int tempCount = counts[i];
                    counts[i] = counts[i + 1];
                    counts[i + 1] = tempCount;

                    String tempName = names[i];
                    names[i] = names[i + 1];
                    names[i + 1] = tempName;
                }
            }
        }
    }

    private static void swap(List<Snippet> list, int a, int b) {
        Snippet temp = list.get(a);
        list.set(a, list.get(b));
        list.set(b, temp);
    }
}
