package com.mcsync.plugin;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class mcsync extends JavaPlugin {

	FileConfiguration config = getConfig();
	FileConfiguration messagesConfig = getCustomConfig();
	private File customConfigFile;
	
	
	String prefix = ChatColor.LIGHT_PURPLE + "[" + ChatColor.BLUE + "MCSYNC" + ChatColor.LIGHT_PURPLE + "] " + ChatColor.RESET ;
	String endpointLocation = "https://v2.mcsync.live/api.php";

	public FileConfiguration getCustomConfig() {
		return this.messagesConfig;
		}

	public void createCustomConfig() {
		customConfigFile = new File(this.getDataFolder(), "messages.yml");
		if (!customConfigFile.exists()) {
			customConfigFile.getParentFile().mkdirs();
			saveResource("messages.yml", false);
			}
		messagesConfig = new YamlConfiguration();
		try {
			messagesConfig.load(customConfigFile);
			} 
		catch (IOException | InvalidConfigurationException e){
			e.printStackTrace();
			}
		
		}

	@Override
	public void onLoad() {
		}
	
    @Override
	public void onEnable() {
        saveDefaultConfig();
        //int pluginId = 23291;
        //new Metrics(this, pluginId);
		}

	@Override
	public void onDisable() {
		}
	
    @SuppressWarnings("deprecation")
    public boolean CheckMcsync(String uuid){
	    boolean pass = false;  
	    String token = this.config.getString("serverKEY");
        try {
            URL url = new URL(String.valueOf(this.endpointLocation) + "?serverKEY=" + token + "&UUID=" + uuid);
            HttpURLConnection connection = (HttpURLConnection)url.openConnection();
            connection.setRequestMethod("GET");
            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String result = reader.readLine();
            reader.close();
            if (result.equals("true")) {
                pass = true;
                  } 
            else {
                pass = false;
                  } 
            } 
        catch (IOException ignored) {
            pass = false;
            }
        return pass;
        }

	@EventHandler(priority = EventPriority.LOW)
	public void onPlayerJoin(AsyncPlayerPreLoginEvent e) throws Exception {
	    String fail = this.messagesConfig.getString("message-fail");
	    boolean authorized = false;  
	    if (getServer().getWhitelistedPlayers().stream().anyMatch(player -> player.getUniqueId().equals(e.getUniqueId()))) {
	    	authorized = true;
	    	}
	    else {
            String uuid = e.getUniqueId().toString().replace("-", "");
            authorized = CheckMcsync(uuid);
	    	}
	    if (!authorized) {
	    	e.disallow(AsyncPlayerPreLoginEvent.Result.KICK_WHITELIST, fail);
	    	} 
	   }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("mcsync-reload")) {
            reloadConfig();
            sender.sendMessage("MCSync reloaded!");
            return true;
        }
        return false;
    }
       
    public class CommandMcsync implements CommandExecutor {
        public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
            String serverKey = config.getString("serverKEY");
            if (args.length == 0 || args[0].equalsIgnoreCase("help")){
                    sender.sendMessage("" + ChatColor.LIGHT_PURPLE + ChatColor.STRIKETHROUGH + "--------------------------------------------");
                    sender.sendMessage(ChatColor.GOLD + "The following are valid commands for MCSync");
                    sender.sendMessage(ChatColor.GOLD + "| " + ChatColor.YELLOW + "/mcs set" + ChatColor.GRAY + ChatColor.ITALIC + " (Used to set server token)");
                    sender.sendMessage(ChatColor.GOLD + "| " + ChatColor.YELLOW + "/mcs get" + ChatColor.GRAY + ChatColor.ITALIC + " (Show your server token)");
                    sender.sendMessage("" + ChatColor.LIGHT_PURPLE + ChatColor.STRIKETHROUGH + "--------------------------------------------");
                    }
            else if (args.length > 0){
                    if (args[0].equalsIgnoreCase("set")){
                        if (args.length < 2){sender.sendMessage(prefix + ChatColor.RED + "Please supply a server Key");}
                        else {
                            config.set("serverKEY", args[1]);
                            sender.sendMessage(prefix + ChatColor.AQUA + "Server key set to " + ChatColor.GREEN + args[1]);
                            saveConfig();
                            }
                        }
                else if (args[0].equalsIgnoreCase("get")){
                        sender.sendMessage(prefix + ChatColor.AQUA + "Your server key is: " + ChatColor.GREEN + serverKey);
                    }
                else {
                        sender.sendMessage(prefix + ChatColor.RED + "Unknown Command");
                        }
                    }
                return true;
                }
        }
}
