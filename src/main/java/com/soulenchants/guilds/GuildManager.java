package com.soulenchants.guilds;

import com.soulenchants.SoulEnchants;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Owns every Guild instance + persistence + the cached Top-10 leaderboard.
 *
 * Storage: guilds.yml
 *   guilds:
 *     <name>:
 *       owner: <uuid>
 *       members: [<uuid>, ...]
 *       points: <long>
 *       vault: <serialized inventory contents>
 *   member_index:
 *     <uuid>: <guild_name>          # reverse lookup so isAlly() is O(1)
 *
 * Top-10 leaderboard is rebuilt every 5 minutes off the main thread so
 * `/guild top` opens instantly without sorting on demand.
 */
public class GuildManager {

    private final SoulEnchants plugin;
    private final File file;
    private final FileConfiguration config;
    /** Lower-cased name → Guild. */
    private final Map<String, Guild> guildsByName = new HashMap<>();
    /** Member UUID → owning guild. */
    private final Map<UUID, Guild>  guildByMember = new HashMap<>();
    /** Pending invitee → set of guild names that have invited them. */
    private final Map<UUID, Set<String>> invites = new HashMap<>();
    /** Cached top-10 snapshot, rebuilt async every 5 minutes. */
    private volatile List<TopEntry> topCache = Collections.emptyList();
    private BukkitRunnable refreshTask;

    public GuildManager(SoulEnchants plugin, File dataFolder) {
        this.plugin = plugin;
        if (!dataFolder.exists()) dataFolder.mkdirs();
        this.file = new File(dataFolder, "guilds.yml");
        if (!file.exists()) try { file.createNewFile(); } catch (IOException ignored) {}
        this.config = YamlConfiguration.loadConfiguration(file);
        load();
        rebuildTopCache();
    }

    public void start() {
        // Refresh leaderboard every 5 min (6000 ticks) — async sort, sync swap
        refreshTask = new BukkitRunnable() {
            @Override public void run() {
                Bukkit.getScheduler().runTaskAsynchronously(plugin, GuildManager.this::rebuildTopCache);
            }
        };
        refreshTask.runTaskTimer(plugin, 6000L, 6000L);
    }

    public void stop() {
        if (refreshTask != null) try { refreshTask.cancel(); } catch (Exception ignored) {}
    }

    // ── Lookups ─────────────────────────────────────────────────────────

    public Guild get(String name)        { return name == null ? null : guildsByName.get(name.toLowerCase()); }
    public Guild getByMember(UUID id)    { return guildByMember.get(id); }
    public Collection<Guild> all()       { return guildsByName.values(); }

    /** True iff both UUIDs are in the SAME guild. Self-checks return true. */
    public boolean isAlly(UUID a, UUID b) {
        if (a == null || b == null) return false;
        if (a.equals(b)) return true;
        Guild ga = guildByMember.get(a);
        if (ga == null) return false;
        return ga.isMember(b);
    }

    // ── Mutations ───────────────────────────────────────────────────────

    public Guild create(String name, Player owner) {
        String key = name.toLowerCase();
        if (guildsByName.containsKey(key)) return null;
        if (guildByMember.containsKey(owner.getUniqueId())) return null;
        Inventory vault = Bukkit.createInventory(null, Guild.VAULT_SIZE,
                ChatColor.DARK_PURPLE + "Guild Vault — " + name);
        Guild g = new Guild(name, owner.getUniqueId(), vault);
        guildsByName.put(key, g);
        guildByMember.put(owner.getUniqueId(), g);
        return g;
    }

    public void invite(Guild g, UUID invitee) {
        invites.computeIfAbsent(invitee, k -> new HashSet<>()).add(g.getName().toLowerCase());
    }

    public boolean hasInvite(UUID invitee, String guildName) {
        Set<String> set = invites.get(invitee);
        return set != null && set.contains(guildName.toLowerCase());
    }

    /** Returns 0=success, 1=full, 2=no-invite, 3=already-in-guild, 4=no-such-guild. */
    public int join(Player p, String guildName) {
        Guild g = get(guildName);
        if (g == null) return 4;
        if (guildByMember.containsKey(p.getUniqueId())) return 3;
        if (!hasInvite(p.getUniqueId(), guildName)) return 2;
        if (g.isFull()) return 1;
        g.addMember(p.getUniqueId());
        guildByMember.put(p.getUniqueId(), g);
        Set<String> set = invites.get(p.getUniqueId());
        if (set != null) set.remove(guildName.toLowerCase());
        return 0;
    }

