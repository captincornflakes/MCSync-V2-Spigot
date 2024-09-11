package com.mcsync.plugin;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.json.JSONObject;

public class mcsync extends JavaPlugin implements Listener {

    private FileConfiguration config = getConfig();
    private String prefix = ChatColor.LIGHT_PURPLE + "[" + ChatColor.BLUE + "MCSYNC" + ChatColor.LIGHT_PURPLE + "] " + ChatColor.RESET;
    private String endpointLocation = "https://v2.mcsync.live/api.php";

    @Override
    public void onLoad() {
    }

    @Override
    public void onEnable() {
        saveDefaultConfig();
        getServer().getPluginManager().registerEvents(this, this);
        this.getCommand("mcsync-reload").setExecutor(new CommandMcsync());
    }

    @Override
    public void onDisable() {
    }

    // Handle player login event
    @EventHandler(priority = EventPriority.LOW)
    public void onPlayerLogin(PlayerLoginEvent e) {
        System.out.println("Called PlayerLogin");
        String fail = "§7Please subscribe to the streamer and join their Discord server.";
        boolean authorized = false;

        // Check if the player is whitelisted
        if (getServer().getWhitelistedPlayers().stream().anyMatch(player -> player.getUniqueId().equals(e.getPlayer().getUniqueId()))) {
            authorized = true;
        } else {
            String uuid = e.getPlayer().getUniqueId().toString().replace("-", "");
            String token = this.config.getString("token");
            System.out.println("Token: " + token);
            System.out.println("UUID: " + uuid);

            try {
                URL url = new URL(this.endpointLocation + "?token=" + token + "&uuid=" + uuid);

                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                StringBuilder response = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                }
                JSONObject jsonResponse = new JSONObject(response.toString());
                if (jsonResponse.has("subscriber")) {
                    authorized = jsonResponse.getBoolean("subscriber");
                } else {
                    System.out.println("No subscriber field found in the response.");
                }
            } catch (IOException x) {
                System.out.println("Error during HTTP request.");
                x.printStackTrace();
            }
        }
        System.out.println("authorized: " + authorized);
        if (!authorized) {
            e.disallow(PlayerLoginEvent.Result.KICK_WHITELIST, fail);
        }
    }

    public class CommandMcsync implements CommandExecutor {

        @Override
        public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
            String serverKey = config.getString("token");
            if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
                sender.sendMessage(ChatColor.LIGHT_PURPLE + ChatColor.STRIKETHROUGH.toString() + "--------------------------------------------");
                sender.sendMessage(ChatColor.GOLD + "The following are valid commands for MCSync:");
                sender.sendMessage(ChatColor.GOLD + "| " + ChatColor.YELLOW + "/mcs set" + ChatColor.GRAY + ChatColor.ITALIC + " (Used to set server token)");
                sender.sendMessage(ChatColor.GOLD + "| " + ChatColor.YELLOW + "/mcs get" + ChatColor.GRAY + ChatColor.ITALIC + " (Show your server token)");
                sender.sendMessage(ChatColor.LIGHT_PURPLE + ChatColor.STRIKETHROUGH.toString() + "--------------------------------------------");
            } else if (args.length > 0) {
                if (args[0].equalsIgnoreCase("set")) {
                    if (args.length < 2) {
                        sender.sendMessage(prefix + ChatColor.RED + "Please supply a server key.");
                    } else {
                        config.set("token", args[1]);
                        sender.sendMessage(prefix + ChatColor.AQUA + "Server key set to " + ChatColor.GREEN + args[1]);
                        saveConfig();
                    }
                } else if (args[0].equalsIgnoreCase("get")) {
                    sender.sendMessage(prefix + ChatColor.AQUA + "Your server key is: " + ChatColor.GREEN + serverKey);
                } else {
                    sender.sendMessage(prefix + ChatColor.RED + "Unknown command.");
                }
            }
            return true;
        }
    }
}
