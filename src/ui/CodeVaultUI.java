package ui;

import exception.InvalidInputException;
import exception.SnippetNotFoundException;
import model.CodeSnippet;
import model.CommandSnippet;
import model.NoteSnippet;
import model.RevisionLog;
import model.Snippet;
import model.User;
import service.AuthService;
import service.RevisionScheduler;
import service.SearchService;
import service.SnippetService;
import service.StatisticsService;
import util.FileUtil;
import util.ValidationUtil;

import javax.swing.BorderFactory;
import javax.swing.plaf.basic.BasicButtonUI;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/** Swing interface for CodeVault; the original console application remains available. */
public class CodeVaultUI {
    private static final Color NAVY = new Color(15, 23, 42);
    private static final Color PANEL = new Color(30, 41, 59);
    private static final Color SURFACE = new Color(248, 250, 252);
    private static final Color TEAL = new Color(13, 148, 136);
    private static final Color TEXT = new Color(30, 41, 59);
    private static final Color MUTED = new Color(100, 116, 139);

    private final SnippetService service = new SnippetService();
    private final SearchService search = new SearchService(service);
    private final RevisionScheduler scheduler = new RevisionScheduler();
    private final StatisticsService stats = new StatisticsService(service, search);
    private final SnippetTableModel tableModel = new SnippetTableModel();
    private final JTable table = new JTable(tableModel);
    private final JTextArea detail = new JTextArea();
    private final JTextField searchField = new JTextField(22);
    private final JLabel status = new JLabel("Ready");
    private final JLabel countLabel = new JLabel("0 snippets");
    private final JLabel dueLabel = new JLabel("0 due today");
    private JFrame frame;
    private int today;
    private String activeQuery = "";

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                CodeVaultUI app = new CodeVaultUI();
                if (app.startSession()) app.showWindow();
            } catch (Exception e) {
                JOptionPane.showMessageDialog(null, "Could not start CodeVault: " + e.getMessage(),
                        "Startup error", JOptionPane.ERROR_MESSAGE);
            }
        });
    }

    private boolean startSession() throws IOException {
        FileUtil.initDirectories();
        if (!authenticate()) return false;
        today = FileUtil.readDay() + 1;
        FileUtil.writeDay(today);
        service.loadFromDisk();
        return true;
    }

    private boolean authenticate() throws IOException {
        if (!FileUtil.exists(FileUtil.PASSWORD_FILE)) {
            while (true) {
                JPasswordField first = new JPasswordField(18);
                int answer = JOptionPane.showConfirmDialog(null,
                        new Object[]{"Create a master password (at least 4 characters):", first},
                        "Welcome to CodeVault", JOptionPane.OK_CANCEL_OPTION);
                if (answer != JOptionPane.OK_OPTION) return false;
                String password = new String(first.getPassword());
                if (password.length() < 4) {
                    message("Password must contain at least 4 characters.", JOptionPane.WARNING_MESSAGE);
                    continue;
                }
                JPasswordField repeat = new JPasswordField(18);
                int confirm = JOptionPane.showConfirmDialog(null,
                        new Object[]{"Repeat the master password:", repeat},
                        "Confirm password", JOptionPane.OK_CANCEL_OPTION);
                if (confirm != JOptionPane.OK_OPTION) return false;
                if (!password.equals(new String(repeat.getPassword()))) {
                    message("The passwords do not match. Please try again.", JOptionPane.WARNING_MESSAGE);
                    continue;
                }
                FileUtil.writeText(FileUtil.PASSWORD_FILE, User.encode(password));
                return true;
            }
        }

        String stored = FileUtil.readWholeFile(FileUtil.PASSWORD_FILE).trim();
        User owner = new User("owner", stored);
        for (int attempt = 1; attempt <= 3; attempt++) {
            JPasswordField field = new JPasswordField(18);
            int answer = JOptionPane.showConfirmDialog(null,
                    new Object[]{"Enter your master password:", field}, "CodeVault login",
                    JOptionPane.OK_CANCEL_OPTION);
            if (answer != JOptionPane.OK_OPTION) return false;
            if (owner.matches(new String(field.getPassword()))) return true;
            message("Incorrect password (" + attempt + " of 3).", JOptionPane.WARNING_MESSAGE);
        }
        message("Too many failed attempts.", JOptionPane.ERROR_MESSAGE);
        return false;
    }

    private void showWindow() {
        frame = new JFrame("CodeVault | Programming Knowledge Vault");
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        frame.setMinimumSize(new Dimension(980, 660));
        frame.setSize(1180, 760);
        frame.setLocationRelativeTo(null);
        frame.setLayout(new BorderLayout());
        frame.add(buildSidebar(), BorderLayout.WEST);
        frame.add(buildWorkspace(), BorderLayout.CENTER);
        frame.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override public void windowClosing(java.awt.event.WindowEvent e) { closeApp(); }
        });
        refreshTable();
        javax.swing.Timer autosave = new javax.swing.Timer(30_000, e -> {
            try {
                FileUtil.writeDatabase(new ArrayList<>(service.getAll()), FileUtil.AUTOSAVE_FILE);
                status.setText("Autosaved at " + java.time.LocalTime.now().withSecond(0).withNano(0));
            } catch (IOException ex) {
                status.setText("Autosave failed: " + ex.getMessage());
            }
        });
        autosave.start();
        frame.setVisible(true);
    }

    private JPanel buildSidebar() {
        JPanel side = new JPanel();
        side.setBackground(NAVY);
        side.setPreferredSize(new Dimension(220, 0));
        side.setBorder(BorderFactory.createEmptyBorder(25, 16, 20, 16));
        side.setLayout(new javax.swing.BoxLayout(side, javax.swing.BoxLayout.Y_AXIS));

        JLabel brand = new JLabel("◆  CodeVault");
        brand.setForeground(Color.WHITE);
        brand.setFont(new Font("Segoe UI", Font.BOLD, 23));
        brand.setBorder(BorderFactory.createEmptyBorder(4, 6, 8, 4));
        side.add(brand);
        JLabel tagline = new JLabel("Your programming library");
        tagline.setForeground(new Color(148, 163, 184));
        tagline.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        tagline.setBorder(BorderFactory.createEmptyBorder(0, 7, 28, 0));
        side.add(tagline);
        side.add(navButton("▦   Overview", () -> { activeQuery = ""; searchField.setText(""); refreshTable(); }));
        side.add(navButton("⌕   Search snippets", () -> searchField.requestFocusInWindow()));
        side.add(navButton("↻   Review due", this::reviewDue));
        side.add(navButton("▤   Statistics", this::showStatistics));
        side.add(javax.swing.Box.createVerticalStrut(22));
        JLabel tools = new JLabel("TOOLS");
        tools.setForeground(new Color(100, 116, 139));
        tools.setFont(new Font("Segoe UI", Font.BOLD, 11));
        tools.setBorder(BorderFactory.createEmptyBorder(12, 8, 8, 0));
        side.add(tools);
        side.add(navButton("⇧   Export snippets", this::exportSnippets));
        side.add(navButton("▣   Create backup", this::createBackup));
        side.add(javax.swing.Box.createVerticalGlue());
        JLabel session = new JLabel("SESSION DAY " + today);
        session.setForeground(new Color(148, 163, 184));
        session.setFont(new Font("Segoe UI", Font.BOLD, 11));
        session.setBorder(BorderFactory.createEmptyBorder(10, 8, 4, 0));
        side.add(session);
        return side;
    }

    private JButton navButton(String text, Runnable action) {
        JButton button = new JButton(text);
        button.setHorizontalAlignment(SwingConstants.LEFT);
        button.setForeground(new Color(226, 232, 240));
        button.setBackground(NAVY);
        button.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        button.setBorder(BorderFactory.createEmptyBorder(12, 10, 12, 8));
        button.setUI(new BasicButtonUI());
        button.setOpaque(true);
        button.setContentAreaFilled(true);
        button.setFocusPainted(false);
        button.setAlignmentX(Component.LEFT_ALIGNMENT);
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));
        button.addActionListener(e -> action.run());
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseEntered(java.awt.event.MouseEvent e) { button.setBackground(PANEL); }
            @Override public void mouseExited(java.awt.event.MouseEvent e) { button.setBackground(NAVY); }
        });
        return button;
    }

    private JPanel buildWorkspace() {
        JPanel root = new JPanel(new BorderLayout(0, 16));
        root.setBackground(SURFACE);
        root.setBorder(BorderFactory.createEmptyBorder(24, 28, 14, 28));

        JPanel top = new JPanel(new BorderLayout(10, 12));
        top.setOpaque(false);
        JPanel title = new JPanel();
        title.setOpaque(false);
        title.setLayout(new javax.swing.BoxLayout(title, javax.swing.BoxLayout.Y_AXIS));
        JLabel heading = new JLabel("Your knowledge vault");
        heading.setFont(new Font("Segoe UI", Font.BOLD, 27));
        heading.setForeground(TEXT);
        JLabel subtitle = new JLabel("Save useful code, notes and commands. Keep learning as you go.");
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        subtitle.setForeground(MUTED);
        title.add(heading); title.add(javax.swing.Box.createVerticalStrut(3)); title.add(subtitle);
        top.add(title, BorderLayout.NORTH);

        JPanel cards = new JPanel(new GridBagLayout()); cards.setOpaque(false);
        cards.add(metricCard("TOTAL SNIPPETS", countLabel), constraints(0));
        cards.add(metricCard("REVIEW QUEUE", dueLabel), constraints(1));
        top.add(cards, BorderLayout.CENTER);
        root.add(top, BorderLayout.NORTH);

        JPanel tablePanel = new JPanel(new BorderLayout(0, 10));
        tablePanel.setBackground(Color.WHITE);
        tablePanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(226, 232, 240)),
                BorderFactory.createEmptyBorder(15, 16, 14, 16)));

        JPanel toolbar = new JPanel(new BorderLayout(8, 0)); toolbar.setOpaque(false);
        JPanel searchBox = new JPanel(new BorderLayout(7, 0)); searchBox.setOpaque(false);
        searchField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        searchField.setToolTipText("Search title, language, tags or snippet content");
        searchField.addActionListener(e -> applySearch());
        JButton find = actionButton("Search", TEAL); find.addActionListener(e -> applySearch());
        JButton clear = actionButton("Clear", new Color(71, 85, 105)); clear.addActionListener(e -> { searchField.setText(""); activeQuery = ""; refreshTable(); });
        searchBox.add(searchField, BorderLayout.CENTER);
        JPanel searchActions = new JPanel(); searchActions.setOpaque(false); searchActions.add(find); searchActions.add(clear);
        searchBox.add(searchActions, BorderLayout.EAST);
        toolbar.add(searchBox, BorderLayout.WEST);

        JPanel actions = new JPanel(); actions.setOpaque(false);
        JButton add = actionButton("+ Add snippet", TEAL); add.addActionListener(e -> editDialog(null));
        JButton edit = actionButton("Edit", new Color(71, 85, 105)); edit.addActionListener(e -> selectedSnippet().ifPresent(this::editDialog));
        JButton duplicate = actionButton("Duplicate", new Color(71, 85, 105)); duplicate.addActionListener(e -> duplicateSelected());
        JButton delete = actionButton("Delete", new Color(185, 60, 60)); delete.addActionListener(e -> deleteSelected());
        actions.add(add); actions.add(edit); actions.add(duplicate); actions.add(delete);
        toolbar.add(actions, BorderLayout.EAST);
        tablePanel.add(toolbar, BorderLayout.NORTH);

        table.setRowHeight(36);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        table.setForeground(TEXT);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setShowVerticalLines(false);
        table.setGridColor(new Color(241, 245, 249));
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        table.getTableHeader().setForeground(MUTED);
        table.getTableHeader().setBackground(new Color(248, 250, 252));
        table.getTableHeader().setPreferredSize(new Dimension(0, 34));
        table.setSelectionBackground(new Color(204, 251, 241));
        table.setSelectionForeground(TEXT);
        table.getSelectionModel().addListSelectionListener(e -> { if (!e.getValueIsAdjusting()) showSelectedDetail(); });
        JScrollPane tableScroll = new JScrollPane(table);
        tableScroll.setBorder(BorderFactory.createEmptyBorder());

        detail.setEditable(false);
        detail.setLineWrap(true); detail.setWrapStyleWord(true);
        detail.setFont(new Font("Consolas", Font.PLAIN, 13));
        detail.setForeground(TEXT); detail.setBackground(new Color(248, 250, 252));
        detail.setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 14));
        detail.setText("Select a snippet to preview its content.");
        JScrollPane detailScroll = new JScrollPane(detail);
        detailScroll.setBorder(BorderFactory.createTitledBorder("Snippet preview"));
        detailScroll.setPreferredSize(new Dimension(0, 150));
        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, tableScroll, detailScroll);
        split.setResizeWeight(0.72); split.setBorder(null); split.setDividerSize(7);
        tablePanel.add(split, BorderLayout.CENTER);
        root.add(tablePanel, BorderLayout.CENTER);

        JPanel footer = new JPanel(new BorderLayout()); footer.setOpaque(false);
        status.setFont(new Font("Segoe UI", Font.PLAIN, 12)); status.setForeground(MUTED);
        footer.add(status, BorderLayout.WEST);
        JLabel save = new JLabel("Changes save automatically when you edit");
        save.setFont(new Font("Segoe UI", Font.PLAIN, 12)); save.setForeground(MUTED);
        footer.add(save, BorderLayout.EAST);
        root.add(footer, BorderLayout.SOUTH);
        return root;
    }

    private GridBagConstraints constraints(int column) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = column; c.gridy = 0; c.weightx = 1; c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(14, column == 0 ? 0 : 8, 0, column == 0 ? 8 : 0);
        return c;
    }

    private JPanel metricCard(String caption, JLabel value) {
        JPanel card = new JPanel(new BorderLayout(0, 6));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(new Color(226, 232, 240)),
                BorderFactory.createEmptyBorder(12, 16, 12, 16)));
        JLabel cap = new JLabel(caption); cap.setFont(new Font("Segoe UI", Font.BOLD, 11)); cap.setForeground(MUTED);
        value.setFont(new Font("Segoe UI", Font.BOLD, 21)); value.setForeground(TEXT);
        card.add(cap, BorderLayout.NORTH); card.add(value, BorderLayout.CENTER);
        return card;
    }

    private JButton actionButton(String text, Color color) {
        JButton button = new JButton(text);
        button.setFont(new Font("Segoe UI", Font.BOLD, 12));
        button.setForeground(Color.WHITE); button.setBackground(color);
        button.setBorder(BorderFactory.createEmptyBorder(9, 13, 9, 13));
        button.setUI(new BasicButtonUI());
        button.setFocusPainted(false); button.setOpaque(true);
        button.setContentAreaFilled(true);
        return button;
    }

    private void applySearch() {
        activeQuery = searchField.getText().trim();
        refreshTable();
        status.setText(activeQuery.isEmpty() ? "Showing all snippets" : "Search results for ‘" + activeQuery + "’");
    }

    private List<Snippet> visibleSnippets() {
        if (activeQuery.isEmpty()) return new ArrayList<>(service.getAll());
        return search.byKeyword(activeQuery);
    }

    private void refreshTable() {
        List<Snippet> items = visibleSnippets();
        tableModel.setItems(items);
        countLabel.setText(service.size() + (service.size() == 1 ? " snippet" : " snippets"));
        dueLabel.setText(scheduler.countDue(service.getAll(), today) + " due today");
        if (items.isEmpty()) detail.setText("No snippets found. Add a snippet or change your search.");
        else if (table.getRowCount() > 0) table.setRowSelectionInterval(0, 0);
    }

    private void showSelectedDetail() {
        selectedSnippet().ifPresent(s -> detail.setText(
                "#" + s.getId() + "  " + s.getTitle() + "  [" + s.getType() + "]\n"
                        + "Language: " + s.getLanguage() + "    Difficulty: " + s.getDifficulty() + "/5\n"
                        + "Tags: " + (s.getTags().isEmpty() ? "—" : s.getTags()) + "\n"
                        + "Next review: day " + s.getNextRevisionDay() + "\n\n" + s.getBody()));
    }

    private java.util.Optional<Snippet> selectedSnippet() {
        int row = table.getSelectedRow();
        if (row < 0) return java.util.Optional.empty();
        return java.util.Optional.ofNullable(tableModel.getSnippet(table.convertRowIndexToModel(row)));
    }

    private void editDialog(Snippet existing) {
        JTextField title = new JTextField(existing == null ? "" : existing.getTitle(), 26);
        JComboBox<String> type = new JComboBox<>(new String[]{"Code", "Note", "Command"});
        JTextField language = new JTextField(existing == null ? "java" : existing.getLanguage(), 26);
        JTextField tags = new JTextField(existing == null ? "" : existing.getTags(), 26);
        JComboBox<Integer> difficulty = new JComboBox<>(new Integer[]{1, 2, 3, 4, 5});
        JTextArea body = new JTextArea(existing == null ? "" : existing.getBody(), 12, 32);
        body.setFont(new Font("Consolas", Font.PLAIN, 13));
        body.setLineWrap(false);
        if (existing != null) {
            type.setSelectedItem(existing.getType().equals("CODE") ? "Code" : existing.getType().equals("NOTE") ? "Note" : "Command");
            type.setEnabled(false);
            difficulty.setSelectedItem(existing.getDifficulty());
        }
        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints(); c.insets = new Insets(5, 6, 5, 6); c.fill = GridBagConstraints.HORIZONTAL;
        addFormRow(form, c, 0, "Type", type); addFormRow(form, c, 1, "Title", title);
        addFormRow(form, c, 2, "Language", language); addFormRow(form, c, 3, "Tags", tags);
        addFormRow(form, c, 4, "Difficulty", difficulty);
        c.gridx = 0; c.gridy = 5; c.anchor = GridBagConstraints.NORTHWEST; c.fill = GridBagConstraints.NONE;
        form.add(new JLabel("Content"), c);
        c.gridx = 1; c.weightx = 1; c.weighty = 1; c.fill = GridBagConstraints.BOTH;
        form.add(new JScrollPane(body), c);
        int answer = JOptionPane.showConfirmDialog(frame, form,
                existing == null ? "Add a snippet" : "Edit snippet", JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE);
        if (answer != JOptionPane.OK_OPTION) return;
        try {
            String cleanTitle = ValidationUtil.requireText(title.getText(), "Title");
            String cleanTags = ValidationUtil.parseTags(tags.getText());
            int level = (Integer) difficulty.getSelectedItem();
            String cleanBody = ValidationUtil.requireText(body.getText(), "Content");
            if (existing == null) {
                String lang = type.getSelectedItem().equals("Note") ? "general"
                        : ValidationUtil.requireText(language.getText(), "Language").toLowerCase();
                Snippet snippet;
                switch ((String) type.getSelectedItem()) {
                    case "Code": snippet = new CodeSnippet(cleanTitle, lang, cleanTags, cleanBody, level, today); break;
                    case "Note": snippet = new NoteSnippet(cleanTitle, lang, cleanTags, cleanBody, level, today); break;
                    default: snippet = new CommandSnippet(cleanTitle, lang, cleanTags, cleanBody, level, today);
                }
                service.add(snippet);
                status.setText("Added ‘" + cleanTitle + "’");
            } else {
                existing.setTitle(cleanTitle);
                existing.setTags(cleanTags);
                existing.setDifficulty(level);
                existing.setBody(cleanBody);
                if (!existing.getType().equals("NOTE"))
                    existing.setLanguage(ValidationUtil.requireText(language.getText(), "Language").toLowerCase());
                service.refreshIndex();
                status.setText("Updated ‘" + cleanTitle + "’");
            }
            save(); refreshTable();
        } catch (InvalidInputException | IOException e) {
            message(e.getMessage(), JOptionPane.WARNING_MESSAGE);
        }
    }

    private void addFormRow(JPanel form, GridBagConstraints c, int row, String label, Component input) {
        c.gridy = row; c.gridx = 0; c.weightx = 0; c.weighty = 0; c.fill = GridBagConstraints.HORIZONTAL;
        form.add(new JLabel(label), c);
        c.gridx = 1; c.weightx = 1;
        form.add(input, c);
    }

    private void duplicateSelected() {
        selectedSnippet().ifPresent(s -> {
            try {
                Snippet copy = service.duplicate(s.getId(), today);
                save(); refreshTable(); status.setText("Created copy ‘" + copy.getTitle() + "’");
            } catch (SnippetNotFoundException | IOException ex) { message(ex.getMessage(), JOptionPane.ERROR_MESSAGE); }
        });
    }

    private void deleteSelected() {
        selectedSnippet().ifPresent(s -> {
            int answer = JOptionPane.showConfirmDialog(frame, "Delete ‘" + s.getTitle() + "’?",
                    "Delete snippet", JOptionPane.YES_NO_OPTION);
            if (answer != JOptionPane.YES_OPTION) return;
            try { service.delete(s.getId()); save(); refreshTable(); status.setText("Snippet deleted"); }
            catch (SnippetNotFoundException | IOException ex) { message(ex.getMessage(), JOptionPane.ERROR_MESSAGE); }
        });
    }

    private void reviewDue() {
        scheduler.rebuild(service.getAll(), today);
        int due = scheduler.queueSize();
        if (due == 0) { message("Nothing is due for review today. Nice work!", JOptionPane.INFORMATION_MESSAGE); return; }
        int revised = 0;
        Snippet snippet;
        while ((snippet = scheduler.nextDue()) != null) {
            JTextArea preview = new JTextArea("Try to remember this snippet, then reveal it:\n\n"
                    + "#" + snippet.getId() + "  " + snippet.getTitle() + "  [" + snippet.getLanguage() + "]");
            preview.setEditable(false); preview.setFont(new Font("Segoe UI", Font.PLAIN, 15));
            int reveal = JOptionPane.showConfirmDialog(frame, preview, "Review " + (revised + 1) + " of " + due,
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (reveal != JOptionPane.OK_OPTION) break;
            JTextArea answer = new JTextArea(snippet.getBody()); answer.setEditable(false);
            answer.setLineWrap(true); answer.setWrapStyleWord(true); answer.setRows(10); answer.setColumns(42);
            int rating = JOptionPane.showOptionDialog(frame, new JScrollPane(answer), "How well did you recall it?",
                    JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null,
                    new Object[]{"Forgot", "Hard", "Easy", "Stop"}, "Easy");
            if (rating < 0 || rating == 3) break;
            try {
                scheduler.recordRecall(snippet, rating + 1, today);
                save(); revised++;
            } catch (IOException e) { message("Could not save revision: " + e.getMessage(), JOptionPane.ERROR_MESSAGE); break; }
        }
        refreshTable();
        status.setText("Review session complete: " + revised + " revised");
        message("You reviewed " + revised + " snippet(s).", JOptionPane.INFORMATION_MESSAGE);
    }

    private void showStatistics() {
        try {
            String report = stats.buildReport(FileUtil.readAllLogs(), today);
            JTextArea view = new JTextArea(report, 20, 45); view.setEditable(false);
            view.setFont(new Font("Consolas", Font.PLAIN, 13));
            JOptionPane.showMessageDialog(frame, new JScrollPane(view), "CodeVault statistics", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException e) { message("Could not load statistics: " + e.getMessage(), JOptionPane.ERROR_MESSAGE); }
    }

    private void exportSnippets() {
        JFileChooser chooser = new JFileChooser(); chooser.setSelectedFile(new File("codesnippets.txt"));
        if (chooser.showSaveDialog(frame) != JFileChooser.APPROVE_OPTION) return;
        File file = chooser.getSelectedFile();
        try { FileUtil.exportText(service.getAll(), file.getAbsolutePath()); status.setText("Exported to " + file.getName()); }
        catch (IOException e) { message("Could not export: " + e.getMessage(), JOptionPane.ERROR_MESSAGE); }
    }

    private void createBackup() {
        try {
            save();
            String target = FileUtil.BACKUP_DIR + File.separator + "snippets_day" + today + ".bak";
            long bytes = FileUtil.binaryCopy(FileUtil.DB_FILE, target);
            status.setText("Backup created: " + target + " (" + bytes + " bytes)");
            message("Backup created successfully.\n" + target, JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException e) { message("Could not create backup: " + e.getMessage(), JOptionPane.ERROR_MESSAGE); }
    }

    private void save() throws IOException { service.saveToDisk(); }

    private void closeApp() {
        try { save(); frame.dispose(); }
        catch (IOException e) {
            int answer = JOptionPane.showConfirmDialog(frame, "Could not save: " + e.getMessage() + "\nExit anyway?",
                    "Save error", JOptionPane.YES_NO_OPTION, JOptionPane.ERROR_MESSAGE);
            if (answer == JOptionPane.YES_OPTION) frame.dispose();
        }
    }

    private void message(String text, int type) {
        JOptionPane.showMessageDialog(frame, text, "CodeVault", type);
    }

    private static class SnippetTableModel extends AbstractTableModel {
        private final String[] columns = {"ID", "TYPE", "TITLE", "LANGUAGE", "LEVEL", "TAGS", "NEXT REVIEW"};
        private List<Snippet> items = new ArrayList<>();
        void setItems(List<Snippet> items) { this.items = items; fireTableDataChanged(); }
        Snippet getSnippet(int row) { return row >= 0 && row < items.size() ? items.get(row) : null; }
        @Override public int getRowCount() { return items.size(); }
        @Override public int getColumnCount() { return columns.length; }
        @Override public String getColumnName(int col) { return columns[col]; }
        @Override public Object getValueAt(int row, int col) {
            Snippet s = items.get(row);
            switch (col) {
                case 0: return "#" + s.getId();
                case 1: return s.getType();
                case 2: return s.getTitle();
                case 3: return s.getLanguage();
                case 4: return s.getDifficulty() + "/5";
                case 5: return s.getTags().isEmpty() ? "—" : s.getTags();
                default: return "Day " + s.getNextRevisionDay();
            }
        }
    }
}
