package com.shardshop.config;

import com.shardshop.ShardShop;
import lombok.Getter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;

@Getter
public class ConfigManager {
    private final ShardShop plugin;
    private FileConfiguration config;
    private FileConfiguration messages;
    
    public ConfigManager(ShardShop plugin) {
        this.plugin = plugin;
    }
    
    public void loadConfigs() {
        plugin.saveDefaultConfig();
        this.config = plugin.getConfig();
        
        File messagesFile = new File(plugin.getDataFolder(), "messages.yml");
        if (!messagesFile.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        this.messages = YamlConfiguration.loadConfiguration(messagesFile);
    }
    
    public void reloadConfigs() {
        plugin.reloadConfig();
        this.config = plugin.getConfig();
        
        File messagesFile = new File(plugin.getDataFolder(), "messages.yml");
        this.messages = YamlConfiguration.loadConfiguration(messagesFile);
    }
    
    public String getMessage(String path) {
        String message = messages.getString(path, "&cMessage not found: " + path);
        String prefix = messages.getString("prefix", "");
        return colorize(prefix + message);
    }
    
    public String getMessageNoPrefix(String path) {
        String message = messages.getString(path, "&cMessage not found: " + path);
        return colorize(message);
    }
    
    public String colorize(String text) {
        return text.replace("&", "§");
    }
    
    public String formatPrice(double price) {
        String format = config.getString("display.price_format", "&e$%,.2f");
        return colorize(String.format(format, price));
    }
    
    public String formatSellPrice(double price) {
        String format = config.getString("display.sell_price_format", "&a$%,.2f");
        return colorize(String.format(format, price));
    }
}