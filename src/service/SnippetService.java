package service;

import exception.InvalidInputException;
import exception.SnippetNotFoundException;
import model.Snippet;
import util.FileUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

/** Holds all snippets in memory and keeps the tag index up to date. */
public class SnippetService {
    private final ArrayList<Snippet> snippets = new ArrayList<>();
    private final HashMap<String, ArrayList<Snippet>> tagIndex = new HashMap<>();
    private final HashSet<String> uniqueTags = new HashSet<>();

    // ---------- persistence ----------
    public void loadFromDisk() throws IOException {
        snippets.clear();
        snippets.addAll(FileUtil.loadDatabase(FileUtil.DB_FILE));
        rebuildIndex();
    }

    /** Written by the main thread only. The auto-save thread writes a different file. */
    public void saveToDisk() throws IOException {
        FileUtil.writeDatabase(snippets, FileUtil.DB_FILE);
    }

    // ---------- CRUD ----------
    public void add(Snippet s) throws InvalidInputException {
        for (Snippet existing : snippets) {
            if (existing.isDuplicate(s)) {
                throw new InvalidInputException("A snippet with the same title, language and content "
                        + "already exists (#" + existing.getId() + ").");
            }
        }
        snippets.add(s);
        rebuildIndex();
    }

    public Snippet findById(int id) throws SnippetNotFoundException {
        for (Snippet s : snippets) {
            if (s.getId() == id) {
                return s;
            }
        }
        throw new SnippetNotFoundException(id);
    }

    public void delete(int id) throws SnippetNotFoundException {
        Snippet target = findById(id);
        snippets.remove(target);
        rebuildIndex();
    }

    /** Call after changing a snippet's tags so the index stays correct. */
    public void refreshIndex() {
        rebuildIndex();
    }

    /** Uses clone() to make a copy that starts a fresh revision schedule. */
    public Snippet duplicate(int id, int today) throws SnippetNotFoundException {
        Snippet original = findById(id);
        Snippet copy = original.clone();
        copy.resetSchedule(today);
        snippets.add(copy);
        rebuildIndex();
        return copy;
    }

    /** Adds imported snippets that are not duplicates. Returns how many were added. */
    public int importAll(List<Snippet> incoming) {
        int added = 0;
        for (Snippet s : incoming) {
            boolean duplicate = false;
            for (Snippet existing : snippets) {
                if (existing.isDuplicate(s)) {
                    duplicate = true;
                    break;
                }
            }
            if (!duplicate) {
                s.assignNewId();
                snippets.add(s);
                added++;
            }
        }
        rebuildIndex();
        return added;
    }

    // ---------- queries ----------
    public ArrayList<Snippet> getAll() {
        return snippets;
    }

    public int size() {
        return snippets.size();
    }

    public ArrayList<Snippet> getByTag(String tag) {
        ArrayList<Snippet> found = tagIndex.get(tag.toLowerCase());
        if (found == null) {
            return new ArrayList<>();
        }
        return found;
    }

    public HashSet<String> getUniqueTags() {
        return uniqueTags;
    }

    public HashMap<String, ArrayList<Snippet>> getTagIndex() {
        return tagIndex;
    }

    private void rebuildIndex() {
        tagIndex.clear();
        uniqueTags.clear();
        for (Snippet s : snippets) {
            for (String tag : s.getTagList()) {
                uniqueTags.add(tag);
                ArrayList<Snippet> bucket = tagIndex.get(tag);
                if (bucket == null) {
                    bucket = new ArrayList<>();
                    tagIndex.put(tag, bucket);
                }
                bucket.add(s);
            }
        }
    }
}