    /** Returns 0=success, 1=owner-cant-leave-must-disband, 2=not-in-guild. */
    public int leave(Player p) {
        Guild g = guildByMember.get(p.getUniqueId());
        if (g == null) return 2;
        if (g.getOwner().equals(p.getUniqueId())) return 1;
        g.removeMember(p.getUniqueId());
        guildByMember.remove(p.getUniqueId());
        return 0;
    }

    /** Disband — only callable by the owner. Drops vault contents at owner's feet. */
    public boolean disband(Player owner) {
        Guild g = guildByMember.get(owner.getUniqueId());
        if (g == null || !g.getOwner().equals(owner.getUniqueId())) return false;
        // Drop vault contents at owner's feet so they're not lost
        for (ItemStack it : g.getVault().getContents()) {
            if (it != null && it.getType() != org.bukkit.Material.AIR)
                owner.getWorld().dropItemNaturally(owner.getLocation(), it);
        }
        for (UUID m : new HashSet<>(g.getMembers())) guildByMember.remove(m);
        guildsByName.remove(g.getName().toLowerCase());
        return true;
    }

    public void awardPoints(Guild g, long amount, String reason) {
        if (g == null || amount <= 0) return;
        g.addPoints(amount);
        for (UUID m : g.getMembers()) {
            Player p = Bukkit.getPlayer(m);
            if (p != null) p.sendMessage(ChatColor.LIGHT_PURPLE + "✦ Guild +" + amount
                    + " points " + ChatColor.GRAY + "(" + reason + ")");
        }
    }

    // ── Leaderboard cache ───────────────────────────────────────────────

    public List<TopEntry> getTopCached() { return topCache; }

    public void rebuildTopCache() {
        // Snapshot guild list — we only iterate map keys + read-only fields.
        List<TopEntry> snap = new ArrayList<>();
        for (Guild g : guildsByName.values()) {
            snap.add(new TopEntry(g.getName(), g.getMembers().size(), g.getPoints()));
        }
        snap.sort((a, b) -> Long.compare(b.points, a.points));
        if (snap.size() > 10) snap = snap.subList(0, 10);
        this.topCache = snap;
    }

    public static final class TopEntry {
        public final String name;
        public final int memberCount;
        public final long points;
        public TopEntry(String name, int memberCount, long points) {
            this.name = name; this.memberCount = memberCount; this.points = points;
        }
    }

    // ── Persistence ─────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private void load() {
        if (!config.isConfigurationSection("guilds")) return;
        for (String key : config.getConfigurationSection("guilds").getKeys(false)) {
            try {
                String base = "guilds." + key + ".";
                String name = config.getString(base + "name", key);
                UUID owner = UUID.fromString(config.getString(base + "owner"));
                long points = config.getLong(base + "points", 0L);
                List<String> memberStrs = config.getStringList(base + "members");
                Inventory vault = Bukkit.createInventory(null, Guild.VAULT_SIZE,
                        ChatColor.DARK_PURPLE + "Guild Vault — " + name);
                Object vaultRaw = config.get(base + "vault");
                if (vaultRaw instanceof List) {
                    List<?> contents = (List<?>) vaultRaw;
                    for (int i = 0; i < contents.size() && i < Guild.VAULT_SIZE; i++) {
                        Object o = contents.get(i);
                        if (o instanceof ItemStack) vault.setItem(i, (ItemStack) o);
                    }
                }
                Guild g = new Guild(name, owner, vault);
                g.setPoints(points);
                for (String ms : memberStrs) {
                    try {
                        UUID id = UUID.fromString(ms);
                        if (!id.equals(owner)) g.addMember(id);
                    } catch (Exception ignored) {}
                }
                guildsByName.put(name.toLowerCase(), g);
                for (UUID m : g.getMembers()) guildByMember.put(m, g);
            } catch (Throwable t) {
                plugin.getLogger().warning("[guilds] failed to load guild " + key + ": " + t);
            }
        }
        plugin.getLogger().info("[guilds] loaded " + guildsByName.size() + " guilds");
    }

    public void save() {
        config.set("guilds", null);
        for (Guild g : guildsByName.values()) {
            String base = "guilds." + g.getName().toLowerCase() + ".";
            config.set(base + "name", g.getName());
            config.set(base + "owner", g.getOwner().toString());
            config.set(base + "points", g.getPoints());
            List<String> members = new ArrayList<>();
            for (UUID m : g.getMembers()) members.add(m.toString());
            config.set(base + "members", members);
            // Save vault — Bukkit serializes ItemStack into a flat List automatically
            List<ItemStack> vaultList = new ArrayList<>(Guild.VAULT_SIZE);
            for (int i = 0; i < Guild.VAULT_SIZE; i++) vaultList.add(g.getVault().getItem(i));
            config.set(base + "vault", vaultList);
        }
        try { config.save(file); } catch (IOException ignored) {}
    }
}
