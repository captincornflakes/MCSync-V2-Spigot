package com.mcsync.plugin;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.UUID;
import java.util.logging.Level;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.json.JSONObject;

public class mcsync extends JavaPlugin implements Listener {

    @SuppressWarnings("FieldMayBeFinal")
    private FileConfiguration config = getConfig();
    @SuppressWarnings("FieldMayBeFinal")
    private String prefix = ChatColor.LIGHT_PURPLE + "[" + ChatColor.BLUE + "MCSYNC" + ChatColor.LIGHT_PURPLE + "] " + ChatColor.RESET;
    @SuppressWarnings("FieldMayBeFinal")
    private String endpointLocation = "https://mcsync.live/api.php";

    private boolean isKicked = false;

    @Override
    public void onLoad() {
    }

    @Override
    public void onEnable() {
        saveDefaultConfig();
        getServer().getPluginManager().registerEvents(this, this);
        this.getCommand("mcsync-reload").setExecutor(new CommandMcsync());
        getLogger().info("MCSync has been enabled!");
        String parameters = this.config.getString("parameters");
        getLogger().info("MCSync parameters: " + parameters);
    }

    @Override
    public void onDisable() {
        getLogger().info("MCSync has been disabled.");
    }

    @EventHandler
    public void onPlayerKick(PlayerKickEvent event) {
        isKicked = true;
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        if (isKicked) {
            event.setQuitMessage(null);
            isKicked = false;
        }

    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        String token = config.getString("token");
        String parameters = config.getString("parameters", "").toLowerCase();
        boolean authorizePlayer = false;
        int tier = 0;
        if (player.isWhitelisted()) {
            authorizePlayer = true;
        }
        if (parameters.contains("debug")) {
            getLogger().info("PlayerJoin: " + player.getName());
            getLogger().info("Token: " + token);
            getLogger().info("UUID: " + uuid);
        }

        HttpURLConnection connection = null;
        try {
            URL url = new URL(endpointLocation + "?token=" + token + "&uuid=" + uuid.toString().replace("-", ""));
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("User-Agent", "Mozilla/5.0");
            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();
                JSONObject data = new JSONObject(response.toString());
                authorizePlayer = data.getBoolean("subscriber");
                if (parameters.contains("debug")) {
                    getLogger().info("Response: " + response.toString());
                }
            }
        } catch (IOException e) {
            getLogger().log(Level.SEVERE, "Error during HTTP request: {0}", e.getMessage());
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }

        if (parameters.contains("debug")) {
            getLogger().info("Auth: " + authorizePlayer);
        }
        if (authorizePlayer) {
        } else {
            player.kickPlayer(config.getString("fail_message", "You are not authorized to join this server."));
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
