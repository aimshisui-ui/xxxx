package com.soulenchants.guilds;

import org.bukkit.inventory.Inventory;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

/**
 * In-memory guild record. Persisted to YAML by {@link GuildManager}.
 *
 * Vault is held as a Bukkit {@link Inventory} so it shares the same item
 * pipeline as a normal chest — drag/shift-click/quick-pickup all work for free.
 * Players who aren't members get the GUI denied at the command layer.
 */
public class Guild {

    public static final int MAX_MEMBERS = 10;
    public static final int VAULT_SIZE  = 54; // double-chest

    private final String name;
    private final UUID owner;
    private final Set<UUID> members = new LinkedHashSet<>();
    private long points;
    private final Inventory vault;

    public Guild(String name, UUID owner, Inventory vault) {
        this.name = name;
        this.owner = owner;
        this.vault = vault;
        this.members.add(owner);
    }

    public String getName()        { return name; }
    public UUID   getOwner()       { return owner; }
    public Set<UUID> getMembers()  { return members; }
    public long   getPoints()      { return points; }
    public Inventory getVault()    { return vault; }

    public boolean isMember(UUID id) { return members.contains(id); }
    public boolean isFull()          { return members.size() >= MAX_MEMBERS; }

    public boolean addMember(UUID id) {
        if (isFull()) return false;
        return members.add(id);
    }

    public boolean removeMember(UUID id) {
        if (id.equals(owner)) return false;       // owner can't be removed via leave
        return members.remove(id);
    }

    public void addPoints(long amount) { if (amount > 0) this.points += amount; }
    public void setPoints(long amount) { this.points = Math.max(0, amount); }
}
