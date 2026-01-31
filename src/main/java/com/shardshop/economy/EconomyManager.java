package com.shardshop.economy;

import com.shardshop.ShardShop;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Getter
public class EconomyManager {
    private final ShardShop plugin;
    private final File balancesFile;
    private FileConfiguration balances;
    private final Map<UUID, Double> balanceCache;
    private double startingBalance;
    
    public EconomyManager(ShardShop plugin) {
        this.plugin = plugin;
        this.balancesFile = new File(plugin.getDataFolder(), "balances.yml");
        this.balanceCache = new HashMap<>();
        this.startingBalance = plugin.getConfigManager().getConfig().getDouble("economy.starting_balance", 1000.0);
        loadBalances();
    }
    
    public void loadBalances() {
        if (!balancesFile.exists()) {
            try {
                balancesFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Failed to create balances file: " + e.getMessage());
            }
        }
        
        balances = YamlConfiguration.loadConfiguration(balancesFile);
        balanceCache.clear();
        
        for (String key : balances.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(key);
                double balance = balances.getDouble(key);
                balanceCache.put(uuid, balance);
            } catch (IllegalArgumentException ignored) {
            }
        }
        
        plugin.getLogger().info("Loaded " + balanceCache.size() + " player balances.");
    }
    
    public void saveBalances() {
        balanceCache.forEach((uuid, balance) -> balances.set(uuid.toString(), balance));
        
        try {
            balances.save(balancesFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save balances: " + e.getMessage());
        }
    }
    
    public void saveBalancesAsync() {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, this::saveBalances);
    }
    
    public double getBalance(UUID uuid) {
        return balanceCache.getOrDefault(uuid, startingBalance);
    }
    
    public double getBalance(OfflinePlayer player) {
        return getBalance(player.getUniqueId());
    }
    
    public void setBalance(UUID uuid, double amount) {
        if (amount < 0) amount = 0;
        balanceCache.put(uuid, amount);
        
        if (plugin.getConfigManager().getConfig().getBoolean("economy.async_save", true)) {
            saveBalancesAsync();
        } else {
            saveBalances();
        }
    }
    
    public void setBalance(OfflinePlayer player, double amount) {
        setBalance(player.getUniqueId(), amount);
    }
    
    public boolean has(UUID uuid, double amount) {
        return getBalance(uuid) >= amount;
    }
    
    public boolean has(OfflinePlayer player, double amount) {
        return has(player.getUniqueId(), amount);
    }
    
    public boolean withdraw(UUID uuid, double amount) {
        if (!has(uuid, amount)) return false;
        setBalance(uuid, getBalance(uuid) - amount);
        return true;
    }
    
    public boolean withdraw(OfflinePlayer player, double amount) {
        return withdraw(player.getUniqueId(), amount);
    }
    
    public void deposit(UUID uuid, double amount) {
        setBalance(uuid, getBalance(uuid) + amount);
    }
    
    public void deposit(OfflinePlayer player, double amount) {
        deposit(player.getUniqueId(), amount);
    }
    
    public boolean transfer(UUID from, UUID to, double amount) {
        if (!has(from, amount)) return false;
        withdraw(from, amount);
        deposit(to, amount);
        return true;
    }
    
    public boolean transfer(OfflinePlayer from, OfflinePlayer to, double amount) {
        return transfer(from.getUniqueId(), to.getUniqueId(), amount);
    }
    
    public String formatBalance(double balance) {
        return plugin.getConfigManager().formatPrice(balance);
    }
}