package net.runelite.client.plugins.playerlogger;

import java.awt.BorderLayout;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import javax.swing.*;
import net.runelite.client.ui.PluginPanel;

public class PlayerLoggerPanel extends PluginPanel {
    private final JList<Row> list = new JList<Row>() {
        @Override
        public String getToolTipText(MouseEvent event) {
            int index = locationToIndex(event.getPoint());
            if (index < 0 || !getCellBounds(index, index).contains(event.getPoint())) {
                return null;
            }
            Row row = getModel().getElementAt(index);
            return row.visit.heading() + " | " + row.player.summary(row.visit.world);
        }
    };
    private final JLabel status = new JLabel(" ");
    private List<WorldVisit> visits = Collections.emptyList();

    PlayerLoggerPanel(Runnable clearHistory) {
        super(false);
        setLayout(new BorderLayout());
        JButton clear = new JButton("Clear Logs");
        clear.addActionListener(e -> clearHistory.run());
        add(clear, BorderLayout.NORTH);
        ToolTipManager.sharedInstance().registerComponent(list);
        add(new JScrollPane(list, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
            ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED), BorderLayout.CENTER);
        add(status, BorderLayout.SOUTH);
        list.addMouseListener(new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) { showMenu(e); }
            @Override public void mouseReleased(MouseEvent e) { showMenu(e); }
        });
    }

    void showVisits(List<WorldVisit> history) {
        visits = history;
        DefaultListModel<Row> rows = new DefaultListModel<>();
        for (int i = visits.size() - 1; i >= 0; i--) {
            WorldVisit visit = visits.get(i);
            List<PlayerDetails> players = new ArrayList<>(visit.players.values());
            for (int j = players.size() - 1; j >= 0; j--) {
                rows.addElement(new Row(visit, players.get(j)));
            }
        }
        list.setModel(rows);
        status.setText(visits.isEmpty() ? "No visits recorded" : visits.size() + " / 25 visits");
    }

    void showStorageError() {
        status.setText("History unavailable. See client log.");
    }

    private void showMenu(MouseEvent event) {
        if (!event.isPopupTrigger()) {
            return;
        }
        int index = list.locationToIndex(event.getPoint());
        if (index < 0 || !list.getCellBounds(index, index).contains(event.getPoint())) {
            return;
        }
        list.setSelectedIndex(index);
        WorldVisit visit = list.getSelectedValue().visit;
        JPopupMenu menu = new JPopupMenu();
        addCopy(menu, "Copy Usernames from This Visit", Collections.singletonList(visit), false, false);
        addCopy(menu, "Copy All Info from This Visit", Collections.singletonList(visit), true, false);
        menu.addSeparator();
        List<WorldVisit> world = visits.stream().filter(v -> v.world == visit.world).collect(Collectors.toList());
        addCopy(menu, "Copy Usernames from World", world, false, false);
        addCopy(menu, "Copy Usernames from World Before Death", world, false, true);
        addCopy(menu, "Copy All Info from World", world, true, false);
        addCopy(menu, "Copy All Info from World Before Death", world, true, true);
        menu.show(list, event.getX(), event.getY());
    }

    private void addCopy(JPopupMenu menu, String label, List<WorldVisit> selected, boolean full, boolean beforeDeath) {
        JMenuItem item = new JMenuItem(label);
        item.addActionListener(e -> {
            long cutoff = selected.stream().mapToLong(v -> v.lastDeath).max().orElse(0);
            List<String> lines = new ArrayList<>();
            for (WorldVisit visit : selected) {
                for (PlayerDetails player : visit.players.values()) {
                    if (!beforeDeath || cutoff == 0 || player.firstSeen <= cutoff) {
                        lines.add(full ? visit.heading() + " | " + player.summary(visit.world) : player.name);
                    }
                }
            }
            Collections.reverse(lines);
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(
                new StringSelection(String.join(System.lineSeparator(), lines)), null);
        });
        menu.add(item);
    }

    private static final class Row {
        final WorldVisit visit;
        final PlayerDetails player;
        final String text;

        Row(WorldVisit visit, PlayerDetails player) {
            this.visit = visit;
            this.player = player;
            text = player.name + ":" + player.combatLevel + ":" + visit.world + ":"
                + player.x + "," + player.y + "," + player.plane + ":" + player.time();
        }

        @Override
        public String toString() {
            return text;
        }
    }
}
