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
        // Get the player who joined
        Player player = event.getPlayer();

        UUID uuid = player.getUniqueId();
        String uuidWithoutHyphens = uuid.toString().replace("-", "");
        String token = this.config.getString("token");
        String parameters = this.config.getString("parameters");
        String fail_message = this.config.getString("fail_message");
        if (parameters.toLowerCase().contains("debug")) {
            getLogger().log(Level.INFO, "Token: {0}", token);
            getLogger().log(Level.INFO, "UUID: {0}", uuidWithoutHyphens);
            getLogger().info("Called PlayerJoin for: " + player.getName());
        }
        boolean authorized = false;
        // Check if the player is whitelisted
        if (player.isWhitelisted()) {
            authorized = true;
        } else {

            HttpURLConnection connection = null;
            try {
                @SuppressWarnings("deprecation")
                URL url = new URL(this.endpointLocation + "?token=" + token + "&uuid=" + uuidWithoutHyphens);
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setRequestProperty("User-Agent", "Mozilla/5.0");

                int responseCode = connection.getResponseCode();

                if (parameters.toLowerCase().contains("debug")) {
                    getLogger().log(Level.INFO, "Response Code: {0}", responseCode);
                }

                // Read the response if the response code is 200 (OK)
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String inputLine;
                    while ((inputLine = in.readLine()) != null) {
                        response.append(inputLine);
                    }
                    in.close();
                    if (parameters.toLowerCase().contains("debug")) {
                        getLogger().log(Level.INFO, "Response: {0}", response.toString());
                    }
                    JSONObject data = new JSONObject(response.toString());
                    boolean subscriber = data.getBoolean("subscriber");
                    boolean exists = data.getBoolean("exists");
                    int tier = data.getInt("tier");
                    if (parameters.toLowerCase().contains("debug")) {
                        getLogger().log(Level.INFO, "Exists: {0}, Subscriber: {1}, Tier: {2}", new Object[]{exists, subscriber, tier});
                    }
                    authorized = subscriber;
                } else {
                    if (parameters.toLowerCase().contains("debug")) {
                        getLogger().log(null, "GET request failed with response code: {0}", responseCode);
                    }
                }
            } catch (IOException e) {
                if (parameters.toLowerCase().contains("debug")) {
                    getLogger().log(null, "Error during HTTP request: {0}", e.getMessage());
                }
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        }
        if (parameters.toLowerCase().contains("debug")) {
            getLogger().log(Level.INFO, "Authorized status for {0}: {1}", new Object[]{player.getName(), authorized});
        }
        if (!authorized) {
            event.setJoinMessage(null);
            player.kickPlayer(fail_message);
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
