package com.soulenchants.rifts;

import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Multi-line armor-stand floating text. 1.8.8 has no HolographicDisplays by
 * default, so we spawn invisible marker ArmorStands stacked 0.26y apart and
 * use their custom-name as the line content.
 */
public final class Hologram {

    private static final double LINE_SPACING = 0.26;

    private final List<ArmorStand> stands = new ArrayList<>();
    private Location anchor;

    public Hologram(Location top, List<String> lines) {
        this.anchor = top.clone();
        spawn(lines);
    }

    private void spawn(List<String> lines) {
        Location l = anchor.clone();
        for (String line : lines) {
            ArmorStand as = (ArmorStand) l.getWorld().spawnEntity(l, EntityType.ARMOR_STAND);
            as.setVisible(false);
            as.setGravity(false);
            as.setMarker(true);
            as.setCustomName(line);
            as.setCustomNameVisible(true);
            as.setBasePlate(false);
            as.setArms(false);
            as.setRemoveWhenFarAway(false);
            stands.add(as);
            l.subtract(0, LINE_SPACING, 0);
        }
    }

    public void updateLine(int i, String text) {
        if (i < 0 || i >= stands.size()) return;
        stands.get(i).setCustomName(text);
    }

    public void setLines(List<String> lines) {
        // Re-use existing stands where possible, add/remove as needed
        int common = Math.min(lines.size(), stands.size());
        for (int i = 0; i < common; i++) stands.get(i).setCustomName(lines.get(i));
        if (lines.size() > stands.size()) {
            Location l = stands.get(stands.size() - 1).getLocation().clone().subtract(0, LINE_SPACING, 0);
            for (int i = stands.size(); i < lines.size(); i++) {
                ArmorStand as = (ArmorStand) l.getWorld().spawnEntity(l, EntityType.ARMOR_STAND);
                as.setVisible(false);
                as.setGravity(false);
                as.setMarker(true);
                as.setCustomName(lines.get(i));
                as.setCustomNameVisible(true);
                as.setBasePlate(false);
                as.setArms(false);
                as.setRemoveWhenFarAway(false);
                stands.add(as);
                l.subtract(0, LINE_SPACING, 0);
            }
        } else if (lines.size() < stands.size()) {
            for (int i = stands.size() - 1; i >= lines.size(); i--) {
                stands.get(i).remove();
                stands.remove(i);
            }
        }
    }

    public void destroy() {
        for (ArmorStand as : stands) {
            try { as.remove(); } catch (Throwable ignored) {}
        }
        stands.clear();
    }

    public static Hologram at(Location top, String... lines) {
        return new Hologram(top, Arrays.asList(lines));
    }
}
