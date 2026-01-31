package com.shardshop;

import com.shardshop.commands.ShopCommand;
import com.shardshop.config.ConfigManager;
import com.shardshop.economy.EconomyManager;
import com.shardshop.inventory.gui.GUIListener;
import com.shardshop.inventory.gui.GUIManager;
import com.shardshop.shop.ShopItem;
import com.shardshop.shop.ShopManager;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.plugin.java.JavaPlugin;

@Getter
public final class ShardShop extends JavaPlugin {
    private ConfigManager configManager;
    private EconomyManager economyManager;
    private GUIManager guiManager;
    private ShopManager shopManager;
    
    @Override
    public void onEnable() {
        ConfigurationSerialization.registerClass(ShopItem.class);
        
        this.configManager = new ConfigManager(this);
        this.configManager.loadConfigs();
        
        this.economyManager = new EconomyManager(this);
        this.guiManager = new GUIManager();
        this.shopManager = new ShopManager(this);
        this.shopManager.loadShopItems();
        
        getServer().getPluginManager().registerEvents(new GUIListener(guiManager), this);
        
        ShopCommand shopCommand = new ShopCommand(this);
        getCommand("shop").setExecutor(shopCommand);
        getCommand("shop").setTabCompleter(shopCommand);
        getCommand("shopadmin").setExecutor(shopCommand);
        getCommand("shopadmin").setTabCompleter(shopCommand);
        
        if (configManager.getConfig().getBoolean("shop.async_save")) {
            startAutoSave();
        }
        
        getLogger().info("ShardShop enabled successfully!");
    }
    
    @Override
    public void onDisable() {
        if (shopManager != null) {
            shopManager.saveShopItems();
        }
        if (economyManager != null) {
            economyManager.saveBalances();
        }
        getLogger().info("ShardShop disabled!");
    }
    
    private void startAutoSave() {
        long interval = configManager.getConfig().getLong("shop.auto_save_interval") * 20L;
        Bukkit.getScheduler().runTaskTimerAsynchronously(this, () -> {
            shopManager.saveShopItems();
            economyManager.saveBalances();
        }, interval, interval);
    }
}