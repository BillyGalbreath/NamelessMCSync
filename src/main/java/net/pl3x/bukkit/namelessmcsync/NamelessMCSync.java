package net.pl3x.bukkit.namelessmcsync;

import net.milkbowl.vault.permission.Permission;
import net.pl3x.bukkit.namelessmcsync.api.CheckWebAPIConnection;
import net.pl3x.bukkit.namelessmcsync.api.NamelessPlayer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.net.MalformedURLException;
import java.net.URL;

public class NamelessMCSync extends JavaPlugin implements Listener {
    public URL apiURL;
    private Permission permission;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            getLogger().severe("This plugin requires Vault. Please install Vault and restart.");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        RegisteredServiceProvider<Permission> permissionProvider = getServer().getServicesManager().getRegistration(net.milkbowl.vault.permission.Permission.class);
        permission = permissionProvider.getProvider();

        if (permission == null) {
            getLogger().severe("You do not have a vault-compatible permissions plugin. Please install one and restart.");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        if (!checkConnection()) {
            return;
        }

        getServer().getPluginManager().registerEvents(this, this);

        getLogger().info(getName() + " v" + getDescription().getVersion() + " enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info(getName() + " disabled.");
    }

    private boolean checkConnection() {
        String url = getConfig().getString("api-url");
        if (url == null || url.equals("")) {
            getLogger().severe("No API URL set in the NamelessMC configuration. Nothing will work until you set the correct url.");
            return false;
        }

        try {
            apiURL = new URL(url);
        } catch (MalformedURLException e) {
            getLogger().severe("Invalid API Url/Key. Nothing will work until you set the correct url.");
            getLogger().severe("Error 1: " + e.getMessage());
            e.printStackTrace();
            return false;
        }

        CheckWebAPIConnection checkConnection = new CheckWebAPIConnection(this);
        if (checkConnection.error) {
            getLogger().severe("Invalid API Url/Key. Nothing will work until you set the correct url.");
            getLogger().severe("Error 2: " + checkConnection.errorMessage);
            return false;
        }

        return true;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        new HandleSync(event.getPlayer()).runTaskAsynchronously(this);
    }

    private class HandleSync extends BukkitRunnable {
        private final Player player;

        private HandleSync(Player player) {
            this.player = player;
        }

        @Override
        public void run() {
            NamelessPlayer namelessPlayer = new NamelessPlayer(player.getUniqueId().toString(), NamelessMCSync.this);
            if (!namelessPlayer.exists || !namelessPlayer.validated) {
                getLogger().warning("does not exist or not validated!");
                return;
            }
            String websiteGroup = getConfig().getString("groups." + namelessPlayer.groupID, "");
            boolean hasGroup = false;
            for (String group : permission.getGroups()) {
                if (group.equals(websiteGroup)) {
                    hasGroup = true;
                    break;
                }
            }
            if (!hasGroup) {
                getLogger().severe("Unable to sync player group! The group '" + websiteGroup + "' does not exist!");
                return;
            }
            if (permission.playerInGroup(null, player, websiteGroup)) {
                getLogger().warning("already in group");
                return;
            }
            String primaryGroup = permission.getPrimaryGroup(player);
            if (websiteGroup.equals(primaryGroup)) {
                getLogger().warning("already in group (2)");
                return;
            }
            if (!permission.playerAddGroup(null, player, websiteGroup)) {
                getLogger().severe("Unable to sync player group! Adding player to new group failed!");
                return;
            }
            if (!permission.playerRemove(null, player, primaryGroup)) {
                permission.playerRemove(null, player, websiteGroup);
                getLogger().severe("Unable to sync player group! Removing player from old group failed!");
                return;
            }
            getLogger().info("Player group synced from website: " + player.getName() + " old: " + primaryGroup + " new: " + websiteGroup);
        }
    }
}
