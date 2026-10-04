package ixoras.converter;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

/**
 * The double-click window (owner, Decided 2026-10-04): lists the worlds it finds in the default Minecraft saves folder
 * (plus any folder the player adds), says which still use the old name, converts the selected one into a new
 * "(converted)" folder, and says what to do next in plain words. The original is never changed.
 *
 * <p>Plain Swing, which ships with the JDK: no library. WRITTEN, COMPILED, NEVER SEEN ON A SCREEN: this session had no
 * display, so the layout and every button are unverified. The conversion itself is {@link Main#convertWorld}, which
 * the self-test covers.
 */
final class Gui {
    private static final String OLD = "horsegenetics";
    private static final String NEW = "ixoras_horses";

    private final JFrame frame = new JFrame("Ixora's Horse Overhaul - world converter");
    private final DefaultListModel<Path> model = new DefaultListModel<>();
    private final JList<Path> list = new JList<>(model);
    private final JTextArea log = new JTextArea(14, 70);
    private final JButton convert = new JButton("Convert the selected world");
    private final JCheckBox keys = new JCheckBox("Also move my key bindings (edits options.txt; the original is kept as options.txt.before-ixoras)");

    static void open() {
        SwingUtilities.invokeLater(() -> new Gui().show());
    }

    private void show() {
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout(8, 8));
        JLabel intro = new JLabel("<html><b>Moving a world from Horse Genetics to Ixora's Horse Overhaul.</b><br>"
                + "Pick a world below (or add a folder). Close Minecraft first. Your original world is never changed: "
                + "a new folder called \"&lt;name&gt; (converted)\" is made beside it, and you open that one with the new mod.</html>");
        intro.setBorder(BorderFactory.createEmptyBorder(8, 8, 0, 8));
        frame.add(intro, BorderLayout.NORTH);

        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setCellRenderer((l, value, i, sel, focus) -> {
            JLabel c = new JLabel(value.getFileName() + "   (" + value.getParent() + ")");
            c.setOpaque(true);
            c.setBackground(sel ? l.getSelectionBackground() : l.getBackground());
            c.setForeground(sel ? l.getSelectionForeground() : l.getForeground());
            c.setBorder(BorderFactory.createEmptyBorder(2, 6, 2, 6));
            return c;
        });
        JScrollPane listPane = new JScrollPane(list);
        listPane.setPreferredSize(new Dimension(640, 150));
        listPane.setBorder(BorderFactory.createTitledBorder("Worlds found"));

        log.setEditable(false);
        log.setLineWrap(true);
        log.setWrapStyleWord(true);
        JScrollPane logPane = new JScrollPane(log);
        logPane.setBorder(BorderFactory.createTitledBorder("What happened"));

        JPanel center = new JPanel(new BorderLayout(6, 6));
        center.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
        center.add(listPane, BorderLayout.NORTH);
        center.add(logPane, BorderLayout.CENTER);
        frame.add(center, BorderLayout.CENTER);

        JButton add = new JButton("Add a folder...");
        JButton check = new JButton("Check it first (changes nothing)");
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT));
        buttons.add(add);
        buttons.add(check);
        buttons.add(convert);
        JPanel south = new JPanel(new BorderLayout());
        south.add(keys, BorderLayout.NORTH);
        south.add(buttons, BorderLayout.SOUTH);
        south.setBorder(BorderFactory.createEmptyBorder(0, 8, 8, 8));
        frame.add(south, BorderLayout.SOUTH);

        add.addActionListener(e -> chooseFolder());
        check.addActionListener(e -> run(true));
        convert.addActionListener(e -> run(false));

        for (Path p : defaultSaves()) {
            model.addElement(p);
        }
        if (model.isEmpty()) {
            say("No worlds were found in the usual Minecraft folder. Use \"Add a folder...\" to pick one (a server's world folder works too).");
        } else {
            say("Found " + model.size() + " world(s). Select one, then press \"Check it first\" or \"Convert\".");
            list.setSelectedIndex(0);
        }
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    /** The default saves folder for this operating system, and the worlds in it. */
    static List<Path> defaultSaves() {
        List<Path> out = new ArrayList<>();
        String os = System.getProperty("os.name", "").toLowerCase();
        String home = System.getProperty("user.home", ".");
        Path saves;
        if (os.contains("win")) {
            String appdata = System.getenv("APPDATA");
            saves = Path.of(appdata != null ? appdata : home, ".minecraft", "saves");
        } else if (os.contains("mac")) {
            saves = Path.of(home, "Library", "Application Support", "minecraft", "saves");
        } else {
            saves = Path.of(home, ".minecraft", "saves");
        }
        if (Files.isDirectory(saves)) {
            try (Stream<Path> s = Files.list(saves)) {
                s.filter(WorldConverter::looksLikeWorld).sorted().forEach(out::add);
            } catch (IOException e) {
                // an unreadable saves folder just means an empty list; the player can add a folder by hand
            }
        }
        return out;
    }

    private void chooseFolder() {
        JFileChooser fc = new JFileChooser();
        fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        fc.setDialogTitle("Pick a world folder (the one that contains level.dat)");
        if (fc.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION) {
            Path p = fc.getSelectedFile().toPath();
            if (!WorldConverter.looksLikeWorld(p)) {
                say("That folder has no level.dat, so it is not a Minecraft world folder.");
                return;
            }
            model.addElement(p);
            list.setSelectedIndex(model.size() - 1);
        }
    }

    private void run(boolean dry) {
        Path world = list.getSelectedValue();
        if (world == null) {
            say("Select a world first.");
            return;
        }
        if (!dry && JOptionPane.showConfirmDialog(frame,
                "Is Minecraft closed? Convert \"" + world.getFileName() + "\" into a new folder beside it?\nYour original is not changed.",
                "Convert", JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) {
            return;
        }
        convert.setEnabled(false);
        log.setText("");
        boolean keyBindings = keys.isSelected() && !dry;
        new SwingWorker<Integer, Void>() {
            private String text = "";

            @Override
            protected Integer doInBackground() throws Exception {
                ByteArrayOutputStream bo = new ByteArrayOutputStream();
                PrintStream ps = new PrintStream(bo, true, StandardCharsets.UTF_8);
                int code = Main.convertWorld(world, OLD, NEW, dry, ps);
                if (code == 0 && keyBindings) {
                    Path options = world.toAbsolutePath().getParent() == null ? null
                            : world.toAbsolutePath().getParent().getParent() == null ? null
                            : world.toAbsolutePath().getParent().getParent().resolve("options.txt");
                    if (options != null && Files.isRegularFile(options)) {
                        OptionsConverter.convert(options, OLD, NEW, ps);
                    } else {
                        ps.println("Could not find options.txt next to the saves folder; key bindings were not changed.");
                    }
                }
                text = bo.toString(StandardCharsets.UTF_8);
                return code;
            }

            @Override
            protected void done() {
                convert.setEnabled(true);
                try {
                    int code = get();
                    log.setText(text);
                    log.append(code == 0
                            ? (dry ? "\nChecked. Nothing was changed.\n" : "\nAll done. Open the \"(converted)\" world with the new mod.\n")
                            : "\nSomething went wrong. Your original world was not changed. Keep this text if you ask for help.\n");
                } catch (Exception ex) {
                    log.setText("Unexpected error: " + ex + "\nYour original world was not changed.");
                }
            }
        }.execute();
    }

    private void say(String s) {
        log.append(s + "\n");
    }
}
