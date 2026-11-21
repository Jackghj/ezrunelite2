package net.runelite.client.plugins.yama;

import com.google.inject.Inject;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.util.*;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.Perspective;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;

@Singleton
public class YamaAoeWarningOverlay extends Overlay
{
    private static final int TOTAL_TICKS = 5;
    private static final int PATTERN_SIZE = 3; // Always 3 boulders per pattern

    private final Client client;
    private final YamaPlugin plugin;
    private final YamaConfig config;

    @Inject
    public YamaAoeWarningOverlay(
            Client client,
            YamaPlugin plugin,
            YamaConfig config)
    {
        this.client = client;
        this.plugin = plugin;
        this.config = config;
        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.ABOVE_SCENE);
        setPriority(OverlayPriority.HIGH);
    }

    @Override
    public Dimension render(Graphics2D g)
    {
        Set<YamaPlugin.AoeBomb> bombs = plugin.getAoeBombs();
        int nowTick = client.getTickCount();

        // Convert to list for easier manipulation
        List<YamaPlugin.AoeBomb> activeBombs = new ArrayList<>();

        // Remove expired bombs
        for (Iterator<YamaPlugin.AoeBomb> it = bombs.iterator(); it.hasNext();)
        {
            YamaPlugin.AoeBomb bomb = it.next();
            int age = nowTick - bomb.getStartTick();
            if (age > TOTAL_TICKS)
            {
                it.remove();
                continue;
            }
            activeBombs.add(bomb);
        }

        // If there are no bombs, nothing to render
        if (activeBombs.isEmpty())
        {
            return null;
        }

        // Convert bombs to world points
        Map<YamaPlugin.AoeBomb, WorldPoint> bombLocations = new HashMap<>();
        for (YamaPlugin.AoeBomb bomb : activeBombs)
        {
            WorldPoint wp = WorldPoint.fromLocal(client, bomb.getLoc());
            bombLocations.put(bomb, wp);
        }

        // Group bombs into sets of 3 if possible
        List<List<YamaPlugin.AoeBomb>> bombGroups = new ArrayList<>();
        Set<YamaPlugin.AoeBomb> processedBombs = new HashSet<>();

        // Split into groups based on tick start time
        Map<Integer, List<YamaPlugin.AoeBomb>> bombsByTick = new HashMap<>();
        for (YamaPlugin.AoeBomb bomb : activeBombs)
        {
            bombsByTick.computeIfAbsent(bomb.getStartTick(), k -> new ArrayList<>()).add(bomb);
        }

        // Process each tick group separately
        for (List<YamaPlugin.AoeBomb> tickGroup : bombsByTick.values())
        {
            // If we have exactly 3 bombs in this tick, they form a pattern
            if (tickGroup.size() == PATTERN_SIZE)
            {
                bombGroups.add(new ArrayList<>(tickGroup));
                processedBombs.addAll(tickGroup);
            }
            // Try to find patterns of 3 bombs if we have more than 3
            else if (tickGroup.size() > PATTERN_SIZE)
            {
                // Try to find groups of 3 bombs that form horizontal, vertical, or diagonal patterns
                List<YamaPlugin.AoeBomb> remainingBombs = new ArrayList<>(tickGroup);
                remainingBombs.removeAll(processedBombs);

                while (remainingBombs.size() >= 3) {
                    boolean foundPattern = false;

                    // Try each possible combination of 3 bombs
                    for (int i = 0; i < remainingBombs.size() - 2 && !foundPattern; i++) {
                        for (int j = i + 1; j < remainingBombs.size() - 1 && !foundPattern; j++) {
                            for (int k = j + 1; k < remainingBombs.size() && !foundPattern; k++) {
                                YamaPlugin.AoeBomb a = remainingBombs.get(i);
                                YamaPlugin.AoeBomb b = remainingBombs.get(j);
                                YamaPlugin.AoeBomb c = remainingBombs.get(k);

                                // Check if these 3 bombs form a pattern
                                if (isHorizontalPattern(a, b, c, bombLocations) ||
                                        isVerticalPattern(a, b, c, bombLocations) ||
                                        isDiagonalPattern(a, b, c, bombLocations)) {

                                    List<YamaPlugin.AoeBomb> group = new ArrayList<>();
                                    group.add(a);
                                    group.add(b);
                                    group.add(c);
                                    bombGroups.add(group);
                                    processedBombs.addAll(group);
                                    remainingBombs.removeAll(group);
                                    foundPattern = true;
                                }
                            }
                        }
                    }

                    // If no pattern found, exit the loop
                    if (!foundPattern) {
                        break;
                    }
                }
            }
        }

        // Add any remaining bombs as individual entities
        for (YamaPlugin.AoeBomb bomb : activeBombs)
        {
            if (!processedBombs.contains(bomb))
            {
                List<YamaPlugin.AoeBomb> singleBombGroup = new ArrayList<>();
                singleBombGroup.add(bomb);
                bombGroups.add(singleBombGroup);
            }
        }

        // Render each group
        for (List<YamaPlugin.AoeBomb> group : bombGroups)
        {
            renderBombGroup(g, group, bombLocations, nowTick);
        }

        return null;
    }

    private boolean isHorizontalPattern(YamaPlugin.AoeBomb a, YamaPlugin.AoeBomb b, YamaPlugin.AoeBomb c, Map<YamaPlugin.AoeBomb, WorldPoint> locations)
    {
        WorldPoint wpA = locations.get(a);
        WorldPoint wpB = locations.get(b);
        WorldPoint wpC = locations.get(c);
        return wpA.getY() == wpB.getY() && wpB.getY() == wpC.getY();
    }

    private boolean isVerticalPattern(YamaPlugin.AoeBomb a, YamaPlugin.AoeBomb b, YamaPlugin.AoeBomb c, Map<YamaPlugin.AoeBomb, WorldPoint> locations)
    {
        WorldPoint wpA = locations.get(a);
        WorldPoint wpB = locations.get(b);
        WorldPoint wpC = locations.get(c);
        return wpA.getX() == wpB.getX() && wpB.getX() == wpC.getX();
    }

    private boolean isDiagonalPattern(YamaPlugin.AoeBomb a, YamaPlugin.AoeBomb b, YamaPlugin.AoeBomb c, Map<YamaPlugin.AoeBomb, WorldPoint> locations)
    {
        WorldPoint wpA = locations.get(a);
        WorldPoint wpB = locations.get(b);
        WorldPoint wpC = locations.get(c);

        List<WorldPoint> points = Arrays.asList(wpA, wpB, wpC);
        points.sort(Comparator.comparing(WorldPoint::getX).thenComparing(WorldPoint::getY));

        int dx1 = points.get(1).getX() - points.get(0).getX();
        int dy1 = points.get(1).getY() - points.get(0).getY();
        int dx2 = points.get(2).getX() - points.get(1).getX();
        int dy2 = points.get(2).getY() - points.get(1).getY();

        return Math.abs(dx1) == Math.abs(dy1) && dx1 == dx2 && dy1 == dy2 && dx1 != 0;
    }

    private void renderBombGroup(Graphics2D g, List<YamaPlugin.AoeBomb> group, Map<YamaPlugin.AoeBomb, WorldPoint> locations, int nowTick)
    {
        // If it's just a single bomb, render it individually
        if (group.size() == 1)
        {
            renderSingleBomb(g, group.get(0), nowTick);
            return;
        }

        // Get the pattern type
        boolean isHorizontal = isHorizontalPattern(group.get(0), group.get(1), group.get(2), locations);
        boolean isVertical = isVerticalPattern(group.get(0), group.get(1), group.get(2), locations);
        boolean isDiagonal = isDiagonalPattern(group.get(0), group.get(1), group.get(2), locations);

        // Render the appropriate danger zone
        Set<WorldPoint> dangerZone = new HashSet<>();

        if (isHorizontal)
        {
            dangerZone = calculateHorizontalDangerZone(group, locations);
        }
        else if (isVertical)
        {
            dangerZone = calculateVerticalDangerZone(group, locations);
        }
        else if (isDiagonal)
        {
            dangerZone = calculateDiagonalDangerZone(group, locations);
        }
        else
        {
            // Fallback: just render individual bombs
            for (YamaPlugin.AoeBomb bomb : group)
            {
                renderSingleBomb(g, bomb, nowTick);
            }
            return;
        }

        // Render all danger tiles
        for (WorldPoint wp : dangerZone)
        {
            renderDangerTile(g, wp, Color.RED);
        }

        // Calculate the tick counter
        YamaPlugin.AoeBomb firstBomb = group.get(0);
        int age = nowTick - firstBomb.getStartTick();
        int ticksLeft = TOTAL_TICKS - age;
        String text = Integer.toString(ticksLeft);
        // Render the count on the middle bomb
        YamaPlugin.AoeBomb middleBomb = group.get(1); // Middle bomb in a group of 3
        LocalPoint middleLp = middleBomb.getLoc();
        Polygon centerPoly = Perspective.getCanvasTilePoly(client, middleLp);
        if (centerPoly != null)
        {
            int cx = centerPoly.getBounds().x + centerPoly.getBounds().width / 2;
            int cy = centerPoly.getBounds().y + centerPoly.getBounds().height / 2;
            g.setColor(Color.WHITE);
            g.drawString(text,
                    cx - g.getFontMetrics().stringWidth(text) / 2,
                    cy + g.getFontMetrics().getHeight() / 2
            );
        }
    }

    private Set<WorldPoint> calculateHorizontalDangerZone(List<YamaPlugin.AoeBomb> group, Map<YamaPlugin.AoeBomb, WorldPoint> locations)
    {
        Set<WorldPoint> zone = new HashSet<>();

        group.sort(Comparator.comparing(b -> locations.get(b).getX()));
        WorldPoint left = locations.get(group.get(0));
        WorldPoint center = locations.get(group.get(1));
        WorldPoint right = locations.get(group.get(2));
        int plane = left.getPlane();
        int y = center.getY();

        for (WorldPoint wp : Arrays.asList(left, center, right))
        {
            for (int dx = -1; dx <= 1; dx++)
            {
                for (int dy = -1; dy <= 1; dy++)
                {
                    zone.add(new WorldPoint(wp.getX() + dx, wp.getY() + dy, plane));
                }
            }
        }
        for (int x = left.getX(); x <= right.getX(); x++)
        {
            zone.add(new WorldPoint(x, y, plane));
        }
        return zone;
    }

    private Set<WorldPoint> calculateVerticalDangerZone(List<YamaPlugin.AoeBomb> group, Map<YamaPlugin.AoeBomb, WorldPoint> locations)
    {
        Set<WorldPoint> zone = new HashSet<>();

        group.sort(Comparator.comparing(b -> locations.get(b).getY()));
        WorldPoint bottom = locations.get(group.get(0));
        WorldPoint center = locations.get(group.get(1));
        WorldPoint top = locations.get(group.get(2));
        int plane = center.getPlane();
        int x = center.getX();

        for (WorldPoint wp : Arrays.asList(bottom, center, top))
        {
            for (int dx = -1; dx <= 1; dx++)
            {
                for (int dy = -1; dy <= 1; dy++)
                {
                    zone.add(new WorldPoint(wp.getX() + dx, wp.getY() + dy, plane));
                }
            }
        }
        for (int y = bottom.getY(); y <= top.getY(); y++)
        {
            zone.add(new WorldPoint(x, y, plane));
        }
        return zone;
    }

    private Set<WorldPoint> calculateDiagonalDangerZone(List<YamaPlugin.AoeBomb> group, Map<YamaPlugin.AoeBomb, WorldPoint> locations)
    {
        Set<WorldPoint> zone = new HashSet<>();

        List<WorldPoint> points = new ArrayList<>();
        for (YamaPlugin.AoeBomb bomb : group) points.add(locations.get(bomb));
        points.sort(Comparator.comparing(WorldPoint::getX).thenComparing(WorldPoint::getY));
        WorldPoint center = points.get(1);
        int x = center.getX();
        int y = center.getY();
        int plane = center.getPlane();

        for (int d = -3; d <= 3; d++)
        {
            if (d == 0) continue;
            zone.add(new WorldPoint(x + d, y + d, plane));
            zone.add(new WorldPoint(x + d, y - d, plane));
        }
        for (WorldPoint wp : points)
        {
            for (int dx = -1; dx <= 1; dx++)
            {
                for (int dy = -1; dy <= 1; dy++)
                {
                    zone.add(new WorldPoint(wp.getX() + dx, wp.getY() + dy, plane));
                }
            }
        }
        zone.add(center);

        return zone;
    }

    private void renderSingleBomb(Graphics2D g, YamaPlugin.AoeBomb bomb, int nowTick)
    {
        // Unchanged from your original code:
        LocalPoint lp = bomb.getLoc();
        Polygon poly = Perspective.getCanvasTilePoly(client, lp);
        if (poly != null)
        {
            g.setColor(Color.YELLOW);
            g.draw(poly);
        }
    }

    private void renderDangerTile(Graphics2D g, WorldPoint wp, Color color)
    {
        LocalPoint lp = LocalPoint.fromWorld(client, wp);
        if (lp == null)
            return;

        Polygon poly = Perspective.getCanvasTilePoly(client, lp);
        if (poly != null)
        {
            g.setColor(color);
            g.draw(poly);
        }
    }
}
