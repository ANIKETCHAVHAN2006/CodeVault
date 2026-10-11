package service;

import model.CodeSnippet;
import model.Searchable;
import model.Snippet;

import java.util.ArrayList;

/** Search helpers. Also shows instanceof and type casting on the snippet subtypes. */
public class SearchService {
    private final SnippetService service;

    public SearchService(SnippetService service) {
        this.service = service;
    }

    public ArrayList<Snippet> byKeyword(String keyword) {
        ArrayList<Snippet> found = new ArrayList<>();
        for (Snippet s : service.getAll()) {
            Searchable searchable = (Searchable) s;   // cast to the interface type
            if (searchable.matches(keyword)) {
                found.add(s);
            }
        }
        return found;
    }

    public ArrayList<Snippet> byLanguage(String language) {
        ArrayList<Snippet> found = new ArrayList<>();
        for (Snippet s : service.getAll()) {
            if (s.getLanguage().equalsIgnoreCase(language)) {
                found.add(s);
            }
        }
        return found;
    }

    public ArrayList<Snippet> byTag(String tag) {
        return new ArrayList<>(service.getByTag(tag));
    }

    public ArrayList<Snippet> byMaxDifficulty(int max) {
        ArrayList<Snippet> found = new ArrayList<>();
        for (Snippet s : service.getAll()) {
            if (s.getDifficulty() <= max) {
                found.add(s);
            }
        }
        return found;
    }

    /** Returns {codeCount, noteCount, commandCount}. */
    public int[] countByType() {
        int[] counts = new int[3];
        for (Snippet s : service.getAll()) {
            if (s instanceof CodeSnippet) {
                counts[0]++;
            } else if (s.getType().equals("NOTE")) {
                counts[1]++;
            } else {
                counts[2]++;
            }
        }
        return counts;
    }

    /** Total lines of code across all CodeSnippets (casts down to use getLineCount). */
    public int totalCodeLines() {
        int lines = 0;
        for (Snippet s : service.getAll()) {
            if (s instanceof CodeSnippet) {
                CodeSnippet code = (CodeSnippet) s;
                lines += code.getLineCount();
            }
        }
        return lines;
    }
}
