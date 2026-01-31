package com.shardshop.shop;

import com.shardshop.ShardShop;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Getter
public class ShopManager {
    private final ShardShop plugin;
    private final List<ShopItem> shopItems;
    private final File shopFile;
    
    public ShopManager(ShardShop plugin) {
        this.plugin = plugin;
        this.shopItems = new ArrayList<>();
        this.shopFile = new File(plugin.getDataFolder(), "shop.yml");
    }
    
    public void addItem(ShopItem item) {
        shopItems.add(item);
        if (plugin.getConfigManager().getConfig().getBoolean("shop.async_save")) {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, this::saveShopItems);
        } else {
            saveShopItems();
        }
    }
    
    public boolean removeItem(ItemStack item) {
        Optional<ShopItem> found = shopItems.stream()
            .filter(si -> si.matches(item))
            .findFirst();
            
        if (found.isPresent()) {
            shopItems.remove(found.get());
            if (plugin.getConfigManager().getConfig().getBoolean("shop.async_save")) {
                Bukkit.getScheduler().runTaskAsynchronously(plugin, this::saveShopItems);
            } else {
                saveShopItems();
            }
            return true;
        }
        return false;
    }
    
    public boolean itemExists(ItemStack item) {
        return shopItems.stream().anyMatch(si -> si.matches(item));
    }
    
    public Optional<ShopItem> findItem(ItemStack item) {
        return shopItems.stream()
            .filter(si -> si.matches(item))
            .findFirst();
    }
    
    public void clearItems() {
        shopItems.clear();
        saveShopItems();
    }
    
    public void loadShopItems() {
        if (!shopFile.exists()) {
            return;
        }
        
        try {
            FileConfiguration config = YamlConfiguration.loadConfiguration(shopFile);
            List<?> items = config.getList("items", new ArrayList<>());
            
            shopItems.clear();
            for (Object obj : items) {
                if (obj instanceof ShopItem) {
                    shopItems.add((ShopItem) obj);
                }
            }
            
            plugin.getLogger().info("Loaded " + shopItems.size() + " shop items.");
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to load shop items: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public void saveShopItems() {
        try {
            FileConfiguration config = new YamlConfiguration();
            config.set("items", shopItems);
            config.save(shopFile);
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to save shop items: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public double getSellPrice(double buyPrice) {
        double percentage = plugin.getConfigManager().getConfig()
            .getDouble("economy.sell_back_percentage", 50.0);
        return (buyPrice * percentage) / 100.0;
    }
    
    public boolean isSellBackEnabled() {
        return plugin.getConfigManager().getConfig()
            .getBoolean("economy.sell_back_enabled", true);
    }
}